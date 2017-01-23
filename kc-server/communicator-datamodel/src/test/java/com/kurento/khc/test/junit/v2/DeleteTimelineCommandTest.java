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
import java.util.ArrayList;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.datamodel.pojo.TimelineParty.PartyType;
import com.kurento.agenda.services.pojo.MessageReadResponse;
import com.kurento.agenda.services.pojo.MessageSend;
import com.kurento.agenda.services.pojo.TimelineCreate;
import com.kurento.agenda.services.pojo.TimelinePartyUpdate;
import com.kurento.agenda.services.pojo.TimelineReadResponse;
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
public class DeleteTimelineCommandTest {

	private Random rnd = new Random();

	private AccountEntity acc;
	private GroupEntity group;
	private UserEntity owner;
	private List<ChannelEntity> channels;
	private Map<Long, Long> lastSequences;

	@Autowired
	KhcTestUtils utils;

	@Autowired
	CommandService commandService;

	private ObjectMapper jsonMapper = new KhcObjectMapper();

	@Test
	public void deleteTimelineTest() throws IOException {
		testSetup();

		TimelinePartyUpdate party = new TimelinePartyUpdate();
		party.setId(group.getUUID());
		party.setType(PartyType.GROUP.getValue());
		TimelineCreate timelineCreate = new TimelineCreate();
		timelineCreate.setOwnerId(owner.getUUID());
		timelineCreate.setParty(party);

		Command command = new Command();
		command.setChannelId(channels.get(0).getUUID());
		command.setMethod(Command.METHOD_DELETE_TIMELINE);
		command.setParams(timelineCreate);

		utils.login(owner);
		commandService.executeCommand(command, null, false);

		for (ChannelEntity channel : channels) {
			Iterator<Command> pendingCommands = commandService
					.getPendingCommands(channel.getUUID(), 0L).iterator();
			assertTimelineUpdate(pendingCommands.next());
			Assert.assertFalse(pendingCommands.hasNext());
		}
	}

	@Test
	public void reactivateTimelineWithMessageTest() throws IOException {

		testSetup();

		TimelinePartyUpdate party = new TimelinePartyUpdate();
		party.setId(group.getUUID());
		party.setType(PartyType.GROUP.getValue());
		TimelineCreate timelineCreate = new TimelineCreate();
		timelineCreate.setOwnerId(owner.getUUID());
		timelineCreate.setParty(party);

		Command command = new Command();
		command.setChannelId(channels.get(0).getUUID());
		command.setMethod(Command.METHOD_DELETE_TIMELINE);
		command.setParams(timelineCreate);

		utils.login(owner);
		commandService.executeCommand(command, null, false);
		for (ChannelEntity channel : channels) {
			Long channelId = channel.getUUID();
			Iterator<Command> pendingCommands = commandService
					.getPendingCommands(channelId, lastSequences.get(channelId))
					.iterator();
			Command cmd = pendingCommands.next();
			assertTimelineUpdate(cmd);

			lastSequences.put(channelId, cmd.getSequenceNumber());
		}

		// Send message
		MessageSend params = new MessageSend();
		params.setLocalId(rnd.nextLong());
		params.setFrom(owner.getUUID());
		params.setTo(group.getUUID());
		params.setBody("activation message");

		Command sendMessage = new Command();
		sendMessage.setChannelId(channels.get(0).getUUID());
		sendMessage.setMethod(Command.METHOD_SEND_MESSAGE_TO_GROUP);
		sendMessage
				.setParams(jsonMapper.convertValue(params, ObjectNode.class));
		commandService.executeCommand(sendMessage, null, false);

		for (ChannelEntity channel : channels) {
			Long channelId = channel.getUUID();
			Iterator<Command> pendingCommands = commandService
					.getPendingCommands(channelId, lastSequences.get(channelId))
					.iterator();
			assertTimelineUpdate(pendingCommands.next());
			int i = 0;
			while (pendingCommands.hasNext()) {
				command = pendingCommands.next();
				assertIsMessage(command);
				i++;
			}
			lastSequences.put(channelId, command.getSequenceNumber());
			Assert.assertTrue(i == 40);
			Assert.assertFalse(pendingCommands.hasNext());
		}

		// Verify a second message does not triggers all activation procedure
		sendMessage = new Command();
		sendMessage.setChannelId(channels.get(0).getUUID());
		sendMessage.setMethod(Command.METHOD_SEND_MESSAGE_TO_GROUP);
		params.setLocalId(rnd.nextLong());
		sendMessage
				.setParams(jsonMapper.convertValue(params, ObjectNode.class));
		commandService.executeCommand(sendMessage, null, false);

		for (ChannelEntity channel : channels) {
			Long channelId = channel.getUUID();
			Iterator<Command> pendingCommands = commandService
					.getPendingCommands(channelId, lastSequences.get(channelId))
					.iterator();
			command = pendingCommands.next();
			assertIsMessage(command);
			lastSequences.put(channelId, command.getSequenceNumber());
			Assert.assertFalse(pendingCommands.hasNext());
		}

	}

