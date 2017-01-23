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
import java.io.InputStream;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.agenda.datamodel.pojo.Content;
import com.kurento.agenda.datamodel.pojo.KhcInvalidDataInfo.Code;
import com.kurento.agenda.datamodel.pojo.User;
import com.kurento.khc.KhcInvalidDataException;
import com.kurento.khc.datamodel.AccountEntity;
import com.kurento.khc.services.v2.AdministrativeService;
import com.kurento.khc.services.v2.ContentService;
import com.kurento.khc.spring.KhcDatamodelConfig;
import com.kurento.khc.test.utils.KhcTestUtils;
import com.kurento.khc.utils.FileRepository;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = KhcDatamodelConfig.class)
@ActiveProfiles({ "embed_db", "embed_security" })
public class UserAutoregisterTest {

	private Random rnd = new Random();

	@Autowired
	private KhcTestUtils utils;

	@Autowired
	private AdministrativeService admService;

	@Autowired
	private FileRepository repo;

	@Autowired
	private ContentService contentService;

	@Test
	public void testRegisterUser() throws FileNotFoundException {
		AccountEntity acc = utils.createAutomanagedAccount();
		User user = buildUser();

		utils.anonymousLogin();
		admService.createUserInAutoregister(user, acc.getUUID());
	}

	@Test
	public void testRegisterUserWithAvatar() throws IOException {
		AccountEntity acc = utils.createAutomanagedAccount();
		User user = buildUser();

		InputStream image = utils.getImage();
		Content picture = contentService.saveContent(image, "image/jpeg");

		utils.anonymousLogin();
		user = admService
				.createUserInAutoregister(user, acc.getUUID(), picture);
		utils.login(user);
		assertIsImage(user);

	}

	@Test(expected = AccessDeniedException.class)
	public void testRegisterWithManagedAccount() {
		AccountEntity acc = utils.createAccount();
		User user = buildUser();

		utils.anonymousLogin();
		admService.createUserInAutoregister(user, acc.getUUID());

	}

	@Test
	public void testCreateExistingUserEmail() {
		try {
			AccountEntity acc = utils.createAutomanagedAccount();
			User user = buildUser();

			utils.anonymousLogin();
			admService.createUserInAutoregister(user, acc.getUUID());
			admService.createUserInAutoregister(user, acc.getUUID());
		} catch (KhcInvalidDataException e) {
			Assert.assertTrue(e.getCode()
.equals(Code.EMAIL_ALREADY_USED));
			return;
		}
		Assert.assertFalse(true);
	}

	@Test
	public void testCreateExistingUserPhone() {
		try {
			AccountEntity acc = utils.createAutomanagedAccount();
			User user = buildUser();

			utils.anonymousLogin();
			admService.createUserInAutoregister(user, acc.getUUID());
			user.setEmail("nomail@kurento.org");
			admService.createUserInAutoregister(user, acc.getUUID());
		} catch (KhcInvalidDataException e) {
			Assert.assertTrue(e.getCode()
.equals(Code.PHONE_ALREADY_USED));
			return;
		}
		Assert.assertFalse(true);
	}

	@Test
	public void testCreateUserWithNoCredentials() {
		try {
			AccountEntity acc = utils.createAutomanagedAccount();
			User user = buildUser();

			utils.anonymousLogin();
			admService.createUserInAutoregister(user, acc.getUUID());
			user.setEmail(null);
			user.setPhone(null);
			admService.createUserInAutoregister(user, acc.getUUID());
		} catch (KhcInvalidDataException e) {
			Assert.assertTrue(e.getCode().equals(Code.NO_CREDENTIALS));
			return;
		}
		Assert.assertFalse(true);
	}

	@Test
	public void testCreateUserWithNoPassword() {
		try {
			AccountEntity acc = utils.createAutomanagedAccount();
			User user = buildUser();
			user.setPassword(null);
			utils.anonymousLogin();
			admService.createUserInAutoregister(user, acc.getUUID());
		} catch (KhcInvalidDataException e) {
			Assert.assertTrue(e.getCode().equals(Code.NO_CREDENTIALS));
			return;
		}
		Assert.assertFalse(true);
	}

	private User buildUser() {
		String name = String.valueOf(rnd.nextInt(Integer.MAX_VALUE));
		User user = new User();
		user.setEmail(name + "@kurentotest.com");
		user.setPhone("91488" + String.format("%04d", rnd.nextInt(9999)));
		user.setPhoneRegion("ES");
		user.setPassword(name);
		user.setName(name);
		return user;
	}

	private void assertIsImage(User user) throws FileNotFoundException {
		// Check images
		Content content = admService.getUserAvatar(user.getId());
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
