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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.datamodel.pojo.TimelineParty.PartyType;
import com.kurento.agenda.services.pojo.MessageReadResponse;
import com.kurento.agenda.services.pojo.MessageSend;
import com.kurento.agenda.services.pojo.TimelineReadResponse;
import com.kurento.agenda.services.pojo.UserReadContactResponse;
import com.kurento.agenda.services.pojo.UserSearchLocalContact;
import com.kurento.khc.datamodel.AccountEntity;
import com.kurento.khc.datamodel.ChannelEntity;
import com.kurento.khc.datamodel.GroupEntity;
import com.kurento.khc.datamodel.UserEntity;
import com.kurento.khc.jackson.KhcObjectMapper;
import com.kurento.khc.services.v2.CommandService;
import com.kurento.khc.spring.KhcDatamodelConfig;
import com.kurento.khc.test.utils.KhcTestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = KhcDatamodelConfig.class)
@ActiveProfiles({ "embed_db", "embed_security" })
public class SendMessageToUserCommandTest {

	private Random rnd = new Random();

	private AccountEntity acc;
	private UserEntity sender;
	private ChannelEntity senderChannel;
	private UserEntity receiver;
	private ChannelEntity receiverChannel;
	private GroupEntity group;
	private Map<Long, Long> lastSequence;

	private ObjectMapper jsonMapper = new KhcObjectMapper();

	@Autowired
	private KhcTestUtils utils;

	@Autowired
	private CommandService commandService;

	@Test(expected = AccessDeniedException.class)
	public void testSendMessageToUnknownUser() throws IOException {
		testSetup();
		sendMessageToUser();
	}

	@Test
	public void testSendMessageToUserInSameGroup() throws IOException {
		testSetup();
		utils.addGroupMember(group, receiver);
		utils.addGroupMember(group, sender);
		sendMessageToUser();
	}

	@Test
	public void testSendMessageToLocalContact() throws IOException {
		testSetup();

		UserSearchLocalContact contacts = new UserSearchLocalContact();
		contacts.getLocalPhones().add(receiver.getPhone());

		Long channelId = senderChannel.getUUID();
		Command searchLocal = new Command();
		searchLocal.setChannelId(channelId);
		searchLocal.setMethod(Command.METHOD_SEARCH_LOCAL_CONTACT);
		searchLocal.setParams(jsonMapper.convertValue(contacts,
				ObjectNode.class));
		utils.login(sender);
		commandService.executeCommand(searchLocal, null, false);
		List<Command> pd = commandService.getPendingCommands(channelId,
				lastSequence.get(channelId));
		lastSequence.put(channelId, pd.get(pd.size() - 1).getSequenceNumber());

		sendMessageToUser();

	}

	private void sendMessageToUser() {
		Long localId = rnd.nextLong();
		MessageSend params = new MessageSend();
		params.setLocalId(localId);
		params.setFrom(sender.getUUID());
		params.setTo(receiver.getUUID());
		params.setBody("test message");

		Command sendMessage = new Command();
		sendMessage.setChannelId(senderChannel.getUUID());
		sendMessage.setMethod(Command.METHOD_SEND_MESSAGE_TO_USER);
		sendMessage
				.setParams(jsonMapper.convertValue(params, ObjectNode.class));

		utils.login(sender);
		commandService.executeCommand(sendMessage, null, false);

		// Verify update message command is received by sender
		Iterator<Command> pendingCommands = commandService.getPendingCommands(
				senderChannel.getUUID(),
				lastSequence.get(senderChannel.getUUID())).iterator();
		assertTimelineUpdate(pendingCommands.next(), sender, receiver);
		assertMessageUpdate(pendingCommands.next(), params, false);
		Assert.assertFalse(pendingCommands.hasNext());

		// Verify update message command is received by receiver
		utils.login(receiver);
		pendingCommands = commandService.getPendingCommands(
				receiverChannel.getUUID(),
				lastSequence.get(receiverChannel.getUUID())).iterator();
		assertContactUpdate(pendingCommands.next(), sender);
		assertTimelineUpdate(pendingCommands.next(), receiver, sender);
		assertMessageUpdate(pendingCommands.next(), params, false);
		Assert.assertFalse(pendingCommands.hasNext());
	}

	private void testSetup() throws IOException {
		String name = String.valueOf(rnd.nextInt());

		acc = utils.createAutomanagedAccount();
		sender = utils.createUser(name + "_sender", acc, false);
		senderChannel = utils.createChannel(sender);
		receiver = utils.createUser(name + "_receiver", acc, false);
		receiverChannel = utils.createChannel(receiver);
		group = utils.createGroup(name + "_group", acc);
		lastSequence = new HashMap<Long, Long>();
		lastSequence.put(receiverChannel.getUUID(), 0L);
		lastSequence.put(senderChannel.getUUID(), 0L);
	}

	private void assertContactUpdate(Command command, UserEntity contact) {
		Assert.assertTrue(Command.METHOD_UPDATE_CONTACT.equals(command
				.getMethod()));
		UserReadContactResponse contactParam = jsonMapper.convertValue(
				command.getParams(), UserReadContactResponse.class);
		Assert.assertTrue(contactParam.getId().equals(contact.getUUID()));
	}

	private void assertTimelineUpdate(Command command, UserEntity owner,
			UserEntity party) {
		Assert.assertTrue(Command.METHOD_UPDATE_TIMELINE.equals(command
				.getMethod()));
		TimelineReadResponse updateTimelineParams = jsonMapper.convertValue(
				command.getParams(), TimelineReadResponse.class);
		Assert.assertTrue(updateTimelineParams.getOwnerId().equals(
				owner.getUUID()));
		Assert.assertTrue(updateTimelineParams.getParty().getId()
				.equals(party.getUUID()));
		updateTimelineParams.getParty().getType()
				.equals(PartyType.USER.getValue());
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
