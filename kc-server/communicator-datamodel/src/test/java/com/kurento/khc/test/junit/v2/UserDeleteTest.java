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
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.services.pojo.AccountId;
import com.kurento.agenda.services.pojo.GroupCreate;
import com.kurento.khc.KhcNotFoundException;
import com.kurento.khc.datamodel.AccountEntity;
import com.kurento.khc.datamodel.ChannelEntity;
import com.kurento.khc.datamodel.GroupEntity;
import com.kurento.khc.datamodel.UserEntity;
import com.kurento.khc.services.v2.AdministrativeService;
import com.kurento.khc.services.v2.CommandService;
import com.kurento.khc.spring.KhcDatamodelConfig;
import com.kurento.khc.test.utils.KhcTestUtils;
import com.kurento.khc.utils.FileRepository;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = KhcDatamodelConfig.class)
@ActiveProfiles({ "embed_db", "embed_security" })
public class UserDeleteTest {

	private static final SecureRandom rnd = new SecureRandom();

	private AccountEntity acc;
	private UserEntity userEntity;
	private UserEntity accAdminUser;
	private UserEntity rootUser;

	@Autowired
	KhcTestUtils utils;

	@Autowired
	AdministrativeService admService;

	@Autowired
	CommandService cmdService;

	@Autowired
	FileRepository repo;

	@Test(expected = AccessDeniedException.class)
	public void testDeleteUser() throws IOException {
		testSetup();

		utils.login(userEntity);
		admService.deleteUser(userEntity.getUUID());
	}

	@Test(expected = KhcNotFoundException.class)
	public void testUserDeleteByRoot() throws IOException {
		testSetup();

		utils.login(rootUser);
		admService.deleteUser(userEntity.getUUID());
		userEntity = utils.findUserEntity(userEntity.getId());
	}

	@Test(expected = KhcNotFoundException.class)
	public void testUserDeleteByAccountAdmin() throws IOException {
		testSetup();

		utils.login(accAdminUser);
		admService.deleteUser(userEntity.getUUID());
		userEntity = utils.findUserEntity(userEntity.getId());
	}

	private void testSetup() throws IOException {
		String name = String.valueOf(rnd.nextInt());
		acc = utils.createAutomanagedAccount();
		accAdminUser = utils.createUser(name + "_admin", acc, false);
		utils.addAccountAdmin(acc, accAdminUser);
		rootUser = utils.createRootUser(name + "_root", acc);

		// Create user to be deleted
		userEntity = utils.createUser(name, acc, true);
		// Create several user channels and send commands
		for (int i = 0; i < 5; i++) {
			ChannelEntity channel = utils.createChannel(userEntity);
			Command command = new Command();
			command.setMethod(Command.METHOD_CREATE_GROUP);
			command.setChannelId(channel.getUUID());
			AccountId accParam = new AccountId();
			accParam.setId(acc.getUUID());
			GroupCreate groupParam = new GroupCreate();
			groupParam.setLocalId(rnd.nextLong());
			groupParam.setName(name + i);

			Map<String, Object> params = new HashMap<String, Object>();
			params.put(Command.PARAM_ACCOUNT, accParam);
			params.put(Command.PARAM_GROUP, groupParam);
			command.setParams(params);
			utils.login(userEntity);
			cmdService.executeCommand(command, null, false);
		}

		for (int i = 0; i < 2; i++) {
			GroupEntity group = utils.createGroup(name + i, acc);
			utils.addGroupMember(group, userEntity);
			utils.addGroupAdmin(group, userEntity);
			for (int j = 0; j < 2; j++) {
				utils.sendMessage("test", userEntity, group, true);
			}
		}
		// for (int i = 0; i < 2; i++) {
		// OrganizationEntity org = utils
		// .createRootOrganization(name + i, acc);
		// utils.addOrganizationMember(org, userEntity);
		// utils.addOrganizationAdmin(org, userEntity);
		// for (int j = 0; j < 2; j++) {
		// GroupEntity group = utils.createGroup(name + i, org);
		// utils.addGroupMember(group, userEntity);
		// utils.addGroupAdmin(group, userEntity);
		// for (int k = 0; k < 2; k++) {
		// utils.sendMessage("test", userEntity, group, true);
		// }
		// }
		// }
	}
}
