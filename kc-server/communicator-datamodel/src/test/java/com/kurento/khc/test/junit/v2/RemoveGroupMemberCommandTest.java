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
import com.kurento.agenda.datamodel.pojo.TimelineParty.PartyType;
import com.kurento.agenda.services.pojo.GroupInfo;
import com.kurento.agenda.services.pojo.TimelineCreate;
import com.kurento.agenda.services.pojo.UserReadAvatarResponse;
import com.kurento.agenda.services.pojo.UserReadNameResponse;
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
public class RemoveGroupMemberCommandTest {

	private Random rnd = new SecureRandom();

	private AccountEntity acc;
	private UserEntity admin;
	private GroupEntity groupA, groupB;
	private Map<Long, ChannelEntity> channels;
	private Map<Long, Long> lastSequence;

	@Autowired
	KhcTestUtils utils;

	@Autowired
	CommandService commandService;

	private ObjectMapper jsonMapper = new KhcObjectMapper();

	@Test
	public void testRemoveGroupMemberCommand() throws IOException {
		testSetup();

		for (UserEntity member : utils.getGroupMembers(groupA)) {
			if (member.getId().equals(admin.getId())) {
				continue; // Do not remove admin
			}
			Assert.assertTrue(utils.isGroupMember(groupA, member));

			Map<String, Object> params = new HashMap<String, Object>();
			GroupInfo groupInfo = new GroupInfo();
			groupInfo.setId(groupA.getUUID());
			UserReadAvatarResponse removedUser = new UserReadAvatarResponse();
			removedUser.setId(member.getUUID());
			params.put(Command.PARAM_GROUP, groupInfo);
			params.put(Command.PARAM_USER, removedUser);

			Command removeMember = new Command();
			removeMember.setChannelId(channels.get(admin.getUUID()).getUUID());
			removeMember.setMethod(Command.METHOD_REMOVE_GROUP_MEMBER);
			removeMember.setParams(params);

			utils.login(admin);
			commandService.executeCommand(removeMember, null, false);

			Assert.assertFalse(utils.isGroupMember(groupA, member));
		}

	}

	@Test
	public void testPropagateRemoveGroupMember() throws JsonParseException,
			JsonMappingException, IOException {
		testSetup();

		for (UserEntity member : utils.getGroupMembers(groupA)) {
			if (member.getId().equals(admin.getId())) {
				continue;
			}
			Map<String, Object> params = new HashMap<String, Object>();
			GroupInfo groupInfo = new GroupInfo();
			groupInfo.setId(groupA.getUUID());
			UserReadAvatarResponse removedUser = new UserReadAvatarResponse();
			removedUser.setId(member.getUUID());
			params.put(Command.PARAM_GROUP, groupInfo);
			params.put(Command.PARAM_USER, removedUser);

			Command removeMember = new Command();
			removeMember.setChannelId(channels.get(admin.getUUID()).getUUID());
			removeMember.setMethod(Command.METHOD_REMOVE_GROUP_MEMBER);
			removeMember.setParams(params);

			utils.login(admin);
			commandService.executeCommand(removeMember, null, false);

			utils.login(member);
			Long channelId = channels.get(member.getUUID()).getUUID();
			Iterator<Command> pendingCommands = commandService
					.getPendingCommands(channelId, lastSequence.get(channelId))
					.iterator();
			Command command = pendingCommands.next();
			// Removed member receives command deleteTimeline
			assertTimelineDeleted(command, member, groupA);
			command = pendingCommands.next();
			// Removed member receives command deleteGroup
			assertGroupDeleted(command, groupA);
			lastSequence.put(channelId, command.getSequenceNumber());

			// Removed member receives deleteContact for all current members,
			// except those shared in other groups
			Map<Long, UserEntity> membersMapToDelete = getMap(utils
					.getGroupMembers(groupA));
			if (utils.isGroupMember(groupB, member)) {
				// Remove members in shared group
				for (UserEntity sharedMember : utils.getGroupMembers(groupB)) {
					membersMapToDelete.remove(sharedMember.getUUID());
				}
			}
			while (pendingCommands.hasNext()) {
				command = pendingCommands.next();
				assertDeleteContact(command, membersMapToDelete);
				lastSequence.put(channelId, command.getSequenceNumber());
			}
			Assert.assertTrue(membersMapToDelete.size() == 0);

			for (UserEntity remainingMember : utils.getGroupMembers(groupA)) {
				// Remaining members always receives removeGroupMember
				utils.login(remainingMember);
				channelId = channels.get(remainingMember.getUUID()).getUUID();
				pendingCommands = commandService.getPendingCommands(channelId,
						lastSequence.get(channelId)).iterator();
				command = pendingCommands.next();
				assertRemoveGroupMember(command, groupA, member);
				lastSequence.put(channelId, command.getSequenceNumber());
				if (!utils.isGroupMember(groupB, member)
						|| !utils.isGroupMember(groupB, remainingMember)) {
					// Not shared group ==> delete contact is also received
					command = pendingCommands.next();
					assertDeleteContact(command, getMap(Arrays.asList(member)));
					lastSequence.put(channelId, command.getSequenceNumber());
				}
				Assert.assertFalse(pendingCommands.hasNext());
			}
		}
	}

