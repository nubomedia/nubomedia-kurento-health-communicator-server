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
import java.util.concurrent.atomic.AtomicLong;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.agenda.datamodel.pojo.Channel;
import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.datamodel.pojo.Topic;
import com.kurento.agenda.services.pojo.AccountId;
import com.kurento.agenda.services.pojo.GroupCreate;
import com.kurento.agenda.services.pojo.GroupUpdate;
import com.kurento.agenda.services.pojo.UserEdit;
import com.kurento.agenda.services.pojo.UserReadResponse;
import com.kurento.agenda.services.pojo.topic.TopicBuilder;
import com.kurento.khc.KhcNotFoundException;
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
public class SubscriptionTest {

	private static final Logger log = LoggerFactory
			.getLogger(SubscriptionTest.class);
	private static final Random rnd = new SecureRandom();
	private AtomicLong sequence = new AtomicLong(0);

	private AccountEntity acc;
	private List<AccountEntity> accList;
	private GroupEntity group;
	private List<GroupEntity> groupList;
	private UserEntity root, admin, adminTwo, user, grpAdmin;
	private List<UserEntity> userList;
	private Map<Long, ChannelEntity> channels;
	private Channel notch;

	@Autowired
	private KhcTestUtils utils;
	@Autowired
	private CommandService cmdService;

	private ObjectMapper jsonMapper = new KhcObjectMapper();

	@Test
	public void testSubscribeService() throws IOException {
		testSetup();

		utils.login(admin);
		notch = cmdService.createSubscription(String.valueOf(rnd.nextInt()));
		cmdService.addSubscriptionTopic(notch, TopicBuilder.toUser(user.getUUID()));

		sendUpdateUser(user);
		sendUpdateGroup(group, grpAdmin);

		utils.login(admin);
		Iterator<Command> pendingCommands = cmdService.getPendingCommands(notch.getId(), 0L)
				.iterator();
		assertIsUserUpdate(pendingCommands.next(), user);
		Assert.assertTrue(!pendingCommands.hasNext());

		utils.login(admin);
		cmdService.addSubscriptionTopic(notch, TopicBuilder.toGroup(group.getUUID()));

		sendUpdateGroup(group, grpAdmin);

		utils.login(admin);
		pendingCommands = cmdService.getPendingCommands(notch.getId(), 0L).iterator();
		Assert.assertTrue("Commands list is empty", pendingCommands.hasNext());
		assertIsGroupUpdate(pendingCommands.next(), group);
		Assert.assertTrue(!pendingCommands.hasNext());
	}

	@Test(expected = KhcNotFoundException.class)
	public void testSubscriptionCleanup() throws InterruptedException,
			IOException {

		testSetup();

		utils.login(admin);
		notch = cmdService.createSubscription(String.valueOf(rnd.nextInt()));
		cmdService.addSubscriptionTopic(notch,
				TopicBuilder.toUser(user.getUUID()));

		Thread.sleep(3000);
		cmdService.getPendingCommands(notch.getId(), 0L);
	}

	@Test
	public void testMultipleSubscription() throws IOException {
		testSetup();

		utils.login(admin);
		notch = cmdService.createSubscription(String.valueOf(rnd.nextInt()));
		cmdService.addSubscriptionTopic(notch, TopicBuilder.toUser(user.getUUID()));

		utils.login(adminTwo);
		Channel notchTwo = cmdService.createSubscription(String.valueOf(rnd.nextInt()));
		cmdService.addSubscriptionTopic(notchTwo, TopicBuilder.toUser(user.getUUID()));

		sendUpdateUser(user);
		sendUpdateGroup(group, grpAdmin);

		utils.login(admin);
		Iterator<Command> pendingCommands = cmdService.getPendingCommands(notch.getId(), 0L)
				.iterator();
		assertIsUserUpdate(pendingCommands.next(), user);
		Assert.assertTrue(!pendingCommands.hasNext());

		utils.login(adminTwo);
		Iterator<Command> pendingCommandsTwo = cmdService.getPendingCommands(notchTwo.getId(), 0L)
				.iterator();
		assertIsUserUpdate(pendingCommandsTwo.next(), user);
		Assert.assertTrue(!pendingCommandsTwo.hasNext());

		utils.login(admin);
		cmdService.addSubscriptionTopic(notch, TopicBuilder.toGroup(group.getUUID()));

		utils.login(adminTwo);
		cmdService.addSubscriptionTopic(notchTwo, TopicBuilder.toGroup(group.getUUID()));

		sendUpdateGroup(group, grpAdmin);

		utils.login(admin);
		pendingCommands = cmdService.getPendingCommands(notch.getId(), 0L).iterator();
		Assert.assertTrue("Commands list is empty", pendingCommands.hasNext());
		assertIsGroupUpdate(pendingCommands.next(), group);
		Assert.assertTrue(!pendingCommands.hasNext());

		utils.login(adminTwo);
		pendingCommandsTwo = cmdService.getPendingCommands(notchTwo.getId(), 0L).iterator();
		Assert.assertTrue("Commands list Two is empty", pendingCommandsTwo.hasNext());
		assertIsGroupUpdate(pendingCommandsTwo.next(), group);
		Assert.assertTrue(!pendingCommandsTwo.hasNext());
	}

