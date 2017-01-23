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
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.codehaus.jackson.map.ObjectMapper;
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
public class CreateTimelineCommandTest {

	private Random rnd = new Random();

	private AccountEntity acc;
	private GroupEntity groupNew;
	private GroupEntity groupExisting;
	private UserEntity owner;
	private List<ChannelEntity> channels;

	@Autowired
	KhcTestUtils utils;

	@Autowired
	CommandService commandService;

	private ObjectMapper jsonMapper = new KhcObjectMapper();

	@Test
	public void createNonExistingTimeline() throws IOException {

		testSetup();

		TimelinePartyUpdate party = new TimelinePartyUpdate();
		party.setId(groupNew.getUUID());
		party.setType(PartyType.GROUP.getValue());
		TimelineCreate timelineCreate = new TimelineCreate();
		timelineCreate.setOwnerId(owner.getUUID());
		timelineCreate.setParty(party);

		Command command = new Command();
		command.setChannelId(channels.get(0).getUUID());
		command.setMethod(Command.METHOD_CREATE_TIMELINE);
		command.setParams(timelineCreate);

		utils.login(owner);
		commandService.executeCommand(command, null, false);

		for (ChannelEntity channel : channels) {
			Iterator<Command> pendingCommands = commandService
					.getPendingCommands(channel.getUUID(), 0L).iterator();
			assertIsTimelineUpdate(pendingCommands.next(), groupNew);
			Assert.assertFalse(pendingCommands.hasNext());
		}
	}

	@Test
	public void createExistingAndEnabledTimeline() throws IOException {
		testSetup();

		TimelinePartyUpdate party = new TimelinePartyUpdate();
		party.setId(groupExisting.getUUID());
		party.setType(PartyType.GROUP.getValue());
		TimelineCreate timelineCreate = new TimelineCreate();
		timelineCreate.setOwnerId(owner.getUUID());
		timelineCreate.setParty(party);

		Command command = new Command();
		command.setChannelId(channels.get(0).getUUID());
		command.setMethod(Command.METHOD_CREATE_TIMELINE);
		command.setParams(timelineCreate);

		utils.login(owner);
		commandService.executeCommand(command, null, false);

		for (ChannelEntity channel : channels) {
			Iterator<Command> pendingCommands = commandService
					.getPendingCommands(channel.getUUID(), 0L).iterator();
			assertIsTimelineUpdate(pendingCommands.next(), groupExisting);
			for (int i = 20; i < 60; i++) {
				command = pendingCommands.next();
				Assert.assertTrue(command.getMethod().equals(
						Command.METHOD_UPDATE_MESSAGE));
				MessageReadResponse messageParam = jsonMapper.convertValue(
						command.getParams(), MessageReadResponse.class);
				Assert.assertTrue(messageParam.getBody().equals(
						"message_from" + owner.getEmail() + "_" + i));
				Assert.assertTrue(messageParam.getFrom().getId()
						.equals(owner.getUUID()));
			}
			Assert.assertFalse(pendingCommands.hasNext());
		}
	}

	private void assertIsTimelineUpdate(Command command, GroupEntity group) {
		Assert.assertTrue(Command.METHOD_UPDATE_TIMELINE.equals(command
				.getMethod()));
		TimelineReadResponse timeline = jsonMapper.convertValue(
				command.getParams(), TimelineReadResponse.class);
		Assert.assertTrue(timeline.getOwnerId().equals(owner.getUUID()));
		Assert.assertTrue(timeline.getParty().getId().equals(group.getUUID()));
		Assert.assertTrue(timeline.getParty().getType()
				.equals(PartyType.GROUP.getValue()));
	}

	private void testSetup() throws IOException {
		String name = String.valueOf(rnd.nextInt());
		acc = utils.createAutomanagedAccount();
		owner = utils.createUser(name, acc, false);
		groupNew = utils.createGroup(name, acc, owner, false);
		groupExisting = utils.createGroup(name, acc, owner, false);
		channels = new ArrayList<ChannelEntity>();

		for (int i = 0; i < 5; i++) {
			channels.add(utils.createChannel(owner));
		}

		// Send several messages to create timeline
		for (int i = 0; i < 60; i++) {
			utils.sendMessage("message_from" + owner.getEmail() + "_" + i,
					owner, groupExisting);
		}
	}
}
