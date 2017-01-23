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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.agenda.datamodel.pojo.Channel;
import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.services.pojo.MessageSend;
import com.kurento.khc.KhcNotFoundException;
import com.kurento.khc.datamodel.AccountEntity;
import com.kurento.khc.datamodel.ChannelDao;
import com.kurento.khc.datamodel.ChannelEntity;
import com.kurento.khc.datamodel.GroupEntity;
import com.kurento.khc.datamodel.UserEntity;
import com.kurento.khc.jackson.KhcObjectMapper;
import com.kurento.khc.push.DevNullManager;
import com.kurento.khc.push.DevNullManager.DevNullListener;
import com.kurento.khc.push.Notification;
import com.kurento.khc.services.v2.CommandService;
import com.kurento.khc.spring.KhcDatamodelConfig;
import com.kurento.khc.test.utils.KhcTestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = KhcDatamodelConfig.class)
@ActiveProfiles({ "embed_db", "embed_security" })
public class ChannelTest {

	private Random rnd = new Random();

	private AccountEntity acc;
	private UserEntity sender, receiver;
	private ChannelEntity senderChannel;
	private ChannelEntity receiverApnsChannel;
	private ChannelEntity receiverDevnullChannel;
	private GroupEntity group;

	@Autowired
	private KhcTestUtils utils;

	@Autowired
	private CommandService commandService;

	@Autowired
	private ChannelDao channelDao;

	@Autowired
	private DevNullManager pushManager;

	private ObjectMapper jsonMapper = new KhcObjectMapper();

	@Test
	public void testCreateChannel() throws IOException {
		testSetup();

		// Test perform
		Channel notch = new Channel();
		notch.setInstanceId("myInstance");
		notch.setRegisterId("abc123");
		notch.setRegisterType(Channel.WEB_POLL);
		notch.setUserId(sender.getUUID());
		notch.setLocale("es");

		utils.login(sender);
		commandService.createNotificationChannel(notch);

	}

	@Test(expected = AccessDeniedException.class)
	public void testCreateAnonymousChannel() throws IOException {

		testSetup();

		// Test perform
		Channel notch = new Channel();
		notch.setRegisterId("abc123");
		notch.setRegisterType(Channel.WEB_POLL);
		notch.setUserId(sender.getUUID());

		utils.anonymousLogin();
		commandService.createNotificationChannel(notch);
	}

	@Test
	public void testChannelDisableDueToBadAPNSResponse() throws IOException,
			InterruptedException {
		testSetup();
		// Create APNS channel
		receiverApnsChannel = utils.createChannel(receiver, Channel.APNS);

		// Send message to force PUSH notification
		sendMessage(senderChannel.getUUID());

		// Wait until APNS declares failure
		int i = 0;
		do {
			receiverApnsChannel = channelDao
					.findNotificationChannelByUUID(receiverApnsChannel
							.getUUID());
			Thread.sleep(1000);
		} while (receiverApnsChannel.isEnabled() && i++ < 20);
		Assert.assertFalse(receiverApnsChannel.isEnabled());
	}

	@Test(expected = KhcNotFoundException.class)
	public void testChannelCleanUpDueToInstance() throws IOException {
		testSetup();
		String instanceId = "" + rnd.nextInt();
		String regId = "" + rnd.nextInt();

		// Create channel
		Channel notch = new Channel();
		notch.setInstanceId(instanceId);
		notch.setRegisterId(regId);
		notch.setRegisterType(Channel.DEV_NULL);
		notch.setUserId(sender.getUUID());
		notch.setLocale("es");

		utils.login(sender);
		Channel origNotch = commandService.createNotificationChannel(notch);

		// Same client creates other channel
		notch = new Channel();
		notch.setInstanceId(instanceId);
		notch.setRegisterId(regId + "new");
		notch.setRegisterType(Channel.DEV_NULL);
		notch.setUserId(sender.getUUID());
		notch.setLocale("es");

		utils.login(sender);
		commandService.createNotificationChannel(notch);

		// origchannel must have been deleted
		channelDao.findNotificationChannelByUUID(origNotch.getId());

	}