	@Test
	public void testAccountSubscription() throws IOException {
		testSetup();

		utils.login(admin);
		notch = cmdService.createSubscription(String.valueOf(rnd.nextInt()));
		cmdService.addSubscriptionTopic(notch,
				TopicBuilder.toAccount(acc.getUUID()));

		for (GroupEntity group : groupList) {
			sendUpdateGroup(group, admin);
			utils.login(admin);
			Assert.assertTrue(cmdService.getPendingCommands(notch.getId(), 0L)
					.size() == 0);
		}
		for (UserEntity user : userList) {
			sendUpdateUser(user);
			utils.login(admin);
			Assert.assertTrue(cmdService.getPendingCommands(notch.getId(), 0L)
					.size() == 0);
		}
	}

	@Test
	public void testAccountGroupsSubscription() throws IOException {
		testSetup();

		utils.login(admin);
		notch = cmdService.createSubscription(String.valueOf(rnd.nextInt()));
		cmdService.addSubscriptionTopic(notch,
				TopicBuilder.toAccount(acc.getUUID()).toGroups());
		List<Command> rc = new ArrayList<Command>();

		// Change name of existing group (this must be received)
		for (GroupEntity group : groupList) {
			sendUpdateGroup(group, admin);
			utils.login(admin);
			rc.addAll(cmdService.getPendingCommands(notch.getId(), 0L));
		}
		// Change user names (not expected)
		for (UserEntity user : userList) {
			sendUpdateUser(user);
			utils.login(admin);
			rc.addAll(cmdService.getPendingCommands(notch.getId(), 0L));
		}
		// Add new group (this must be received)
		sendCreateGroup(acc, admin);

		// Add new group in foreing account (not expected)
		for (AccountEntity account : accList) {
			GroupEntity group = utils.createGroup("" + rnd.nextInt(), account);
			sendUpdateGroup(group, root);
			utils.login(admin);
			rc.addAll(cmdService.getPendingCommands(notch.getId(), 0L));
		}

		Iterator<Command> pendingCommands = rc.iterator();

		for (GroupEntity group : groupList) {
			assertIsGroupUpdate(pendingCommands.next(), group);
		}

		assertIsGroupUpdate(pendingCommands.next());

		Assert.assertTrue(!pendingCommands.hasNext());
	}

