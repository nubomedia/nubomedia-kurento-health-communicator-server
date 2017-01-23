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
import java.io.InputStream;
import java.util.Iterator;
import java.util.Random;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.datamodel.pojo.Content;
import com.kurento.agenda.services.pojo.MessageReadResponse;
import com.kurento.agenda.services.pojo.MessageSend;
import com.kurento.agenda.services.pojo.TimelineReadResponse;
import com.kurento.khc.datamodel.AccountEntity;
import com.kurento.khc.datamodel.ChannelEntity;
import com.kurento.khc.datamodel.GroupEntity;
import com.kurento.khc.datamodel.UserEntity;
import com.kurento.khc.jackson.KhcObjectMapper;
import com.kurento.khc.push.DevNullManager;
import com.kurento.khc.services.v2.CommandService;
import com.kurento.khc.services.v2.ContentService;
import com.kurento.khc.spring.KhcDatamodelConfig;
import com.kurento.khc.test.utils.KhcTestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = KhcDatamodelConfig.class)
@ActiveProfiles({ "embed_db", "embed_security" })
public class SendMessageToGroupCommandTest {

	private Random rnd = new Random();

	private AccountEntity acc;
	// private OrganizationEntity org;
	private GroupEntity group;
	private UserEntity sender;
	private ChannelEntity senderChannel;
	private UserEntity receiver;
	private ChannelEntity receiverChannel;
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
	public void testExecCommandSendMessageToManagedGroup()
			throws JsonParseException, JsonMappingException, IOException {
		testSetupManagedAccount();
		sendMessageToGroup(1);
	}

	@Test
	public void testExecCommandSendMessageToUnmanagedGroup()
			throws JsonParseException, JsonMappingException, IOException {
		testSetupUnmanagedAccount();
		sendMessageToGroup(1);
	}

	@Test
	public void testExecCommandSendMassiveMessageToUnmanagedGroup()
			throws JsonParseException, JsonMappingException, IOException {
		testSetupUnmanagedAccount();
		sendMessageToGroup(10);
	}

	private void sendMessageToGroup(int n) {
		Long localId = rnd.nextLong();
		String body = "test message: ";

		utils.login(sender);
		for (int i = 0; i < n; i++) {
			MessageSend params = new MessageSend();
			params.setLocalId(localId + i);
			params.setFrom(sender.getUUID());
			params.setTo(group.getUUID());
			params.setBody(body + i);

			Command sendMessage = new Command();
			sendMessage.setChannelId(senderChannel.getUUID());
			sendMessage.setMethod(Command.METHOD_SEND_MESSAGE_TO_GROUP);
			sendMessage.setParams(jsonMapper.convertValue(params,
					ObjectNode.class));

			commandService.executeCommand(sendMessage, null, false);
		}

		// Verify update message command is received by sender
		Iterator<Command> pendingCommands = commandService.getPendingCommands(
				senderChannel.getUUID(), 0L).iterator();
		assertTimelineUpdate(pendingCommands.next(), sender);
		for (int i = 0; i < n; i++) {
			MessageSend message = new MessageSend();
			message.setLocalId(localId + i);
			message.setFrom(sender.getUUID());
			message.setTo(group.getUUID());
			message.setBody(body + i);
			assertMessageUpdate(pendingCommands.next(), message, false);
		}
		Assert.assertFalse(pendingCommands.hasNext());

		// Verify update message command is received by receiver
		utils.login(receiver);
		pendingCommands = commandService.getPendingCommands(
				receiverChannel.getUUID(), 0L).iterator();
		assertTimelineUpdate(pendingCommands.next(), receiver);
		for (int i = 0; i < n; i++) {
			MessageSend message = new MessageSend();
			message.setLocalId(localId + i);
			message.setFrom(sender.getUUID());
			message.setTo(group.getUUID());
			message.setBody(body + i);
			assertMessageUpdate(pendingCommands.next(), message, false);
		}
		Assert.assertFalse(pendingCommands.hasNext());

	}

	@Test
	public void testExecCommandSendMessageToGroupWithContent()
			throws IOException {
		testSetupManagedAccount();

		Long localId = rnd.nextLong();

		MessageSend params = new MessageSend();
		params.setLocalId(localId);
		params.setFrom(sender.getUUID());
		params.setTo(group.getUUID());
		params.setBody("test message");

		Command sendMessage = new Command();
		sendMessage.setChannelId(senderChannel.getUUID());
		sendMessage.setMethod(Command.METHOD_SEND_MESSAGE_TO_GROUP);
		sendMessage
				.setParams(jsonMapper.convertValue(params, ObjectNode.class));

		InputStream picture = utils.getImage();
		Content content = contentService.saveContent(picture, "image/jpeg");

		utils.login(sender);
		commandService.executeCommand(sendMessage, content, false);

		// Verify update message command is received by sender
		Iterator<Command> pendingCommands = commandService.getPendingCommands(
				senderChannel.getUUID(), 0L).iterator();
		assertTimelineUpdate(pendingCommands.next(), sender);
		assertMessageUpdate(pendingCommands.next(), params, true);
		Assert.assertFalse(pendingCommands.hasNext());

		// Verify update message command is received by receiver
		utils.login(receiver);
		pendingCommands = commandService.getPendingCommands(
				receiverChannel.getUUID(), 0L).iterator();
		assertTimelineUpdate(pendingCommands.next(), receiver);
		assertMessageUpdate(pendingCommands.next(), params, true);
		Assert.assertFalse(pendingCommands.hasNext());

	}

