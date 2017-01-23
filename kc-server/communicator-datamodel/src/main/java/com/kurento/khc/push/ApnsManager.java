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

package com.kurento.khc.push;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javapns.communication.exceptions.CommunicationException;
import javapns.communication.exceptions.KeystoreException;
import javapns.devices.Device;
import javapns.devices.implementations.basic.BasicDevice;
import javapns.notification.AppleNotificationServer;
import javapns.notification.AppleNotificationServerBasicImpl;
import javapns.notification.PushNotificationManager;
import javapns.notification.PushNotificationPayload;
import javapns.notification.PushedNotification;
import javapns.notification.ResponsePacket;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.kurento.agenda.datamodel.pojo.Channel;
import com.kurento.khc.datamodel.ChannelEntity;

@Component("khcApnsPush")
public class ApnsManager implements NotificationManager {

	private static Logger log = LoggerFactory.getLogger(ApnsManager.class);

	@Value(value = "${kurento.apns.production:#{null}}")
	private String APNS_PRODUCTION_CFG;
	private Boolean APNS_PRODUCTION = false;
	@Value(value = "${kurento.apns.keystore.password:#{null}}")
	private String APNS_KEYSTORE_PWD = "secreta";
	@Value("${kurento.apns.keystore:#{null}}")
	private String APNS_KEYSTORE_FILENAME = "khc_apns_dev.p12";

	@Autowired
	private NotificationServer notificationServer;

	private Boolean isApnsEnabled = false;
	private AppleNotificationServer apnsServer;
	private PushNotificationManager apnsManager;
	private byte[] apnsKeystore;

	@PostConstruct
	public void init() {
		isApnsEnabled = false;
		if (APNS_PRODUCTION_CFG != null) {
			APNS_PRODUCTION = Boolean.valueOf(APNS_PRODUCTION_CFG);
		}
		try {
			File apnsKeystoreFile = new File(APNS_KEYSTORE_FILENAME);
			if (apnsKeystoreFile.exists()) {
				apnsKeystore = IOUtils.toByteArray(new FileInputStream(
						apnsKeystoreFile));
			} else {
				// Search keystore in classpath
				apnsKeystore = IOUtils.toByteArray(this.getClass()
						.getClassLoader()
						.getResourceAsStream(APNS_KEYSTORE_FILENAME));
			}
		} catch (IOException e) {
			log.error("Unable to read APNS keystore. APNS disabled", e);
			return;
		}

		try {
			apnsServer = new AppleNotificationServerBasicImpl(apnsKeystore,
					APNS_KEYSTORE_PWD, APNS_PRODUCTION);
			apnsManager = new PushNotificationManager();
			apnsManager.initializeConnection(apnsServer);
		} catch (KeystoreException e) {
			log.warn("Unable to read APNS keystore");
			isApnsEnabled = false;
		} catch (CommunicationException e) {
			log.warn("Unable to communicate with APNS service");
			isApnsEnabled = false;
		}
		isApnsEnabled = true;
	}

	@Override
	public Boolean isEnabled(ChannelEntity channel) {
		String channelType = channel.getRegisterType();
		return Channel.APNS.equals(channelType)
				&& channel.isEnabled();
	}

	@Override
	public void sendNotification(Notification notification) {
		Assert.notNull(notification);
		ChannelEntity channel = notification.getChannel();
		if (notification.isQos()) {
			//TODO not implemented...
			log.warn("Not sending APNS notification for QoS to channel {}, not implemented yet",
					channel.getUUID());
			return;
		}
		if (channel.getRegisterId() != null
				&& !channel.getRegisterId().isEmpty()) {
			// Send alert
			try {
				log.trace("Sending APNS notification to channel {}",
						channel.getUUID());

				PushNotificationPayload payload = PushNotificationPayload
						.complex();
				payload.addSound("new-mail.caf");
				payload.addAlert(notification.getMsg());
				payload.addBadge(notification.getBadge());
				Device device = new BasicDevice();
				device.setToken(channel.getRegisterId());
				PushedNotification apnsResult = apnsManager.sendNotification(
						device, payload);
				if (!apnsResult.isSuccessful()) {
					ResponsePacket response = apnsResult.getResponse();
					if (response != null) {
						log.warn(
								"APNS REMOTE ERROR on channel: {} .Message {}",
								channel.getUUID(), response.getMessage());
					}
					Exception err = apnsResult.getException();
					if (err != null) {
						log.warn(
								"APNS LOCAL ERROR on channel: {}. Reconnecting ...",
								channel.getUUID(), err);
						// Try to reconnect
						init();
					}
					notificationServer.processResult(new PushResult(
							notification, PushResult.Cause.INVALID_CHANNEL));
				} else {
					notificationServer.processResult(new PushResult(
							notification, PushResult.Cause.OK));
				}
			} catch (JSONException e) {
				log.warn(
						"Unable to build custom APNS payload due to JSON error for channel {}",
						channel.getId());
				notificationServer.processResult(new PushResult(notification,
						PushResult.Cause.FAILED));
			} catch (CommunicationException e) {
				log.warn(
						"Unable to send APNS notification due to communication error for channel {}",
						notification.getChannel().getId());
				isApnsEnabled = false;
				notificationServer.processResult(new PushResult(notification,
						PushResult.Cause.SERVICE_UNAVAILABLE));
			}
		}

	}

	@Scheduled(fixedDelay = 60000)
	public void connectionWatchdog() {
		if (!isApnsEnabled) {
			init();
		}
	}
}
