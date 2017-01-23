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
import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.agenda.datamodel.pojo.Account;
import com.kurento.agenda.datamodel.pojo.KhcInvalidDataInfo.Code;
import com.kurento.khc.KhcInvalidDataException;
import com.kurento.khc.datamodel.AccountEntity;
import com.kurento.khc.datamodel.GroupEntity;
import com.kurento.khc.datamodel.UserEntity;
import com.kurento.khc.services.v2.AdministrativeService;
import com.kurento.khc.spring.KhcDatamodelConfig;
import com.kurento.khc.test.utils.KhcTestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = KhcDatamodelConfig.class)
@ActiveProfiles({ "embed_db", "embed_security" })
public class AccountTest {

	private Random rnd = new SecureRandom();

	@Autowired
	KhcTestUtils utils;

	@Autowired
	AdministrativeService administrativeService;

	private AccountEntity acc;

	private GroupEntity group;
	private UserEntity user, accAdmin;
	private List<UserEntity> members;

	@Test
	public void testGetInfoFromManagedAccount() {
		AccountEntity acc = utils.createAccount();
		utils.anonymousLogin();
		Account accInfo = administrativeService.getAccountInfo(acc.getUUID());
		Assert.assertTrue(!accInfo.isGroupAutoregister());
		Assert.assertTrue(!accInfo.isUserAutoregister());
	}

	@Test
	public void testGetInfoFromAutoManagedAccount() {
		AccountEntity acc = utils.createAutomanagedAccount();
		utils.anonymousLogin();
		Account accInfo = administrativeService.getAccountInfo(acc.getUUID());
		Assert.assertTrue(accInfo.isGroupAutoregister());
		Assert.assertTrue(accInfo.isUserAutoregister());
	}

	@Test
	public void testDeleteAutomanagedAccount() throws IOException {
		testSetup();
		AccountEntity accext = utils.createAccount();
		UserEntity root = utils.createRootUser(rnd.nextInt() + "-root", accext);

		// Test perform
		utils.login(root);
		administrativeService.deleteAccount(acc.getUUID());
	}

	@Test(expected = AccessDeniedException.class)
	public void testDeleteOwnAccount() throws IOException {
		String name = "" + (new SecureRandom()).nextInt();
		AccountEntity acc = utils.createAutomanagedAccount();
		UserEntity root = utils.createRootUser(name, acc);
		for (int i = 0; i < 50; i++) {
			UserEntity user = utils.createUser(name + i, acc, false);
			GroupEntity group = utils.createGroup(name + i, acc, user, false);
			for (int j = 1; j < 10; j++) {
				utils.sendMessage("test message", user, group);
			}
		}

		// Test perform
		utils.login(root);
		administrativeService.deleteAccount(acc.getUUID());
	}

	@Test
	public void createExistingAccount() throws IOException {
		// test setup
		AccountEntity rootAcc = utils.createAutomanagedAccount();
		UserEntity root = utils.createRootUser("root2", rootAcc);

		String name = "" + (new Random()).nextInt();
		Account account = new Account();
		account.setName(name);

		utils.login(root);
		administrativeService.createAccount(account);

		// Test perform
		try {
			administrativeService.createAccount(account);
		} catch (KhcInvalidDataException e) {
			Assert.assertTrue(e.getCode().equals(Code.ACCOUNT_ALREADY_EXISTS));
		}
	}

	private void testSetup() throws IOException {
		String name = String.valueOf(rnd.nextInt());
		acc = utils.createAutomanagedAccount();
		user = utils.createUser(name, acc, false);
		accAdmin = utils.createUser(name + "admin", acc, false);
		utils.addAccountAdmin(acc, accAdmin);

		// Create Account groups
		group = utils.createGroup(name, acc, user, false);

		members = new ArrayList<UserEntity>();
		for (int i = 0; i < 5; i++) {
			UserEntity member = utils.createUser(name + i, acc, false);
			utils.addGroupMember(group, member);
			members.add(member);
		}

		// Send several messages to create timelines
		for (int i = 0; i < 10; i++) {
			utils.sendMessage("message_from" + user.getEmail() + "_" + i, user,
					group);
		}

	}
}
