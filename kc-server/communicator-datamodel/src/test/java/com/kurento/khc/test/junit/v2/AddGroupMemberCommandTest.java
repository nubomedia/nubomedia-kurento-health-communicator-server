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
import java.util.Arrays;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.services.pojo.GroupInfo;
import com.kurento.agenda.services.pojo.GroupUpdate;
import com.kurento.agenda.services.pojo.UserReadAvatarResponse;
import com.kurento.agenda.services.pojo.UserReadContactResponse;
import com.kurento.khc.datamodel.AccountEntity;
import com.kurento.khc.datamodel.BaseEntity;
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
public class AddGroupMemberCommandTest {

	private Random rnd = new Random();

	private AccountEntity acc, accExt;
	private UserEntity grpAdmin, accAdmin, root, rootExt;
	private GroupEntity group;
	private List<UserEntity> nonMembers;
	private List<UserEntity> members;
	private Map<Long, ChannelEntity> channels;
	private Map<Long, Long> lastSequence;

	@Autowired
	KhcTestUtils utils;

	@Autowired
	CommandService commandService;

	private ObjectMapper jsonMapper = new KhcObjectMapper();

	@Test
	public void testAddGroupMemberCommandByGroupAdmin()
 throws IOException {
		testSetup();

		for (int i = 0; i < 5; i++) {
			// Verify user is not member
			Assert.assertFalse(utils.isGroupMember(group, nonMembers.get(i)));

			Command createGroup = buildGroupCommand(grpAdmin, nonMembers.get(i));

			utils.login(grpAdmin);
			commandService.executeCommand(createGroup, null, false);

			Assert.assertTrue(utils.isGroupMember(group, nonMembers.get(i)));
		}
	}

	public void testAddGroupMemberCommandByAccountAdmin()
 throws IOException {
		testSetup();

		for (int i = 0; i < 5; i++) {
			// Verify user is not member
			Assert.assertFalse(utils.isGroupMember(group, nonMembers.get(i)));

			Command createGroup = buildGroupCommand(accAdmin, nonMembers.get(i));

			utils.login(accAdmin);
			commandService.executeCommand(createGroup, null, false);

			Assert.assertTrue(utils.isGroupMember(group, nonMembers.get(i)));
		}
	}

	public void testAddGroupMemberCommandByRoot() throws IOException {
		testSetup();

		for (int i = 0; i < 5; i++) {
			// Verify user is not member
			Assert.assertFalse(utils.isGroupMember(group, nonMembers.get(i)));

			Command createGroup = buildGroupCommand(root, nonMembers.get(i));

			utils.login(root);
			commandService.executeCommand(createGroup, null, false);

			Assert.assertTrue(utils.isGroupMember(group, nonMembers.get(i)));
		}
	}

	public void testAddGroupMemberCommandByExternalRoot()
 throws IOException {
		testSetup();

		for (int i = 0; i < 5; i++) {
			// Verify user is not member
			Assert.assertFalse(utils.isGroupMember(group, nonMembers.get(i)));

			Command createGroup = buildGroupCommand(rootExt, nonMembers.get(i));

			utils.login(rootExt);
			commandService.executeCommand(createGroup, null, false);

			Assert.assertTrue(utils.isGroupMember(group, nonMembers.get(i)));
		}
	}

	Command buildGroupCommand(UserEntity admin, UserEntity user) {
		Map<String, Object> params = new HashMap<String, Object>();
		GroupInfo groupInfo = new GroupInfo();
		groupInfo.setId(group.getUUID());
		UserReadAvatarResponse member = new UserReadAvatarResponse();
		member.setId(user.getUUID());
		params.put(Command.PARAM_GROUP, groupInfo);
		params.put(Command.PARAM_USER, member);

		Command createGroup = new Command();
		createGroup.setChannelId(channels.get(admin.getUUID()).getUUID());
		createGroup.setMethod(Command.METHOD_ADD_GROUP_MEMBER);
		createGroup.setParams(params);
		return createGroup;
	}

