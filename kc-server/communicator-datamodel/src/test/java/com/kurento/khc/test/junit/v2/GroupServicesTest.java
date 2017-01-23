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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.agenda.datamodel.pojo.Content;
import com.kurento.agenda.datamodel.pojo.Group;
import com.kurento.agenda.datamodel.pojo.User;
import com.kurento.khc.datamodel.AccountEntity;
import com.kurento.khc.datamodel.GroupEntity;
import com.kurento.khc.datamodel.GroupSecureDao;
import com.kurento.khc.datamodel.UserEntity;
import com.kurento.khc.services.v2.AdministrativeService;
import com.kurento.khc.spring.KhcDatamodelConfig;
import com.kurento.khc.test.utils.KhcTestUtils;
import com.kurento.khc.utils.FileRepository;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = KhcDatamodelConfig.class)
@ActiveProfiles({ "embed_db", "embed_security" })
public class GroupServicesTest {

	private Random rnd = new SecureRandom();

	private AccountEntity acc, accExt;
	private UserEntity accAdm, root, rootExt;
	private GroupEntity groupEntity;
	private Group group;
	private Map<Long, UserEntity> admins;
	private Map<Long, UserEntity> members;

	@Autowired
	private KhcTestUtils utils;

	@Autowired
	private AdministrativeService administrativeService;
	@Autowired
	private GroupSecureDao groupSecureDao;

	@Autowired
	private FileRepository repo;

	@Test
	public void testCreateGroupByAccountAdmin() throws IOException {

		prepareManagedGroupTest();
		utils.addAccountAdmin(acc, accAdm);
		utils.login(accAdm);
		administrativeService.createAccountGroup(group, acc.getUUID());

	}

	@Test
	public void testCreateAccountGroupByRootFromOtherAccount()
			throws IOException {
		prepareUnmanagedGroupTest();
		utils.login(rootExt);
		administrativeService.createAccountGroup(group, acc.getUUID());
	}

	@Test
	public void testCreateAccountGroupByAdmin() throws IOException {
		prepareUnmanagedGroupTest();
		utils.login(accAdm);
		administrativeService.createAccountGroup(group, acc.getUUID());
	}

	@Test
	public void testReadGroupAdminsByRoot() throws IOException {
		prepareReadAdmin();

		// Test perform
		utils.login(root);
		for (User user : administrativeService.getGroupAdmins(groupEntity
				.getUUID())) {
			admins.remove(user.getId());
		}
		Assert.assertTrue(admins.size() == 0);
		for (User user : administrativeService
				.getGroupMembersNotAdmins(groupEntity.getUUID())) {
			members.remove(user.getId());
		}
		Assert.assertTrue(members.size() == 0);
	}

	@Test
	public void testReadGroupAdminsByAdmin() throws IOException {
		prepareReadAdmin();

		// Test perform
		utils.login(accAdm);
		for (User user : administrativeService.getGroupAdmins(groupEntity
				.getUUID())) {
			admins.remove(user.getId());
		}
		Assert.assertTrue(admins.size() == 0);
		for (User user : administrativeService
				.getGroupMembersNotAdmins(groupEntity.getUUID())) {
			members.remove(user.getId());
		}
		Assert.assertTrue(members.size() == 0);
	}

	// /////////////////////////////////////////////////

	private void prepareManagedGroupTest() throws IOException {
		String name = String.valueOf(rnd.nextInt());
		acc = utils.createAccount();
		accAdm = utils.createUser(name, acc, false);

		group = new Group();
		group.setName(name);
	}

	private void prepareUnmanagedGroupTest() throws IOException {
		String name = String.valueOf(rnd.nextInt());
		acc = utils.createAutomanagedAccount();
		accExt = utils.createAutomanagedAccount();
		accAdm = utils.createUser(name, acc, false);
		utils.addAccountAdmin(acc, accAdm);
		root = utils.createRootUser(name + "root", acc);
		rootExt = utils.createRootUser(name + "rootExt", accExt);

		group = new Group();
		group.setName(name);

	}

	private void prepareReadAdmin() throws IOException {
		// Test setup
		String name = String.valueOf(rnd.nextInt());
		acc = utils.createAutomanagedAccount();
		root = utils.createRootUser(name + "root", acc);
		accAdm = utils.createUser(name + "adminacc", acc, false);
		utils.addAccountAdmin(acc, accAdm);
		groupEntity = utils.createGroup(name, acc);
		admins = new HashMap<Long, UserEntity>();
		for (int i = 0; i < 5; i++) {
			UserEntity admin = utils.createUser(name + "admin" + i, acc, false);
			utils.addGroupAdmin(groupEntity, admin);
		}
		members = new HashMap<Long, UserEntity>();
		for (int i = 0; i < 5; i++) {
			UserEntity member = utils.createUser(name + "member" + i, acc,
					false);
			utils.addGroupMember(groupEntity, member);
			members.put(member.getUUID(), member);
		}
	}

	void assertIsImage() throws FileNotFoundException {
		// Get images
		Content content = administrativeService.getGroupAvatar(group.getId());
		String contentUrl = content.getContentUrl();
		repo.getMediaFile(contentUrl);
		repo.getMediaSize(contentUrl);
		repo.getMediaUrl(contentUrl);

		String thLargeUrl = content.getLargeThumbnailUrl();
		repo.getMediaFile(thLargeUrl);
		repo.getMediaSize(thLargeUrl);
		repo.getMediaUrl(thLargeUrl);

		String thMediumUrl = content.getMediumThumbnailUrl();
		repo.getMediaFile(thMediumUrl);
		repo.getMediaSize(thMediumUrl);
		repo.getMediaUrl(thMediumUrl);

		String thSmallUrl = content.getSmallThumbnailUrl();
		repo.getMediaFile(thSmallUrl);
		repo.getMediaSize(thSmallUrl);
		repo.getMediaUrl(thSmallUrl);

	}
}