	@Ignore
	@Test
	public void testQueuePersistence() throws IOException {
		testSetup();

		utils.login(admin);
		notch = cmdService.createSubscription(String.valueOf(rnd.nextInt()));
		Topic topic = TopicBuilder.toAccount(acc.getUUID()).toGroups();
		cmdService.addSubscriptionTopic(notch, topic);
		Long lastSequence = 0L;
		for (int i = 0; i < 200; i++) {
			log.debug("Get commands for subscription: {}", topic.getTopic());
			List<Command> pc = cmdService.getPendingCommands(notch.getId(),
					lastSequence);
			if (pc.size() > 0)
				lastSequence = pc.get(pc.size() - 1).getSequenceNumber();
			try {
				Thread.sleep(4000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	private void sendUpdateUser(UserEntity user) {
		String newName = "" + rnd.nextInt();
		UserEdit params = new UserEdit();
		params.setId(user.getUUID());
		params.setName(newName);

		Command updateUser = new Command();
		updateUser.setChannelId(channels.get(user.getUUID()).getUUID());
		updateUser.setMethod(Command.METHOD_UPDATE_USER);
		updateUser.setParams(params);
		updateUser.setSequenceNumber(sequence.getAndIncrement());
		utils.login(user);
		cmdService.executeCommand(updateUser, null, false);
	}

	private void sendUpdateGroup(GroupEntity group, UserEntity admin) {
		String newName = "" + rnd.nextInt();
		GroupUpdate params = new GroupUpdate();
		params.setId(group.getUUID());
		params.setName(newName);

		Command updateGroup = new Command();
		updateGroup.setChannelId(channels.get(admin.getUUID()).getUUID());
		updateGroup.setMethod(Command.METHOD_UPDATE_GROUP);
		updateGroup.setParams(params);
		updateGroup.setSequenceNumber(sequence.getAndIncrement());
		utils.login(admin);
		cmdService.executeCommand(updateGroup, null, false);
	}

	private void sendCreateGroup(AccountEntity account, UserEntity admin) {
		String newName = "" + rnd.nextInt();
		GroupCreate paramGroup = new GroupCreate();
		paramGroup.setName(newName);
		AccountId paramAcc = new AccountId();
		paramAcc.setId(account.getUUID());
		Map<String, Object> params = new HashMap<String, Object>();
		params.put(Command.PARAM_GROUP, paramGroup);
		params.put(Command.PARAM_ACCOUNT, paramAcc);

		Command createGroup = new Command();
		createGroup.setChannelId(channels.get(admin.getUUID()).getUUID());
		createGroup.setMethod(Command.METHOD_CREATE_GROUP);
		createGroup.setParams(params);
		createGroup.setSequenceNumber(sequence.getAndIncrement());
		utils.login(admin);
		cmdService.executeCommand(createGroup, null, false);
	}

	private void assertIsUserUpdate(Command cmd, UserEntity user) {
		UserReadResponse userParam = jsonMapper.convertValue(cmd.getParams(),
				UserReadResponse.class);
		Assert.assertTrue(cmd.getMethod().equals(Command.METHOD_UPDATE_USER));
		Assert.assertTrue(userParam.getId().equals(user.getUUID()));
	}

	private void assertIsGroupUpdate(Command cmd, GroupEntity group) {
		GroupUpdate groupParam = jsonMapper.convertValue(cmd.getParams(),
				GroupUpdate.class);
		Assert.assertTrue(cmd.getMethod().equals(Command.METHOD_UPDATE_GROUP));
		Assert.assertTrue(groupParam.getId().equals(group.getUUID()));
	}

	private void assertIsGroupUpdate(Command cmd) {
		Assert.assertTrue(cmd.getMethod().equals(Command.METHOD_UPDATE_GROUP));
	}

	private void testSetup() throws IOException {
		channels = new HashMap<Long, ChannelEntity>();
		String name = "" + rnd.nextInt();
		acc = utils.createAutomanagedAccount();
		group = utils.createGroup(name, acc);
		user = utils.createUser(name + "-user", acc, false);
		channels.put(user.getUUID(), utils.createChannel(user));
		utils.addGroupMember(group, user);
		grpAdmin = utils.createUser(name + "-grpAdmin", acc, false);
		channels.put(grpAdmin.getUUID(), utils.createChannel(grpAdmin));
		utils.addGroupAdmin(group, grpAdmin);

		admin = utils.createUser(name + "-subs", acc, false);
		channels.put(admin.getUUID(), utils.createChannel(admin));
		utils.addAccountAdmin(acc, admin);

		adminTwo = utils.createUser(name + "-subs-two", acc, false);
		channels.put(adminTwo.getUUID(), utils.createChannel(adminTwo));
		utils.addAccountAdmin(acc, adminTwo);

		root = utils.createRootUser(name + "root", acc);
		channels.put(root.getUUID(), utils.createChannel(root));

		accList = new ArrayList<AccountEntity>();
		for (int i = 0; i < 10; i++) {
			accList.add(utils.createAutomanagedAccount());
		}
		groupList = new ArrayList<GroupEntity>();
		for (int i = 0; i < 10; i++) {
			groupList.add(utils.createGroup(name + "-" + i, acc));
		}
		userList = new ArrayList<UserEntity>();
		for (int i = 0; i < 10; i++) {
			UserEntity user = utils.createUser(name + "-user-" + i, acc, false);
			userList.add(user);
			channels.put(user.getUUID(), utils.createChannel(user));
		}

	}
}