	@Test
	public void testAddGroupMemberPropagation() throws JsonParseException,
			JsonMappingException, IOException {
		testSetup();

		for (int i = 0; i < nonMembers.size(); i++) {
			UserEntity newMember = nonMembers.get(i);
			// Update member list
			members = utils.getGroupMembers(group);

			Assert.assertFalse(utils.isGroupMember(group, newMember));

			Map<String, Object> params = new HashMap<String, Object>();
			GroupInfo groupParam = new GroupInfo();
			groupParam.setId(group.getUUID());
			UserReadAvatarResponse memberParam = new UserReadAvatarResponse();
			memberParam.setId(newMember.getUUID());
			params.put(Command.PARAM_GROUP, groupParam);
			params.put(Command.PARAM_USER, memberParam);

			Command addGroupMember = new Command();
			addGroupMember.setChannelId(channels.get(grpAdmin.getUUID())
					.getUUID());
			addGroupMember.setMethod(Command.METHOD_ADD_GROUP_MEMBER);
			addGroupMember.setParams(params);

			utils.login(grpAdmin);
			commandService.executeCommand(addGroupMember, null, false);
			Assert.assertTrue(utils.isGroupMember(group, newMember));

			// Joined member must receive command updateGroup
			utils.login(newMember);
			Long channelId = channels.get(newMember.getUUID()).getUUID();
			Iterator<Command> pendingCommands = commandService
					.getPendingCommands(channelId, lastSequence.get(channelId))
					.iterator();
			assertUpdateGroupCommand(pendingCommands.next());

			Map<Long, UserEntity> memberMapForUpdateGroup = getMap(members);
			Map<Long, UserEntity> memberMapForAddMember = getMap(members);
			// Joined member must receive updateContact & addGroupMember for
			// each current member & addgroupAdmin for admins
			for (int m = 0; m < members.size(); m++) {
				Command command = pendingCommands.next();
				assertUpdateContact(command, memberMapForUpdateGroup);

				command = pendingCommands.next();
				UserEntity removed = assertAddGroupMember(command, group,
						memberMapForAddMember);

				// Joined member must receive addGroupadmin for each admin
				if (utils.isGroupAdmin(group, removed)) {
					command = pendingCommands.next();
					assertAddGroupAdmin(command, group, grpAdmin);
				}
				lastSequence.put(channelId, command.getSequenceNumber());

			}

			Assert.assertFalse(pendingCommands.hasNext());
			Assert.assertTrue(memberMapForAddMember.size() == 0);
			Assert.assertTrue(memberMapForUpdateGroup.size() == 0);

			// All group members must receive command updateContact &
			// addGroupMember for joined member
			for (UserEntity member : members) {
				Map<Long, UserEntity> newMemberMapForUpdateJoinedMember = getMap(Arrays
						.asList(newMember));
				Map<Long, UserEntity> newMemberMapForAddJoinedMember = getMap(Arrays
						.asList(newMember));
				utils.login(member);
				channelId = channels.get(member.getUUID()).getUUID();
				pendingCommands = commandService.getPendingCommands(channelId,
						lastSequence.get(channelId)).iterator();
				Command command = pendingCommands.next();
				assertUpdateContact(command, newMemberMapForUpdateJoinedMember);
				lastSequence.put(channelId, command.getSequenceNumber());
				command = pendingCommands.next();
				assertAddGroupMember(command, group,
						newMemberMapForAddJoinedMember);
				lastSequence.put(channelId, command.getSequenceNumber());
				Assert.assertFalse(pendingCommands.hasNext());
				Assert.assertTrue(newMemberMapForUpdateJoinedMember.size() == 0);
				Assert.assertTrue(newMemberMapForAddJoinedMember.size() == 0);
			}

			// All non members must receive nothing
			for (int j = i + 1; j < 5; j++) {
				utils.login(nonMembers.get(j));
				channelId = channels.get(nonMembers.get(j).getUUID()).getUUID();
				pendingCommands = commandService.getPendingCommands(channelId,
						lastSequence.get(channelId)).iterator();
				Assert.assertFalse(pendingCommands.hasNext());
			}
		}
	}

