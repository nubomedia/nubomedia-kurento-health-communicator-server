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
import java.util.List;
import java.util.Map;
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
import com.kurento.agenda.services.pojo.MessageReadResponse;
import com.kurento.agenda.services.pojo.UserReadContactResponse;
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
public class FactoryResetCommandTest {

	private Random rnd = new Random();

	private AccountEntity acc;
	private GroupEntity group;
	private UserEntity user;
	private Map<Long, ChannelEntity> channels;

	@Autowired
	KhcTestUtils utils;

	@Autowired
	CommandService commandService;

	private ObjectMapper jsonMapper = new KhcObjectMapper();

	@Test
	public void testContactSyncronization() throws IOException {
		testSetup();

		utils.login(user);
		Command factoryReset = new Command();
		factoryReset.setMethod(Command.METHOD_FACTORY_RESET);
		factoryReset.setChannelId(channels.get(user.getUUID()).getUUID());
		commandService.executeCommand(factoryReset, null, false);

		assertContactSynchronization(commandService.getPendingCommands(channels
				.get(user.getUUID()).getUUID(), 0L));
	}

	@Test
	public void testMessageSynchronization() throws IOException {
		testSetup();

		utils.login(user);
		Command factoryReset = new Command();
		factoryReset.setMethod(Command.METHOD_FACTORY_RESET);
		factoryReset.setChannelId(channels.get(user.getUUID()).getUUID());
		commandService.executeCommand(factoryReset, null, false);

		assertMessageSinchronization(commandService.getPendingCommands(channels
				.get(user.getUUID()).getUUID(), 0L));
	}

	@Test
	public void testConsecutiveFactoryResetAfterReadCommands()
			throws IOException {
		testSetup();

		// Request factory reset
		utils.login(user);
		Command factoryReset = new Command();
		factoryReset.setId(1L);
		factoryReset.setMethod(Command.METHOD_FACTORY_RESET);
		factoryReset.setChannelId(channels.get(user.getUUID()).getUUID());
		commandService.executeCommand(factoryReset, null, false);

		// Get commands
		List<Command> commands = commandService.getPendingCommands(channels
				.get(user.getUUID()).getUUID(), 0L);
		Long lastSequence = commands.get(commands.size() - 1)
				.getSequenceNumber();
		// Verify first command is factory reset
		Assert.assertTrue(commands.get(0).getMethod()
				.equals(Command.METHOD_FACTORY_RESET));

		// Request factory reset again
		utils.login(user);
		factoryReset = new Command();
		factoryReset.setId(2L);
		factoryReset.setMethod(Command.METHOD_FACTORY_RESET);
		factoryReset.setChannelId(channels.get(user.getUUID()).getUUID());
		commandService.executeCommand(factoryReset, null, false);

		// Get commands from the begining again
		int factoryResetCount = 0;
		List<Command> pendingCommands = commandService.getPendingCommands(
				channels.get(user.getUUID()).getUUID(), 0L);
		for (Command command : pendingCommands) {
			if (command.getMethod().equals(Command.METHOD_FACTORY_RESET)) {
				factoryResetCount++;
			}
		}
		// Once sequence is assigned no further consolidation is allowed
		Assert.assertTrue(factoryResetCount == 2);

		// Verify second sincronization is correct
		pendingCommands = commandService.getPendingCommands(
				channels.get(user.getUUID()).getUUID(), lastSequence);
		assertMessageSinchronization(pendingCommands);
		assertContactSynchronization(pendingCommands);
	}

	@Test
	public void testConsecutiveFactoryResetBeforeReadCommands()
			throws IOException {
		testSetup();

		// Request factory reset
		utils.login(user);
		Command factoryReset = new Command();
		factoryReset.setId(1L);
		factoryReset.setMethod(Command.METHOD_FACTORY_RESET);
		factoryReset.setChannelId(channels.get(user.getUUID()).getUUID());
		commandService.executeCommand(factoryReset, null, false);

		// Request factory reset again
		utils.login(user);
		factoryReset = new Command();
		factoryReset.setId(2L);
		factoryReset.setMethod(Command.METHOD_FACTORY_RESET);
		factoryReset.setChannelId(channels.get(user.getUUID()).getUUID());
		commandService.executeCommand(factoryReset, null, false);

		List<Command> pendingCommands = commandService.getPendingCommands(
				channels.get(user.getUUID()).getUUID(), 0L);

		// Get commands from the begining again
		int factoryResetCount = 0;
		for (Command command : pendingCommands) {
			if (command.getMethod().equals(Command.METHOD_FACTORY_RESET)) {
				factoryResetCount++;
			}
		}
		// As sequence is not assigned first factory reset is removed by
		// consolidation
		Assert.assertTrue(factoryResetCount == 1);

		// Verify second sincronization is correct
		assertMessageSinchronization(pendingCommands);
		assertContactSynchronization(pendingCommands);
	}

	private void testSetup() throws IOException {
		String name = String.valueOf(rnd.nextInt());
		acc = utils.createAutomanagedAccount();
		user = utils.createUser(name, acc, false);
		group = utils.createGroup(name, acc, user, false);
		for (int i = 0; i < 5; i++) {
			UserEntity member = utils.createUser(name + "_" + i, acc, false);
			utils.addGroupMember(group, member);
		}
		channels = new HashMap<Long, ChannelEntity>();
		for (UserEntity member : utils.getGroupMembers(group)) {
			channels.put(member.getUUID(), utils.createChannel(member));
		}
		// Send messages
		int i = 0;
		for (UserEntity member : utils.getGroupMembers(group)) {
			utils.sendMessage("" + i++, member, group);
		}
	}

	private void assertMessageSinchronization(List<Command> pendingCommands) {
		// Receive commands & verify there is one for each message sent in
		// apropriate order
		Map<Long, MessageReadResponse> messages = new HashMap<Long, MessageReadResponse>();
		int i = 0;
		for (Command command : pendingCommands) {
			if (command.getMethod().equals(Command.METHOD_UPDATE_MESSAGE)) {
				MessageReadResponse message = jsonMapper.convertValue(
						command.getParams(), MessageReadResponse.class);
				Assert.assertTrue(message.getBody().equals("" + i++));
				messages.put(message.getFrom().getId(), message);
			}
		}
		for (UserEntity member : utils.getGroupMembers(group)) {
			MessageReadResponse message = messages.remove(member.getUUID());
			Assert.assertNotNull(message);
		}

		Assert.assertTrue(messages.size() == 0);
	}

	private void assertContactSynchronization(List<Command> pendingCommands) {
		// Receive commands & verify there is one for each contatc in group
		Map<Long, UserReadContactResponse> contacts = new HashMap<Long, UserReadContactResponse>();
		for (Command command : pendingCommands) {
			if (command.getMethod().equals(Command.METHOD_UPDATE_CONTACT)) {
				UserReadContactResponse contact = jsonMapper.convertValue(
						command.getParams(), UserReadContactResponse.class);
				contacts.put(contact.getId(), contact);
			}
		}
		for (UserEntity member : utils.getGroupMembers(group)) {
			if (member.getId().equals(user.getId())) {
				continue;
			}
			UserReadContactResponse contact = contacts.remove(member.getUUID());
			Assert.assertNotNull(contact);
		}
		Assert.assertTrue(contacts.size() == 0);
	}
}
