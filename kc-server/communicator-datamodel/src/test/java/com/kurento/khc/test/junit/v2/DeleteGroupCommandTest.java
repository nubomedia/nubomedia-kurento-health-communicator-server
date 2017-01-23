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
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
import com.kurento.agenda.datamodel.pojo.Timeline.State;
import com.kurento.agenda.datamodel.pojo.TimelineParty.PartyType;
import com.kurento.agenda.services.pojo.GroupInfo;
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
public class DeleteGroupCommandTest {

	private Random rnd = new SecureRandom();

	private AccountEntity acc, accExt;

	private GroupEntity group;
	private UserEntity user, accAdmin, root, rootExt;
	private List<UserEntity> members;
	private Map<Long, ChannelEntity> channels;

	@Autowired
	KhcTestUtils utils;

	@Autowired
	CommandService commandService;

	private ObjectMapper jsonMapper = new KhcObjectMapper();

	@Test
	public void deleteGroupCommandAutomanagedTest() throws IOException {
		testSetup(false);
		GroupInfo groupParam = new GroupInfo();
		groupParam.setId(group.getUUID());

		Command command = new Command();
		command.setMethod(Command.METHOD_DELETE_GROUP);
		command.setChannelId(channels.get(user.getUUID()).getUUID());
		command.setParams(groupParam);
		List<UserEntity> members = utils.getGroupMembers(group);

		utils.login(user);
		commandService.executeCommand(command, null, false);

		for (UserEntity member : members) {
			utils.login(member);
			Iterator<Command> pendingCommands = commandService
					.getPendingCommands(
							channels.get(member.getUUID()).getUUID(), 0L)
					.iterator();
			if (member.getId().equals(user.getId())) {
				assertTimelineDeletion(pendingCommands.next(), member);
			} else {
				assertTimelineUpdate(pendingCommands.next(), member);
			}
			assertGroupDelete(pendingCommands.next());
			Assert.assertFalse(pendingCommands.hasNext());
		}
	}

	@Test
	public void deleteGroupCommandAutomanagedWithAvatarTest()
			throws IOException {
		testSetup(true);
		// Get list of members before deletion. Later will be imposible
		List<UserEntity> members = utils.getGroupMembers(group);

		GroupInfo groupParam = new GroupInfo();
		groupParam.setId(group.getUUID());

		Command command = new Command();
		command.setMethod(Command.METHOD_DELETE_GROUP);
		command.setChannelId(channels.get(user.getUUID()).getUUID());
		command.setParams(groupParam);

		utils.login(user);
		commandService.executeCommand(command, null, false);

		for (UserEntity member : members) {
			utils.login(member);
			Iterator<Command> pendingCommands = commandService
					.getPendingCommands(
							channels.get(member.getUUID()).getUUID(), 0L)
					.iterator();
			if (member.getId().equals(user.getId())) {
				assertTimelineDeletion(pendingCommands.next(), member);
			} else {
				assertTimelineUpdate(pendingCommands.next(), member);
			}
			assertGroupDelete(pendingCommands.next());
			Assert.assertFalse(pendingCommands.hasNext());
		}
	}


	@Test
	public void testDeleteGroupByAccountAdmin() throws IOException {
		testSetup(true);
		GroupInfo groupParam = new GroupInfo();
		groupParam.setId(group.getUUID());

		Command command = new Command();
		command.setMethod(Command.METHOD_DELETE_GROUP);
		command.setChannelId(channels.get(accAdmin.getUUID()).getUUID());
		command.setParams(groupParam);

		utils.login(accAdmin);
		commandService.executeCommand(command, null, false);
	}

	@Test
	public void testDeleteGroupByRoot() throws IOException {
		testSetup(true);
		GroupInfo groupParam = new GroupInfo();
		groupParam.setId(group.getUUID());

		Command command = new Command();
		command.setMethod(Command.METHOD_DELETE_GROUP);
		command.setChannelId(channels.get(root.getUUID()).getUUID());
		command.setParams(groupParam);

		utils.login(root);
		commandService.executeCommand(command, null, false);

	}

