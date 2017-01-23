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
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.agenda.datamodel.pojo.Group;
import com.kurento.khc.datamodel.AccountEntity;
import com.kurento.khc.datamodel.UserEntity;
import com.kurento.khc.services.v2.AdministrativeService;
import com.kurento.khc.spring.KhcDatamodelConfig;
import com.kurento.khc.test.utils.KhcTestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = KhcDatamodelConfig.class)
@ActiveProfiles({ "embed_db", "embed_security" })
public class UserGroupTest {

	private static final Random rnd = new Random();

	private AccountEntity acc;
	private UserEntity root, admin, userA, userB;

	@Autowired
	KhcTestUtils utils;

	@Autowired
	AdministrativeService admService;

	@Test
	public void testGetGroupsWhereUserIsMemberByRoot() throws IOException {
		testSetup();
		utils.login(root);
		for (Group grp : admService.getGroupsWhereUserIsMember(userA.getUUID())) {
			Assert.assertTrue(grp.getName().endsWith("A"));
		}
	}

	@Test
	public void testGetGroupsWhereUserIsMemberByAdmin() throws IOException {
		testSetup();
		utils.login(admin);
		for (Group grp : admService.getGroupsWhereUserIsMember(userA.getUUID())) {
			Assert.assertTrue(grp.getName().endsWith("A"));
		}
	}

	private void testSetup() throws IOException {
		String name = "" + rnd.nextInt();
		acc = utils.createAccount();
		admin = utils.createUser(name + "admin", acc, false);
		root = utils.createRootUser(name + "root", acc);
		userA = utils.createUser(name + "A", acc, false);
		userB = utils.createUser(name + "B", acc, false);
		for (int i = 0; i < 10; i++) {
			utils.createGroup(name + i + "A", acc, userA, false);
			utils.createGroup(name + i + "B", acc, userB, false);
		}
	}

}
