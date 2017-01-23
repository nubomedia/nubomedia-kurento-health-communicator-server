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
import java.util.Random;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.khc.datamodel.AccountEntity;
import com.kurento.khc.datamodel.GroupEntity;
import com.kurento.khc.datamodel.UserEntity;
import com.kurento.khc.services.v2.AdministrativeService;
import com.kurento.khc.spring.KhcDatamodelConfig;
import com.kurento.khc.test.utils.KhcTestUtils;
import com.kurento.khc.utils.FileRepository;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = KhcDatamodelConfig.class)
@ActiveProfiles({ "embed_db", "embed_security" })
public class UserReadTest {

	private static final Random rnd = new SecureRandom();

	private AccountEntity acc;
	private AccountEntity accExt;
	private GroupEntity group;
	private UserEntity userEntity;
	private UserEntity userGroupBrother;
	private UserEntity userAccBrother;
	private UserEntity userExt;
	private UserEntity accAdminUser;
	private UserEntity accExtAdminUser;
	private UserEntity rootUser;

	@Autowired
	KhcTestUtils utils;

	@Autowired
	AdministrativeService admService;

	@Autowired
	FileRepository repo;

	@Test
	public void testReadUserByRoot() throws IOException {
		testSetup();
		utils.login(rootUser);
		admService.getUser(userEntity.getUUID());
		admService.getUser(userExt.getUUID());
		admService.getUser(userAccBrother.getUUID());
		admService.getUser(accAdminUser.getUUID());
		admService.getUser(accExtAdminUser.getUUID());
	}

	@Test
	public void testReadUserAvatarByRoot() throws IOException {
		testSetup();
		utils.login(rootUser);
		admService.getUserAvatar(userEntity.getUUID());
		admService.getUserAvatar(userExt.getUUID());
		admService.getUserAvatar(userAccBrother.getUUID());
		admService.getUserAvatar(accAdminUser.getUUID());
		admService.getUserAvatar(accExtAdminUser.getUUID());
	}

	@Test
	public void testReadUserByAdmin() throws IOException {
		testSetup();
		utils.login(accAdminUser);
		admService.getUser(userEntity.getUUID());
		admService.getUser(userAccBrother.getUUID());
		admService.getUser(accAdminUser.getUUID());
	}

	@Test
	public void testReadUserByGroupMember() throws IOException {
		testSetup();
		utils.login(userGroupBrother);
		admService.getUser(userEntity.getUUID());
	}

	@Test(expected = AccessDeniedException.class)
	public void testAccAdminDontReadExternal() throws IOException {
		testSetup();
		utils.login(accAdminUser);
		admService.getUser(userExt.getUUID());
	}

	@Test
	public void testReadUserAvatarByAdmin() throws IOException {
		testSetup();
		utils.login(accAdminUser);
		admService.getUserAvatar(userEntity.getUUID());
		admService.getUserAvatar(userGroupBrother.getUUID());
	}

	@Test
	public void testReadUserAvatarByGroupMember() throws IOException {
		testSetup();
		utils.login(userGroupBrother);
		admService.getUserAvatar(userEntity.getUUID());
	}

	@Test(expected = AccessDeniedException.class)
	public void testAccAdminDontReadExternalAvatar() throws IOException {
		testSetup();
		utils.login(accAdminUser);
		admService.getUserAvatar(userExt.getUUID());
	}

	private void testSetup() throws IOException {
		String name = rnd.nextInt() + "-" + rnd.nextInt();
		acc = utils.createAccount();
		accExt = utils.createAccount();
		userEntity = utils.createUser(name + "-user", acc, true);
		userExt = utils.createUser(name + "-ext", accExt, true);
		accAdminUser = utils.createUser(name + "_admin", acc, true);
		group = utils.createGroup(name, acc, userEntity, false);
		userGroupBrother = utils.createUser(name + "grp_bro", acc, true);
		utils.addGroupMember(group, userGroupBrother);
		userAccBrother = utils.createUser(name + "bro", acc, true);
		utils.addAccountAdmin(acc, accAdminUser);
		accExtAdminUser = utils.createUser(name + "_adminext", accExt, true);
		utils.addAccountAdmin(accExt, accExtAdminUser);
		rootUser = utils.createRootUser(name + "_root", acc);
	}

}