	@Test
	public void reactivateTimelineWithTimelineCreateTest() throws IOException {

		testSetup();

		TimelinePartyUpdate party = new TimelinePartyUpdate();
		party.setId(group.getUUID());
		party.setType(PartyType.GROUP.getValue());
		TimelineCreate timelineCreate = new TimelineCreate();
		timelineCreate.setOwnerId(owner.getUUID());
		timelineCreate.setParty(party);

		Command command = new Command();
		command.setChannelId(channels.get(0).getUUID());
		command.setMethod(Command.METHOD_DELETE_TIMELINE);
		command.setParams(timelineCreate);

		utils.login(owner);
		commandService.executeCommand(command, null, false);
		for (ChannelEntity channel : channels) {
			Long channelId = channel.getUUID();
			Iterator<Command> pendingCommands = commandService
					.getPendingCommands(channelId, lastSequences.get(channelId))
					.iterator();
			Command cmd = pendingCommands.next();
			assertTimelineUpdate(cmd);

			lastSequences.put(channelId, cmd.getSequenceNumber());
		}
		// Create timeline
		command.setMethod(Command.METHOD_CREATE_TIMELINE);
		commandService.executeCommand(command, null, false);

		// Send message
		MessageSend params = new MessageSend();
		params.setLocalId(rnd.nextLong());
		params.setFrom(owner.getUUID());
		params.setTo(group.getUUID());
		params.setBody("activation message");

		Command sendMessage = new Command();
		sendMessage.setChannelId(channels.get(0).getUUID());
		sendMessage.setMethod(Command.METHOD_SEND_MESSAGE_TO_GROUP);
		sendMessage
				.setParams(jsonMapper.convertValue(params, ObjectNode.class));
		commandService.executeCommand(sendMessage, null, false);

		for (ChannelEntity channel : channels) {
			Long channelId = channel.getUUID();
			Iterator<Command> pendingCommands = commandService
					.getPendingCommands(channelId, lastSequences.get(channelId))
					.iterator();
			assertTimelineUpdate(pendingCommands.next());
			int i = 0;
			while (pendingCommands.hasNext()) {
				command = pendingCommands.next();
				assertIsMessage(command);
				i++;
			}
			lastSequences.put(channelId, command.getSequenceNumber());
			Assert.assertTrue(i == 41);
			Assert.assertFalse(pendingCommands.hasNext());
		}

		// Verify a second message does not triggers all activation procedure
		sendMessage = new Command();
		sendMessage.setChannelId(channels.get(0).getUUID());
		sendMessage.setMethod(Command.METHOD_SEND_MESSAGE_TO_GROUP);
		params.setLocalId(rnd.nextLong());
		sendMessage
				.setParams(jsonMapper.convertValue(params, ObjectNode.class));
		commandService.executeCommand(sendMessage, null, false);

		for (ChannelEntity channel : channels) {
			Long channelId = channel.getUUID();
			Iterator<Command> pendingCommands = commandService
					.getPendingCommands(channelId, lastSequences.get(channelId))
					.iterator();
			command = pendingCommands.next();
			assertIsMessage(command);
			lastSequences.put(channelId, command.getSequenceNumber());
			Assert.assertFalse(pendingCommands.hasNext());
		}

	}

	private void assertTimelineDeleted(Command command) {
		Assert.assertTrue(Command.METHOD_DELETE_TIMELINE.equals(command
				.getMethod()));
		TimelineCreate timeline = jsonMapper.convertValue(command.getParams(),
				TimelineCreate.class);
		Assert.assertTrue(timeline.getOwnerId().equals(owner.getUUID()));
		Assert.assertTrue(timeline.getParty().getId().equals(group.getUUID()));
		Assert.assertTrue(timeline.getParty().getType()
				.equals(PartyType.GROUP.getValue()));
	}

	private void assertTimelineUpdate(Command command) {
		Assert.assertTrue(Command.METHOD_UPDATE_TIMELINE.equals(command
				.getMethod()));
		TimelineReadResponse timeline = jsonMapper.convertValue(
				command.getParams(), TimelineReadResponse.class);
		Assert.assertTrue(timeline.getOwnerId().equals(owner.getUUID()));
		Assert.assertTrue(timeline.getParty().getId().equals(group.getUUID()));
		Assert.assertTrue(timeline.getParty().getType()
				.equals(PartyType.GROUP.getValue()));
	}

	private void assertIsMessage(Command command) {
		Assert.assertTrue(Command.METHOD_UPDATE_MESSAGE.equals(command
				.getMethod()));
		MessageReadResponse message = jsonMapper.convertValue(
				command.getParams(), MessageReadResponse.class);
		Assert.assertTrue(message.getFrom().getId().equals(owner.getUUID()));
	}

	private void testSetup() throws IOException {
		String name = String.valueOf(rnd.nextInt());
		acc = utils.createAutomanagedAccount();
		owner = utils.createUser(name, acc, false);
		group = utils.createGroup(name, acc, owner, false);
		channels = new ArrayList<ChannelEntity>();
		lastSequences = new HashMap<Long, Long>();

		for (int i = 0; i < 5; i++) {
			ChannelEntity channel = utils.createChannel(owner);
			channels.add(channel);
			lastSequences.put(channel.getUUID(), 0L);
		}

		// Send several messages to create timeline
		for (int i = 0; i < 60; i++) {
			utils.sendMessage("message_from" + owner.getEmail() + "_" + i,
					owner, group);
		}

	}
}