	@Test(expected = KhcNotFoundException.class)
	public void testChannelCleanUpDueToRegisterId() throws IOException {
		testSetup();
		String instanceId = "" + rnd.nextInt();
		String regId = "" + rnd.nextInt();

		// Create channel
		Channel notch = new Channel();
		notch.setInstanceId(instanceId);
		notch.setRegisterId(regId);
		notch.setRegisterType(Channel.DEV_NULL);
		notch.setUserId(sender.getUUID());
		notch.setLocale("es");

		utils.login(sender);
		Channel origNotch = commandService.createNotificationChannel(notch);

		// Same client creates other channel
		notch = new Channel();
		notch.setInstanceId(instanceId + "new");
		notch.setRegisterId(regId);
		notch.setRegisterType(Channel.DEV_NULL);
		notch.setUserId(sender.getUUID());
		notch.setLocale("es");

		utils.login(sender);
		commandService.createNotificationChannel(notch);

		// origchannel must have been deleted
		channelDao.findNotificationChannelByUUID(origNotch.getId());

	}

	@Test
	public void testMultilanguageApnsNotification() throws IOException,
			InterruptedException {
		testSetup();

		// Create ES channel
		Channel notchParam = new Channel();
		notchParam.setInstanceId(receiverDevnullChannel.getInstanceId());
		notchParam.setRegisterId(receiverDevnullChannel.getRegisterId());
		notchParam.setRegisterType(Channel.DEV_NULL);
		notchParam.setUserId(sender.getUUID());
		notchParam.setLocale("es");

		utils.login(sender);
		Channel notch = commandService.createNotificationChannel(notchParam);

		// Add listener for channel
		pushManager.addListener(notch.getId(), new DevNullListener() {

			@Override
			public void push(Notification notification) {
				String msg = notification.getMsg();
				if (msg != null) {
					Assert.assertTrue(notification.getMsg().endsWith(
							"mensajes sin leer"));
				}
			}
		});

		// Send command on this channel and verify message language is spanish
		sendMessage(notch.getId());

		// Create CH channel (use default lang)
		notchParam = new Channel();
		notchParam.setInstanceId(receiverDevnullChannel.getInstanceId());
		notchParam.setRegisterId(receiverDevnullChannel.getRegisterId());
		notchParam.setRegisterType(Channel.DEV_NULL);
		notchParam.setUserId(sender.getUUID());
		notchParam.setLocale("ch");

		notch = commandService.createNotificationChannel(notchParam);

		// Add listener for channel
		pushManager.addListener(notch.getId(), new DevNullListener() {

			@Override
			public void push(Notification notification) {
				String msg = notification.getMsg();
				if (msg != null) {
					Assert.assertTrue(notification.getMsg().endsWith(
							"unread messages"));
				}
			}
		});

		// Send command on this channel and verify message language is spanish
		sendMessage(notch.getId());
		Thread.sleep(2000);

	}

	@Test
	public void testNotificationMsg() throws IOException, InterruptedException {
		testSetup();

		utils.login(sender);
		pushManager.addListener(receiverDevnullChannel.getUUID(),
				new DevNullListener() {

					Integer pendingMsg = 1;

					@Override
					public void push(Notification notification) {
						String msg = notification.getMsg();
						if (msg != null) {
							Assert.assertTrue(notification.getMsg().endsWith(
									"KHC: " + pendingMsg++
											+ " mensajes sin leer"));
						}
					}
				});

		// Send command on this channel and verify message language is spanish
		for (int i = 0; i < 5; i++) {
			sendMessage(senderChannel.getUUID());
		}

		Thread.sleep(2000);

	}

	private void testSetup() throws IOException {
		acc = utils.createAccount();
		String name = String.valueOf(rnd.nextInt());
		sender = utils.createUser(name, acc, false);
		senderChannel = utils.createChannel(sender, Channel.DEV_NULL);
		receiver = utils.createUser("receiver-" + name, acc, false);
		receiverDevnullChannel = utils.createChannel(receiver, Channel.DEV_NULL);
		group = utils.createGroup(name, acc);
		utils.addGroupMember(group, sender);
		utils.addGroupMember(group, receiver);
	}

	private void sendMessage(Long channelId) {
		// Send message to force PUSH notification
		MessageSend params = new MessageSend();
		params.setLocalId(rnd.nextLong());
		params.setFrom(sender.getUUID());
		params.setTo(group.getUUID());
		params.setBody("body");

		Command sendMessage = new Command();
		sendMessage.setChannelId(channelId);
		sendMessage.setSequenceNumber(rnd.nextLong());
		sendMessage.setMethod(Command.METHOD_SEND_MESSAGE_TO_GROUP);
		sendMessage
				.setParams(jsonMapper.convertValue(params, ObjectNode.class));
		utils.login(sender);
		commandService.executeCommand(sendMessage, null, false);

	}
}