	private void testSetup() throws IOException {

		String name = String.valueOf(rnd.nextInt());
		channels = new HashMap<Long, ChannelEntity>();
		lastSequence = new HashMap<Long, Long>();
		acc = utils.createAutomanagedAccount();
		admin = utils.createUser(name, acc, false);
		channels.put(admin.getUUID(), utils.createChannel(admin));
		groupA = utils.createGroup(name + "A", acc, admin, false);
		groupB = utils.createGroup(name + "B", acc, admin, false);

		for (int i = 0; i < 5; i++) {
			UserEntity userBoth = utils.createUser(name + "_both_" + i, acc,
					false);
			channels.put(userBoth.getUUID(), utils.createChannel(userBoth));
			UserEntity userA = utils.createUser(name + "_A_" + i, acc, false);
			channels.put(userA.getUUID(), utils.createChannel(userA));
			utils.addGroupMember(groupA, userA);
			utils.addGroupMember(groupA, userBoth);
			utils.addGroupMember(groupB, userBoth);
		}
		for (ChannelEntity channel : channels.values()) {
			lastSequence.put(channel.getUUID(), 0L);
		}
	}

	private void assertTimelineDeleted(Command command, UserEntity owner,
			GroupEntity party) {
		Assert.assertTrue(Command.METHOD_DELETE_TIMELINE.equals(command
				.getMethod()));
		TimelineCreate timeline = jsonMapper.convertValue(command.getParams(),
				TimelineCreate.class);
		Assert.assertTrue(timeline.getOwnerId().equals(owner.getUUID()));
		Assert.assertTrue(timeline.getParty().getId().equals(party.getUUID()));
		Assert.assertTrue(timeline.getParty().getType()
				.equals(PartyType.GROUP.getValue()));
	}

	private void assertGroupDeleted(Command command, GroupEntity group) {
		GroupInfo groupInfoParam = jsonMapper.convertValue(command.getParams(),
				GroupInfo.class);
		Assert.assertTrue(command.getMethod().equals(
				Command.METHOD_DELETE_GROUP));
		Assert.assertTrue(groupInfoParam.getId().equals(group.getUUID()));
	}

	private void assertDeleteContact(Command command,
			Map<Long, UserEntity> userMap) {
		UserReadNameResponse userParam = jsonMapper.convertValue(
				command.getParams(), UserReadNameResponse.class);
		Assert.assertTrue(command.getMethod().equals(
				Command.METHOD_DELETE_CONTACT));
		UserEntity user = userMap.remove(userParam.getId());
		Assert.assertTrue(userParam.getId().equals(user.getUUID()));
	}

	private void assertRemoveGroupMember(Command command, GroupEntity group,
			UserEntity member) throws JsonParseException, JsonMappingException,
			IOException {
		ObjectNode prms = jsonMapper.convertValue(command.getParams(),
				ObjectNode.class);
		GroupInfo groupParam = jsonMapper.readValue(
				prms.get(Command.PARAM_GROUP), GroupInfo.class);
		UserReadAvatarResponse userParam = jsonMapper.readValue(
				prms.get(Command.PARAM_USER), UserReadAvatarResponse.class);
		Assert.assertTrue(groupParam.getId().equals(group.getUUID()));
		Assert.assertTrue(userParam.getId().equals(member.getUUID()));

	}

	private <T extends BaseEntity> Map<Long, T> getMap(List<T> list) {
		Map<Long, T> map = new HashMap<Long, T>();
		for (T element : list) {
			map.put(element.getUUID(), element);
		}
		return map;
	}
}
