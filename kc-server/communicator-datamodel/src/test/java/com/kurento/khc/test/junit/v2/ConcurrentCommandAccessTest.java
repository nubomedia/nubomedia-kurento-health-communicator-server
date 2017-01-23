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

package com.kurento.khc.test.junit.v2;

import java.io.IOException;
import java.util.Random;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.services.pojo.MessageSend;
import com.kurento.khc.datamodel.AccountEntity;
import com.kurento.khc.datamodel.ChannelEntity;
import com.kurento.khc.datamodel.GroupEntity;
import com.kurento.khc.datamodel.UserEntity;
import com.kurento.khc.jackson.KhcObjectMapper;
import com.kurento.khc.push.DevNullManager;
import com.kurento.khc.push.DevNullManager.DevNullListener;
import com.kurento.khc.push.Notification;
import com.kurento.khc.services.v2.CommandService;
import com.kurento.khc.services.v2.ContentService;
import com.kurento.khc.spring.KhcDatamodelConfig;
import com.kurento.khc.test.utils.KhcTestUtils;

@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = KhcDatamodelConfig.class)
@ActiveProfiles({ "embed_db", "embed_security" })
public class ConcurrentCommandAccessTest {

	private Random rnd = new Random();

	private AccountEntity acc;
	private GroupEntity group;
	private UserEntity sender;
	private ChannelEntity senderChannel;
	private UserEntity receiver;
	private UserEntity admin;

	private ObjectMapper jsonMapper = new KhcObjectMapper();

	@Autowired
	private KhcTestUtils utils;

	@Autowired
	private CommandService commandService;

	@Autowired
	private ContentService contentService;

	@Autowired
	private DevNullManager pushManager;

	@Test
	public void testReadCommandConcurrently() throws IOException,
			InterruptedException {
		testSetupUnmanagedAccount();

		// Send message
		Long localId = rnd.nextLong();
		Long lastSequence = 0L;

		MessageSend params = new MessageSend();
		params.setLocalId(localId);
		params.setFrom(sender.getUUID());
		params.setTo(group.getUUID());
		params.setBody("test message");

		final Command sendMessage = new Command();
		sendMessage.setChannelId(senderChannel.getUUID());
		sendMessage.setMethod(Command.METHOD_SEND_MESSAGE_TO_GROUP);
		sendMessage
				.setParams(jsonMapper.convertValue(params, ObjectNode.class));

		// Add listener to DevNull
		final Long sequence = lastSequence;
		pushManager.addListener(senderChannel.getUUID(), new DevNullListener() {

			@Override
			public void push(Notification notification) {
				new Thread("reader") {
					@Override
					public void run() {
						try {
							Thread.sleep(50);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						utils.login(sender);
						Assert.assertTrue(commandService.getPendingCommands(
								senderChannel.getUUID(), sequence).size() > 0);
					}
				}.start();
			}

		});

		utils.login(sender);
		for (Command command : commandService.getPendingCommands(
				senderChannel.getUUID(), 0L)) {
			lastSequence = command.getSequenceNumber();
		}
		new Thread("sender") {
			@Override
			public void run() {
				utils.login(sender);
				commandService.executeCommand(sendMessage, null, false);
			}
		}.start();

		Thread.sleep(400000);
	}

	private void testSetupUnmanagedAccount() throws IOException {
		String name = String.valueOf(rnd.nextInt());
		acc = utils.createAutomanagedAccount();
		group = utils.createGroup(name + "_group", acc);
		sender = utils.createUser(name + "_sender", acc, false);
		senderChannel = utils.createChannel(sender);
		receiver = utils.createUser(name + "_receiver", acc, false);
		admin = utils.createUser(name + "_admin", acc, false);
		utils.addAccountAdmin(acc, admin);
		utils.addGroupMember(group, sender);
		utils.addGroupMember(group, receiver);
		for (int i = 0; i < 50; i++) {
			UserEntity member = utils.createUser(name + i, acc, false);
			utils.addGroupMember(group, member);
		}
	}

}
