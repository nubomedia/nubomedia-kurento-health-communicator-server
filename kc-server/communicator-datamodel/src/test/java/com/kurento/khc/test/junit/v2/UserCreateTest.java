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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.agenda.datamodel.pojo.KhcInvalidDataInfo.Code;
import com.kurento.agenda.datamodel.pojo.User;
import com.kurento.khc.KhcInvalidDataException;
import com.kurento.khc.datamodel.AccountEntity;
import com.kurento.khc.datamodel.UserEntity;
import com.kurento.khc.services.v2.AdministrativeService;
import com.kurento.khc.spring.KhcDatamodelConfig;
import com.kurento.khc.test.utils.KhcTestUtils;
import com.kurento.khc.utils.FileRepository;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = KhcDatamodelConfig.class)
@ActiveProfiles({ "embed_db", "embed_security" })
public class UserCreateTest {

	private Random rnd = new SecureRandom();

	@Autowired
	private KhcTestUtils utils;

	@Autowired
	private AdministrativeService admService;

	@Autowired
	private FileRepository repo;

	private AccountEntity acc;
	private UserEntity accAdminUser;
	private UserEntity rootUser;

	@Test
	public void testCreateUserByRoot() throws IOException {

		testSetup();

		User user = buildUser();

		utils.login(rootUser);
		admService.createUserInAccount(user, acc.getUUID());
	}

	@Test
	public void testCreateUserByRootOnlyRegion() throws IOException {

		testSetup();

		User user = buildUser();

		utils.login(rootUser);
		admService.createUserInAccount(user, acc.getUUID());
	}

	@Test
	public void testCreateUserByAdmin() throws IOException {
		testSetup();

		User user = buildUser();

		utils.login(accAdminUser);
		admService.createUserInAccount(user, acc.getUUID());
	}

	@Test
	public void testCreateExistingAccountUser() throws IOException {
		testSetup();

		User user = buildUser();

		utils.login(accAdminUser);
		admService.createUserInAccount(user, acc.getUUID());
		try {
			admService.createUserInAccount(user, acc.getUUID());
		} catch (KhcInvalidDataException e) {
			Assert.assertTrue(e.getCode().equals(Code.EMAIL_ALREADY_USED));
			return;
		}
		Assert.assertFalse(true);
	}

	private void testSetup() throws IOException {
		String name = "" + rnd.nextInt();
		acc = utils.createAccount();
		accAdminUser = utils.createUser(name + "_admin", acc, false);
		utils.addAccountAdmin(acc, accAdminUser);
		rootUser = utils.createRootUser(name + "_root", acc);
	}

	private User buildUser() {
		User user = new User();
		user.setEmail(rnd.nextInt() + "dd@dd");
		user.setPassword("pwd");
		user.setPhone("91488" + String.format("%04d", rnd.nextInt(9999)));
		user.setPhoneRegion("ES");
		user.setName(rnd.nextInt() + "");
		return user;
	}
}
