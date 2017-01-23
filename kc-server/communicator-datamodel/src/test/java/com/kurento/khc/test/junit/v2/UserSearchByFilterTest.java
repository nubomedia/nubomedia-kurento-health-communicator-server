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
import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.agenda.datamodel.pojo.User;
import com.kurento.khc.datamodel.AccountEntity;
import com.kurento.khc.datamodel.UserEntity;
import com.kurento.khc.services.v2.AdministrativeService;
import com.kurento.khc.spring.KhcDatamodelConfig;
import com.kurento.khc.test.utils.KhcTestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = KhcDatamodelConfig.class)
@ActiveProfiles({ "embed_db", "embed_security" })
public class UserSearchByFilterTest {

	private Random rnd = new Random();

	private AccountEntity acc;
	private UserEntity admin;
	private List<UserEntity> members = new ArrayList<UserEntity>();

	@Autowired
	private KhcTestUtils utils;

	@Autowired
	private AdministrativeService administrativeService;

	@Test
	public void testUserSearchByFilterTest() throws IOException {

		String name = String.valueOf(rnd.nextInt());
		acc = utils.createAutomanagedAccount();
		admin = utils.createRootUser(name, acc);
		int MAX_MEMBER = 5;
		int MAX_FILTER = 3;
		String PATTERN = "pattern";
		String aux = "";
		for (int i = 0; i < MAX_MEMBER; i++) {
			if (i % 2 == 0)
				aux = PATTERN;
			else
				aux = "";
			members.add(i, utils.createUser(name + aux + i, acc, false));
		}

		utils.login(admin);

		List<User> searchResult = administrativeService
				.searchAccountUsersByFilter(acc.getUUID(), PATTERN, 0, 100);
		Assert.assertTrue(searchResult.size() == MAX_FILTER);
	}
}
