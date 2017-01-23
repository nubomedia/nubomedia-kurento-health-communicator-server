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
import java.util.HashMap;
import java.util.Iterator;
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
import com.kurento.khc.services.v2.CommandService;
import com.kurento.khc.spring.KhcDatamodelConfig;
import com.kurento.khc.test.utils.KhcTestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = KhcDatamodelConfig.class)
@ActiveProfiles({ "embed_db", "embed_security" })
public class AddGroupAdminCommandTest {

	private Random rnd = new SecureRandom();

	private AccountEntity acc, accExt;
	private GroupEntity group;
	private UserEntity user, grpAdmin, accAdmin, root, rootExt;
	private Map<Long, ChannelEntity> channels;
	private Map<Long, Long> lastSequence;

	@Autowired
	KhcTestUtils utils;

	@Autowired
	CommandService commandService;

	private ObjectMapper jsonMapper = new KhcObjectMapper();
	
	@Test
	public void testAddGroupAdminCommand() throws JsonParseException,
			JsonMappingException, IOException {
		testSetup();
		for (UserEntity member : utils.getGroupMembers(group)) {
			if (utils.isGroupAdmin(group, member)) {
				continue;
			}

			Assert.assertFalse(utils.isGroupAdmin(group, member));
			Command command = buildGroupCommand(grpAdmin, member);

			utils.login(grpAdmin);
			commandService.executeCommand(command, null, false);

			Assert.assertTrue(utils.isGroupAdmin(group, member));

			// Process propagated commands
			for (UserEntity m : utils.getGroupMembers(group)) {
				utils.login(m);
				Long channelId = channels.get(m.getUUID()).getUUID();
				Iterator<Command> pendingCommands = commandService
						.getPendingCommands(channelId,
								lastSequence.get(channelId)).iterator();
				command = pendingCommands.next();
				if (m.getId().equals(member.getId())) {
					// Promoted member receives updateGroup with new permissions
					assertUpdateGroupCommand(command, group);
				} else {
					// All other members receive command addGroupAdmin
					assertAddGroupAdminCommand(command, group, member);

				}
				lastSequence.put(channelId, command.getSequenceNumber());
				Assert.assertFalse(pendingCommands.hasNext());
			}
		}

	}

	@Test
	public void testAddGroupAdminByAccountAdmin() throws IOException {
		testSetup();
		Assert.assertFalse(utils.isGroupAdmin(group, user));
		Command command = buildGroupCommand(accAdmin, user);
		utils.login(accAdmin);
		commandService.executeCommand(command, null, false);
		Assert.assertTrue(utils.isGroupAdmin(group, user));
	}

	@Test
	public void testAddGroupAdminByRoot() throws IOException {
		testSetup();
		Assert.assertFalse(utils.isGroupAdmin(group, user));
		Command command = buildGroupCommand(root, user);
		utils.login(root);
		commandService.executeCommand(command, null, false);
		Assert.assertTrue(utils.isGroupAdmin(group, user));
	}

	@Test
	public void testAddGroupAdminByExternalRoot() throws IOException {
		testSetup();
		Assert.assertFalse(utils.isGroupAdmin(group, user));
		Command command = buildGroupCommand(rootExt, user);
		utils.login(rootExt);
		commandService.executeCommand(command, null, false);
		Assert.assertTrue(utils.isGroupAdmin(group, user));
	}

	private void testSetup() throws IOException {
		String name = "" + rnd.nextInt();
		channels = new HashMap<Long, ChannelEntity>();
		lastSequence = new HashMap<Long, Long>();

		acc = utils.createAutomanagedAccount();
		accExt = utils.createAutomanagedAccount();
		user = utils.createUser(name + "user", acc, false);
		grpAdmin = utils.createUser(name + "grpAdm", acc, false);
		channels.put(grpAdmin.getUUID(), utils.createChannel(grpAdmin));
		accAdmin = utils.createUser(name + "accAdmin", acc, false);
		channels.put(accAdmin.getUUID(), utils.createChannel(accAdmin));
		utils.addAccountAdmin(acc, accAdmin);
		root = utils.createRootUser(name + "root", acc);
		channels.put(root.getUUID(), utils.createChannel(root));
		rootExt = utils.createRootUser(name + "rootExt", accExt);
		channels.put(rootExt.getUUID(), utils.createChannel(rootExt));

		group = utils.createGroup(name, acc, grpAdmin, false);

		for (int i = 0; i < 5; i++) {
			UserEntity member = utils.createUser(name + i, acc, false);
			utils.addGroupMember(group, member);
		}

		for (UserEntity user : utils.getGroupMembers(group)) {
			ChannelEntity channel = utils.createChannel(user);
			channels.put(user.getUUID(), channel);
			lastSequence.put(channel.getUUID(), 0L);
		}
	}

	private Command buildGroupCommand(UserEntity admin, UserEntity member) {
		Command command = new Command();
		command.setMethod(Command.METHOD_ADD_GROUP_ADMIN);
		command.setChannelId(channels.get(admin.getUUID()).getUUID());
		GroupInfo groupParam = new GroupInfo();
		groupParam.setId(group.getUUID());
		UserReadAvatarResponse userParam = new UserReadAvatarResponse();
		userParam.setId(member.getUUID());
		Map<String, Object> params = new HashMap<String, Object>();
		params.put(Command.PARAM_GROUP, groupParam);
		params.put(Command.PARAM_USER, userParam);
		command.setParams(params);
		return command;
	}

	private void assertAddGroupAdminCommand(Command command, GroupEntity group,
			UserEntity admin) throws JsonParseException, JsonMappingException,
			IOException {
		ObjectNode prms = jsonMapper.convertValue(command.getParams(),
				ObjectNode.class);
		GroupInfo groupParam = jsonMapper.readValue(
				prms.get(Command.PARAM_GROUP), GroupInfo.class);
		UserReadAvatarResponse userParam = jsonMapper.readValue(
				prms.get(Command.PARAM_USER), UserReadAvatarResponse.class);
		Assert.assertTrue(command.getMethod().equals(
				Command.METHOD_ADD_GROUP_ADMIN));
		Assert.assertTrue(admin.getUUID().equals(userParam.getId()));
		Assert.assertTrue(groupParam.getId().equals(group.getUUID()));
	}

	private void assertUpdateGroupCommand(Command command, GroupEntity group) {
		GroupUpdate updateGroupParam = jsonMapper.convertValue(
				command.getParams(), GroupUpdate.class);
		Assert.assertTrue(command.getMethod().equals(
				Command.METHOD_UPDATE_GROUP));
		Assert.assertTrue(updateGroupParam.getId().equals(group.getUUID()));
		Assert.assertTrue(updateGroupParam.getName().equals(group.getName()));
		Assert.assertTrue(updateGroupParam.getCanLeave());
		Assert.assertTrue(updateGroupParam.isAdmin());
	}

}