	@Test
	public void testDeleteGroupByExternalRoot() throws IOException {
		testSetup(true);
		GroupInfo groupParam = new GroupInfo();
		groupParam.setId(group.getUUID());

		Command command = new Command();
		command.setMethod(Command.METHOD_DELETE_GROUP);
		command.setChannelId(channels.get(rootExt.getUUID()).getUUID());
		command.setParams(groupParam);

		utils.login(rootExt);
		commandService.executeCommand(command, null, false);

	}

	private void assertGroupDelete(Command command) {
		GroupInfo groupParam = jsonMapper.convertValue(command.getParams(),
				GroupInfo.class);
		Assert.assertTrue(command.getMethod().equals(
				Command.METHOD_DELETE_GROUP));
		Assert.assertTrue(groupParam.getId().equals(group.getUUID()));
	}

	private void assertTimelineDeletion(Command command, UserEntity member) {
		TimelineReadResponse timelineParam = jsonMapper.convertValue(
				command.getParams(), TimelineReadResponse.class);
		Assert.assertTrue(command.getMethod().equals(
				Command.METHOD_DELETE_TIMELINE));
		Assert.assertTrue(timelineParam.getOwnerId().equals(member.getUUID()));
		Assert.assertTrue(timelineParam.getParty().getId()
				.equals(group.getUUID()));
		Assert.assertTrue(PartyType.create(timelineParam.getParty().getType())
				.equals(PartyType.GROUP));
	}

	private void assertTimelineUpdate(Command command, UserEntity member) {
		TimelineReadResponse timelineParam = jsonMapper.convertValue(
				command.getParams(), TimelineReadResponse.class);
		Assert.assertTrue(command.getMethod().equals(
				Command.METHOD_UPDATE_TIMELINE));
		Assert.assertTrue(timelineParam.getOwnerId().equals(member.getUUID()));
		Assert.assertTrue(timelineParam.getParty().getId()
				.equals(group.getUUID()));
		Assert.assertTrue(timelineParam.getState().equals(State.DISABLED));
		Assert.assertTrue(PartyType.create(timelineParam.getParty().getType())
				.equals(PartyType.GROUP));
	}

	private void testSetup(Boolean avatar) throws IOException {
		String name = String.valueOf(rnd.nextInt());
		channels = new HashMap<Long, ChannelEntity>();
		acc = utils.createAutomanagedAccount();
		accExt = utils.createAutomanagedAccount();
		user = utils.createUser(name, acc, false);
		channels.put(user.getUUID(), utils.createChannel(user));
		accAdmin = utils.createUser(name + "admin", acc, false);
		utils.addAccountAdmin(acc, accAdmin);
		channels.put(accAdmin.getUUID(), utils.createChannel(accAdmin));
		root = utils.createRootUser(name + "root", acc);
		channels.put(root.getUUID(), utils.createChannel(root));
		rootExt = utils.createRootUser(name + "rootExt", accExt);
		channels.put(rootExt.getUUID(), utils.createChannel(rootExt));

		// Create Account groups
		group = utils.createGroup(name, acc, user, false);

		for (int i = 0; i < 5; i++) {
			channels.put(user.getUUID(), utils.createChannel(user));
		}

		members = new ArrayList<UserEntity>();
		for (int i = 0; i < 5; i++) {
			UserEntity member = utils.createUser(name + i, acc, false);
			utils.addGroupMember(group, member);
			members.add(member);
			channels.put(member.getUUID(), utils.createChannel(member));
		}

		// Send several messages to create timelines
		for (int i = 0; i < 60; i++) {
			utils.sendMessage("message_from" + user.getEmail() + "_" + i, user,
					group);
		}
	}
}
