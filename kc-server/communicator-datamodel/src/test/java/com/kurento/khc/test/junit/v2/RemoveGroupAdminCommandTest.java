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

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
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
import com.kurento.agenda.services.pojo.GroupInfo;
import com.kurento.agenda.services.pojo.GroupUpdate;
import com.kurento.agenda.services.pojo.UserReadAvatarResponse;
import com.kurento.khc.datamodel.AccountEntity;
import com.kurento.khc.datamodel.ChannelEntity;
import com.kurento.khc.datamodel.GroupEntity;
import com.kurento.khc.datamodel.UserEntity;
import com.kurento.khc.jackson.KhcObjectMapper;
import com.kurento.khc.services.v2.AdministrativeService;
import com.kurento.khc.services.v2.CommandService;
import com.kurento.khc.spring.KhcDatamodelConfig;
import com.kurento.khc.test.utils.KhcTestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = KhcDatamodelConfig.class)
@ActiveProfiles({ "embed_db", "embed_security" })
public class RemoveGroupAdminCommandTest {

	private Random rnd = new SecureRandom();

	private AccountEntity acc;
	private UserEntity admin;
	private UserEntity removed;
	private GroupEntity group;
	private List<UserEntity> members;
	private Map<Long, ChannelEntity> channels;
	private Map<Long, Long> lastSequence;

	@Autowired
	KhcTestUtils utils;

	@Autowired
	CommandService commandService;

	@Autowired
	AdministrativeService admService;

	private ObjectMapper jsonMapper = new KhcObjectMapper();

	@Test
	public void testRemoveAdminCommand() throws JsonParseException,
			JsonMappingException, IOException {
		testSetup();

		Map<String, Object> params = new HashMap<String, Object>();
		GroupInfo groupParam = admService.buildGroupPojo(group)
				.buildGroupInfo();
		UserReadAvatarResponse userParam = admService.buildUserPojo(removed)
				.buildUserReadAvatarResponse();
		params.put(Command.PARAM_GROUP, groupParam);
		params.put(Command.PARAM_USER, userParam);

		Command command = new Command();
		command.setChannelId(channels.get(admin.getUUID()).getUUID());
		command.setMethod(Command.METHOD_REMOVE_GROUP_ADMIN);
		command.setParams(params);

		utils.login(admin);
		commandService.executeCommand(command, null, false);

		// Current group members must received remove admin notification
		for (UserEntity member : members) {
			utils.login(member);
			Long channelId = channels.get(member.getUUID()).getUUID();
			Iterator<Command> pendingCommands = commandService
					.getPendingCommands(channelId, lastSequence.get(channelId))
					.iterator();
			command = pendingCommands.next();
			assertRemoveAdmin(command);
			lastSequence.put(channelId, command.getSequenceNumber());
			Assert.assertFalse(pendingCommands.hasNext());
		}

		// Removed admin must receive updateGroup with new permission
		utils.login(removed);
		Long channelId = channels.get(removed.getUUID()).getUUID();
		Iterator<Command> pendingCommands = commandService.getPendingCommands(
				channelId, lastSequence.get(channelId)).iterator();
		command = pendingCommands.next();
		assertUpdateGroup(command);
		Assert.assertFalse(pendingCommands.hasNext());

	}

	@Test(expected = AccessDeniedException.class)
	public void testUnableToRemoveMyselfAdmin() throws IOException {
		testSetup();
		Map<String, Object> params = new HashMap<String, Object>();
		GroupInfo groupParam = admService.buildGroupPojo(group)
				.buildGroupInfo();
		UserReadAvatarResponse userParam = admService.buildUserPojo(admin)
				.buildUserReadAvatarResponse();
		params.put(Command.PARAM_GROUP, groupParam);
		params.put(Command.PARAM_USER, userParam);

		Command command = new Command();
		command.setChannelId(channels.get(admin.getUUID()).getUUID());
		command.setMethod(Command.METHOD_REMOVE_GROUP_ADMIN);
		command.setParams(params);

		utils.login(admin);
		commandService.executeCommand(command, null, false);
	}

	private void testSetup() throws IOException {

		String name = rnd.nextInt() + "-" + rnd.nextInt();
		channels = new HashMap<Long, ChannelEntity>();
		lastSequence = new HashMap<Long, Long>();
		acc = utils.createAutomanagedAccount();
		admin = utils.createUser(name, acc, false);
		channels.put(admin.getUUID(), utils.createChannel(admin));
		group = utils.createGroup(name, acc, admin, false);
		removed = utils.createUser(name + "removed", acc, false);
		utils.addGroupAdmin(group, removed);
		channels.put(removed.getUUID(), utils.createChannel(removed));

		members = new ArrayList<UserEntity>();
		for (int i = 0; i < 5; i++) {
			UserEntity user = utils.createRootUser(name + i, acc);
			members.add(user);
			channels.put(user.getUUID(), utils.createChannel(user));
			utils.addGroupMember(group, user);
		}
		for (ChannelEntity channel : channels.values()) {
			lastSequence.put(channel.getUUID(), 0L);
		}
	}

	private void assertRemoveAdmin(Command command) throws JsonParseException,
			JsonMappingException, IOException {
		ObjectNode prms = jsonMapper.convertValue(command.getParams(),
				ObjectNode.class);
		GroupInfo groupParam = jsonMapper.readValue(
				prms.get(Command.PARAM_GROUP), GroupInfo.class);
		UserReadAvatarResponse userParam = jsonMapper.readValue(
				prms.get(Command.PARAM_USER), UserReadAvatarResponse.class);

		Assert.assertTrue(command.getMethod().equals(
				Command.METHOD_REMOVE_GROUP_ADMIN));
		Assert.assertTrue(groupParam.getId().equals(group.getUUID()));
		Assert.assertTrue(userParam.getId().equals(removed.getUUID()));
	}

	private void assertUpdateGroup(Command command) {
		GroupUpdate updateGroupParam = jsonMapper.convertValue(
				command.getParams(), GroupUpdate.class);
		Assert.assertTrue(command.getMethod().equals(
				Command.METHOD_UPDATE_GROUP));
		Assert.assertTrue(updateGroupParam.getId().equals(group.getUUID()));
		Assert.assertTrue(updateGroupParam.getName().equals(group.getName()));
		Assert.assertTrue(updateGroupParam.getCanLeave());
		Assert.assertFalse(updateGroupParam.isAdmin());
	}
}