	private void testSetup() throws IOException {
		String name = String.valueOf(rnd.nextInt());
		nonMembers = new ArrayList<UserEntity>();
		members = new ArrayList<UserEntity>();
		channels = new HashMap<Long, ChannelEntity>();
		lastSequence = new HashMap<Long, Long>();

		acc = utils.createAutomanagedAccount();
		accExt = utils.createAutomanagedAccount();
		grpAdmin = utils.createUser(name, acc, false);
		channels.put(grpAdmin.getUUID(), utils.createChannel(grpAdmin));
		accAdmin = utils.createUser(name + "accAdmin", acc, false);
		channels.put(accAdmin.getUUID(), utils.createChannel(accAdmin));
		utils.addAccountAdmin(acc, accAdmin);
		root = utils.createRootUser(name + "root", acc);
		channels.put(root.getUUID(), utils.createChannel(root));
		rootExt = utils.createRootUser(name + "rootExt", accExt);
		channels.put(rootExt.getUUID(), utils.createChannel(rootExt));

		lastSequence.put(channels.get(grpAdmin.getUUID()).getUUID(), 0L);

		group = utils.createGroup(name, acc, grpAdmin, false);

		// Create users
		for (int i = 0; i < 5; i++) {
			// Create member
			UserEntity user = utils.createUser(name + "_member_" + i, acc,
					false);
			utils.addGroupMember(group, user);
			channels.put(user.getUUID(), utils.createChannel(user));
			lastSequence.put(channels.get(user.getUUID()).getUUID(), 0L);

			// Create non member
			user = utils.createUser(name + "_non-member_" + i, acc, false);
			nonMembers.add(user);
			channels.put(user.getUUID(), utils.createChannel(user));
			lastSequence.put(channels.get(user.getUUID()).getUUID(), 0L);
		}

	}

	private void assertUpdateGroupCommand(Command command) {
		GroupUpdate updateGroupParam = jsonMapper.convertValue(
				command.getParams(), GroupUpdate.class);
		Assert.assertTrue(command.getMethod().equals(
				Command.METHOD_UPDATE_GROUP));
		Assert.assertTrue(updateGroupParam.getId().equals(group.getUUID()));
		Assert.assertTrue(updateGroupParam.getName().equals(group.getName()));
		Assert.assertTrue(updateGroupParam.getCanLeave());
		Assert.assertTrue(!updateGroupParam.isAdmin());
	}

	private void assertUpdateContact(Command command,
			Map<Long, UserEntity> userMap) throws JsonParseException,
			JsonMappingException, IOException {
		Assert.assertTrue(command.getMethod().equals(
				Command.METHOD_UPDATE_CONTACT));
		UserReadContactResponse userParam = jsonMapper.convertValue(
				command.getParams(), UserReadContactResponse.class);
		UserEntity user = userMap.remove(userParam.getId());
		Assert.assertTrue(user != null);
		Assert.assertTrue(userParam.getName().equals(user.getName()));
		Assert.assertTrue(userParam.getSurname().equals(user.getSurname()));
	}

	private UserEntity assertAddGroupMember(Command command, GroupEntity group,
			Map<Long, UserEntity> userMap) throws JsonParseException,
			JsonMappingException, IOException {
		ObjectNode prms = jsonMapper.convertValue(command.getParams(),
				ObjectNode.class);
		GroupInfo groupParam = jsonMapper.readValue(
				prms.get(Command.PARAM_GROUP), GroupInfo.class);
		UserReadAvatarResponse userParam = jsonMapper.readValue(
				prms.get(Command.PARAM_USER), UserReadAvatarResponse.class);
		Assert.assertTrue(command.getMethod().equals(
				Command.METHOD_ADD_GROUP_MEMBER));
		UserEntity user = null;
		Assert.assertTrue((user = userMap.remove(userParam.getId())) != null);
		Assert.assertTrue(groupParam.getId().equals(group.getUUID()));
		return user;
	}

	private void assertAddGroupAdmin(Command command, GroupEntity group,
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
		Assert.assertTrue(groupParam.getId().equals(group.getUUID()));
		Assert.assertTrue(userParam.getId().equals(admin.getUUID()));
	}

	private <T extends BaseEntity> Map<Long, T> getMap(List<T> list) {
		Map<Long, T> map = new HashMap<Long, T>();
		for (T element : list) {
			map.put(element.getUUID(), element);
		}
		return map;
	}
}
