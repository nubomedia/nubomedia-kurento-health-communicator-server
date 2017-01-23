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
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.datamodel.pojo.Content;
import com.kurento.agenda.services.pojo.AccountId;
import com.kurento.agenda.services.pojo.GroupCreate;
import com.kurento.agenda.services.pojo.GroupUpdate;
import com.kurento.khc.datamodel.AccountEntity;
import com.kurento.khc.datamodel.ChannelEntity;
import com.kurento.khc.datamodel.UserEntity;
import com.kurento.khc.jackson.KhcObjectMapper;
import com.kurento.khc.services.v2.CommandService;
import com.kurento.khc.services.v2.ContentService;
import com.kurento.khc.spring.KhcDatamodelConfig;
import com.kurento.khc.test.utils.KhcTestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = KhcDatamodelConfig.class)
@ActiveProfiles({ "embed_db", "embed_security" })
public class CreateGroupCommandTest {

	private Random rnd = new Random();

	private AccountEntity acc, accExt;
	private UserEntity user, accAdmin, root, rootExt;
	private Map<Long, ChannelEntity> channels;

	@Autowired
	KhcTestUtils utils;

	@Autowired
	CommandService commandService;
	@Autowired
	ContentService contentService;

	private ObjectMapper jsonMapper = new KhcObjectMapper();

	@Test
	public void testCreateAutomanagedGroup() throws IOException {
		testSetup();

		Command createGroup = buildCreateGroupCommand(channels.get(user
				.getUUID()));

		utils.login(user);
		commandService.executeCommand(createGroup, null, false);
	}

	@Test(expected = AccessDeniedException.class)
	public void testCreateAutomanagedGroupWithManagedAccount()
			throws IOException {
		testSetup();
		acc = utils.createAccount();

		Command createGroup = buildCreateGroupCommand(channels.get(user
				.getUUID()));

		utils.login(user);
		commandService.executeCommand(createGroup, null, false);
	}

	@Test
	public void testPropagateCreateAutomanagedGroup()
 throws IOException {
		testSetup();

		Command createGroup = buildCreateGroupCommand(channels.get(user
				.getUUID()));

		utils.login(user);
		commandService.executeCommand(createGroup, null, false);

		// verify command updateGroup is received in any of the user's channels
		for (ChannelEntity channel : utils.getUserChannels(user)) {
			List<Command> pendingCommands = commandService.getPendingCommands(
					channel.getUUID(), 0L);
			Assert.assertTrue(pendingCommands.size() == 1);
			Command updateGroup = pendingCommands.get(0);
			GroupUpdate updateGroupParam = jsonMapper.convertValue(
					updateGroup.getParams(), GroupUpdate.class);
			Assert.assertTrue(updateGroup.getMethod().equals(Command.METHOD_UPDATE_GROUP));
			Assert.assertTrue(updateGroupParam.getCanLeave());
			Assert.assertTrue(updateGroupParam.isAdmin());
		}
	}

	@Test
	public void testCreateAutomanagedGroupWithAvatar()
 throws IOException {
		testSetup();

		Command createGroup = buildCreateGroupCommand(channels.get(user
				.getUUID()));
		InputStream avatar = utils.getImage();
		Content content = contentService.saveContent(avatar, "image/jpeg");
		utils.login(user);
		commandService.executeCommand(createGroup, content, false);
	}

	@Test(expected = AccessDeniedException.class)
	public void testCreateAutomanagedGroupByAnonymous() throws IOException {
		testSetup();

		Command createGroup = buildCreateGroupCommand(channels.get(root
				.getUUID()));
		InputStream avatar = utils.getImage();
		Content content = contentService.saveContent(avatar, "image/jpeg");
		utils.anonymousLogin();
		commandService.executeCommand(createGroup, content, false);
	}

	private void testSetup() throws IOException {
		String name = "" + rnd.nextInt();
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
		channels.put(root.getUUID(), utils.createChannel(rootExt));

		for (int i = 0; i < 5; i++) {
			channels.put(user.getUUID(), utils.createChannel(user));
		}
	}

	private Command buildCreateGroupCommand(ChannelEntity channel) {

		Long localId = rnd.nextLong();
		String groupName = "GROUPNAME";
		Map<String, Object> params = new HashMap<String, Object>();
		GroupCreate group = new GroupCreate();
		group.setLocalId(localId);
		group.setName(groupName);
		AccountId account = new AccountId();
		account.setId(acc.getUUID());
		params.put(Command.PARAM_GROUP, group);
		params.put(Command.PARAM_ACCOUNT, account);

		Command createGroup = new Command();
		createGroup.setChannelId(channel.getUUID());
		createGroup.setMethod(Command.METHOD_CREATE_GROUP);
		createGroup.setParams(params);
		return createGroup;
	}
}