	@Test
	public void testSendMessageToGroupWithTimelineDeactivated()
			throws IOException {
		testSetupManagedAccount();
		// Disable receiver timeline
		utils.disableUserTimeline(receiver, group);

		// Send message
		Long localId = rnd.nextLong();

		MessageSend params = new MessageSend();
		params.setLocalId(localId);
		params.setFrom(sender.getUUID());
		params.setTo(group.getUUID());
		params.setBody("test message");

		Command sendMessage = new Command();
		sendMessage.setChannelId(senderChannel.getUUID());
		sendMessage.setMethod(Command.METHOD_SEND_MESSAGE_TO_GROUP);
		sendMessage
				.setParams(jsonMapper.convertValue(params, ObjectNode.class));

		utils.login(sender);
		commandService.executeCommand(sendMessage, null, false);

	}

	@Test
	public void testSendHugeMessageToGroup() throws IOException {
		testSetupManagedAccount();
		Long localId = rnd.nextLong();

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 100; i++) {
			sb.append("El gato espero un rato");
		}

		MessageSend params = new MessageSend();
		params.setLocalId(localId);
		params.setFrom(sender.getUUID());
		params.setTo(group.getUUID());
		params.setBody(sb.toString());

		Command sendMessage = new Command();
		sendMessage.setChannelId(senderChannel.getUUID());
		sendMessage.setMethod(Command.METHOD_SEND_MESSAGE_TO_GROUP);
		sendMessage
				.setParams(jsonMapper.convertValue(params, ObjectNode.class));

		utils.login(sender);
		commandService.executeCommand(sendMessage, null, false);
		// Verify update message command is received by sender
		Iterator<Command> pendingCommands = commandService.getPendingCommands(
				senderChannel.getUUID(), 0L).iterator();
		assertTimelineUpdate(pendingCommands.next(), sender);
		assertMessageUpdate(pendingCommands.next(), params, false);

	}

	private void testSetupManagedAccount() throws IOException {
		// Test setup
		String name = String.valueOf(rnd.nextInt());
		acc = utils.createAccount();
		// org = utils.createRootOrganization(name, acc);
		group = utils.createGroup(name, acc);
		sender = utils.createUser(name + "_sender", acc, false);
		senderChannel = utils.createChannel(sender);
		receiver = utils.createUser(name + "_receiver", acc, false);
		receiverChannel = utils.createChannel(receiver);
		admin = utils.createUser(name + "_admin", acc, false);
		utils.addAccountAdmin(acc, admin);
		utils.addGroupMember(group, sender);
		utils.addGroupMember(group, receiver);
	}

	private void testSetupUnmanagedAccount() throws IOException {
		String name = String.valueOf(rnd.nextInt());
		acc = utils.createAutomanagedAccount();
		group = utils.createGroup(name + "_group", acc);
		sender = utils.createUser(name + "_sender", acc, false);
		senderChannel = utils.createChannel(sender);
		receiver = utils.createUser(name + "_receiver", acc, false);
		receiverChannel = utils.createChannel(receiver);
		admin = utils.createUser(name + "_admin", acc, false);
		utils.addAccountAdmin(acc, admin);
		utils.addGroupMember(group, sender);
		utils.addGroupMember(group, receiver);
		for (int i = 0; i < 50; i++) {
			UserEntity member = utils.createUser(name + i, acc, false);
			utils.addGroupMember(group, member);
		}
	}

	private void assertTimelineUpdate(Command command, UserEntity owner) {
		Assert.assertTrue(Command.METHOD_UPDATE_TIMELINE.equals(command
				.getMethod()));
		TimelineReadResponse updateTimelineParams = jsonMapper.convertValue(
				command.getParams(), TimelineReadResponse.class);
		Assert.assertTrue(updateTimelineParams.getOwnerId().equals(
				owner.getUUID()));
	}

	private void assertMessageUpdate(Command command, MessageSend message,
			Boolean content) {
		Assert.assertTrue(Command.METHOD_UPDATE_MESSAGE.equals(command
				.getMethod()));
		MessageReadResponse updateMessageParams = jsonMapper.convertValue(
				command.getParams(), MessageReadResponse.class);
		Assert.assertTrue(updateMessageParams.getBody().equals(
				message.getBody()));
		Assert.assertTrue(updateMessageParams.getLocalId().equals(
				message.getLocalId()));
		Assert.assertTrue(updateMessageParams.getFrom().getId()
				.equals(message.getFrom()));
		Assert.assertTrue(updateMessageParams.getBody().equals(
				message.getBody()));
		if (content) {
			Assert.assertTrue(updateMessageParams.getContent().getId() != null);
		}
	}

}
