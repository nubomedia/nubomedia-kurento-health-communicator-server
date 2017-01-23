// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.kurento.khc.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.net.pop3.POP3MessageInfo;
import org.apache.commons.net.pop3.POP3SClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;


@Component
public class MailUtils {

	private final static Logger log = LoggerFactory
			.getLogger(MailUtils.class);
	private ExecutorService executorService = Executors.newFixedThreadPool(10);

	@Autowired
	private Environment environment;

	@Value("${kurento.smtp.host:#{null}}")
	private String SMTP_HOST = "localhost";
	@Value("${kurento.smtp.port:#{null}}")
	private String SMTP_PORT = "25";
	@Value("${kurento.smtp.username:#{null}}")
	private String STMP_USERNAME = "user";
	@Value("${kurento.smtp.password:#{null}}")
	private String SMTP_PASSWORD = "password";
	@Value("${kurento.smtp.from:#{null}}")
	private String SMTP_FROM = "info@kurento.org";
	@Value("${kurento.smtp.reply:#{null}}")
	private String SMTP_REPLYTO = "info@kurento.org";


	public void sendMail(final String recipient, final String subject,
			final String body) {
		List<String> recipients = new ArrayList<String>();
		recipients.add(recipient);
		sendMail(recipients, subject, body);

	}

	public void sendMail(final List<String> recipients, final String subject,
			final String body) {
		Runnable sendMailAsync = new Runnable() {

			@Override
			public void run() {

				try {

					Properties props = new Properties();

					// Server data
					props.setProperty("mail.smtp.host", SMTP_HOST);
					props.setProperty("mail.smtp.port", SMTP_PORT); // 587
					props.setProperty("mail.smtp.starttls.enable", "true");
					props.setProperty("mail.smtp.auth", "true");

					// Credentials
					// props.setProperty("mail.smtp.user", username);

					final String authUser = STMP_USERNAME;
					final String authPwd = SMTP_PASSWORD;
					Session session = Session.getInstance(props,
							new javax.mail.Authenticator() {
								@Override
								protected PasswordAuthentication getPasswordAuthentication() {
									return new PasswordAuthentication(authUser,
											authPwd);
								}
							});
					MimeMessage message = new MimeMessage(session);
					message.setFrom(new InternetAddress(SMTP_FROM));

					Address[] to = createArrayAddress(recipients);

					message.addRecipients(Message.RecipientType.TO, to);

					ArrayList<String> replyToList = new ArrayList<String>();
					replyToList.add(SMTP_REPLYTO);
					Address[] replyToAddress = createArrayAddress(replyToList);

					message.setReplyTo(replyToAddress);

					message.setSubject(subject, "UTF-8");
					message.setText(body, "UTF-8", "html");

					// Send email

					Transport.send(message);

				} catch (Exception e) {
					log.warn("Unable to send email from: " + SMTP_FROM + " to: "
							+ recipients, e);
				}
			}

		};
		executorService.execute(sendMailAsync);
	}

	public Object[] receiveMail(String server, int port, String username,
			String password) {

		Object[] response = new Object[2];
		// Connect to server
		POP3SClient pop3 = new POP3SClient(true);
		pop3.setDefaultTimeout(5000);
		try {
			// Connect
			pop3.connect(server, port);
			log.debug("Connected to POP server: " + server + ":" + port);

			// Login
			if (!pop3.login(username, password)) {
				log.info("Could not login to server, Check username and password");
				response[0] = new Boolean(false);
				return response;
			}
			response[0] = new Boolean(true);
			log.debug("Logged to " + server + " as " + username);

			// List messages
			List<String[]> receivedMessages = new ArrayList<String[]>();
			response[1] = receivedMessages;
			POP3MessageInfo[] messages = pop3.listMessages();
			if (messages != null) {
				for (POP3MessageInfo msginfo : messages) {
					// Get header
					BufferedReader reader = (BufferedReader) pop3
							.retrieveMessage(msginfo.number);

					String line;
					String subject = "";
					String body = "";
					// Get headers
					while ((line = reader.readLine()) != null
							&& !line.isEmpty()) {
						String lower = line.toLowerCase(Locale.ENGLISH);
						if (lower.startsWith("subject: ")) {
							subject = line.substring(9).trim();
						}
					}
					// Get message
					while ((line = reader.readLine()) != null) {
						body += line;
					}
					reader.close();
					receivedMessages.add(new String[] { subject, body });
				}
			}

			return response;

		} catch (IOException e) {
			log.info(
					"Could not connect to server. Check host, port, protocol and connectivity",
					e);
			response[0] = false;
			return response;
		}

	}

	private static Address[] createArrayAddress(List<String> l)
			throws AddressException {

		if (l == null)
			return new Address[0];
		Address[] address = new Address[l.size()];

		for (int i = 0; i < l.size(); i++) {
			address[i] = new InternetAddress(l.get(i));

		}

		return address;
	}

}
