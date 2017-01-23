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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.datamodel.pojo.Content;
import com.kurento.agenda.datamodel.pojo.KhcInvalidDataInfo.Code;
import com.kurento.agenda.datamodel.pojo.User;
import com.kurento.agenda.services.pojo.UserEdit;
import com.kurento.agenda.services.pojo.UserReadResponse;
import com.kurento.khc.KhcInvalidDataException;
import com.kurento.khc.datamodel.AccountEntity;
import com.kurento.khc.datamodel.ChannelEntity;
import com.kurento.khc.datamodel.GroupEntity;
import com.kurento.khc.datamodel.UserEntity;
import com.kurento.khc.jackson.KhcObjectMapper;
import com.kurento.khc.services.v2.AdministrativeService;
import com.kurento.khc.services.v2.CommandService;
import com.kurento.khc.services.v2.ContentService;
import com.kurento.khc.spring.KhcDatamodelConfig;
import com.kurento.khc.test.utils.KhcTestUtils;
import com.kurento.khc.utils.FileRepository;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = KhcDatamodelConfig.class)
@ActiveProfiles({ "embed_db", "embed_security" })
public class UpdateUserCommandTest {

	private Random rnd = new Random();

	private AccountEntity acc, accExt;
	private UserEntity user1;
	private ChannelEntity channel11;
	private ChannelEntity channel12;
	private UserEntity user2;
	private ChannelEntity channel2;
	private UserEntity accAdmin, root, rootExt;
	private Map<Long, ChannelEntity> channels;
	private GroupEntity group;

	String name, surname, password, email, phone;

	private ObjectMapper jsonMapper = new KhcObjectMapper();

	@Autowired
	private KhcTestUtils utils;

	@Autowired
	private CommandService commandService;

	@Autowired
	private AdministrativeService administrativeService;

	@Autowired
	private ContentService contentService;

	@Autowired
	FileRepository repo;

	@Test
	public void testUpdateUserCommand() throws IOException {
		testSetup();

		// send command to update user data
		Command updateUser = getUserUpdateCommandChangePassword(channel11);

		utils.login(user1);
		commandService.executeCommand(updateUser, null, false);
		User updatedUser = administrativeService.getUser(user1.getUUID());
		Assert.assertTrue(updatedUser.getName().equals(name));
		Assert.assertTrue(updatedUser.getSurname().equals(surname));
		Assert.assertTrue(updatedUser.getEmail().equals(user1.getEmail()));
		// Verify password
		Assert.assertTrue(utils.findUserEntity(user1.getId()).getPassword()
				.equals(password));
	}

	@Test
	public void testUpdateUserPhone() throws IOException {
		testSetup();

		// send command to update user data
		Command updateUser = getUserUpdateCommandChangePhone(channel11);

		utils.login(user1);
		commandService.executeCommand(updateUser, null, false);
		User updatedUser = administrativeService.getUser(user1.getUUID());
		Assert.assertTrue(updatedUser.getName().equals(user1.getName()));
		Assert.assertTrue(updatedUser.getSurname().equals(user1.getSurname()));
		Assert.assertTrue(updatedUser.getEmail().equals(user1.getEmail()));
		Assert.assertTrue(updatedUser.getPhone().endsWith(phone));
	}

	@Test
	public void testUpdateUserEmail() throws IOException {
		testSetup();

		// send command to update user data
		Command updateUser = getUserUpdateCommandChangeEmail(channel11);

		utils.login(user1);
		commandService.executeCommand(updateUser, null, false);
		User updatedUser = administrativeService.getUser(user1.getUUID());
		Assert.assertTrue(updatedUser.getName().equals(user1.getName()));
		Assert.assertTrue(updatedUser.getSurname().equals(user1.getSurname()));
		Assert.assertTrue(updatedUser.getEmail().equals(email));
	}

	@Test
	public void testUpdateUserByAccountAdmin() throws IOException {
		testSetup();

		// send command to update user data
		Command updateUser = getUserUpdateCommandChangePassword(channels
				.get(accAdmin.getUUID()));

		utils.login(accAdmin);
		commandService.executeCommand(updateUser, null, false);
		User updatedUser = administrativeService.getUser(user1.getUUID());
		Assert.assertTrue(updatedUser.getName().equals(name));
		Assert.assertTrue(updatedUser.getSurname().equals(surname));
		Assert.assertTrue(updatedUser.getEmail().equals(user1.getEmail()));
	}

	@Test
	public void testUpdateUserByRoot() throws IOException {
		testSetup();

		// send command to update user data
		Command updateUser = getUserUpdateCommandChangePassword(channels
				.get(root.getUUID()));

		utils.login(root);
		commandService.executeCommand(updateUser, null, false);
		User updatedUser = administrativeService.getUser(user1.getUUID());
		Assert.assertTrue(updatedUser.getName().equals(name));
		Assert.assertTrue(updatedUser.getSurname().equals(surname));
		Assert.assertTrue(updatedUser.getEmail().equals(user1.getEmail()));
	}

	@Test
	public void testUpdateUserByExternalRoot() throws IOException {
		testSetup();

		// send command to update user data
		Command updateUser = getUserUpdateCommandChangePassword(channels
				.get(rootExt.getUUID()));

		utils.login(rootExt);
		commandService.executeCommand(updateUser, null, false);
		User updatedUser = administrativeService.getUser(user1.getUUID());
		Assert.assertTrue(updatedUser.getName().equals(name));
		Assert.assertTrue(updatedUser.getSurname().equals(surname));
		Assert.assertTrue(updatedUser.getEmail().equals(user1.getEmail()));
	}

	@Test
	public void testUpdateUserCommandWithAvatarChange() throws IOException {
		testSetup();

		Command updateUser = getUserUpdateCommandChangePassword(channel11);

		InputStream picture = utils.getImage();
		Content avatar = contentService.saveContent(picture, "image/jpeg");

		utils.login(user1);
		// replace avatar several times
		commandService.executeCommand(updateUser, avatar, false);
		picture = utils.getImage();
		avatar = contentService.saveContent(picture, "image/jpeg");
		commandService.executeCommand(updateUser, avatar, false);
		picture = utils.getImage();
		avatar = contentService.saveContent(picture, "image/jpeg");
		commandService.executeCommand(updateUser, avatar, false);
		User updatedUser = administrativeService.getUser(user1.getUUID());

		Assert.assertTrue(updatedUser.getName().equals(name));
		Assert.assertTrue(updatedUser.getSurname().equals(surname));
		Assert.assertTrue(updatedUser.getEmail().equals(user1.getEmail()));
		assertIsImage(updatedUser);
	}

	@Ignore
	// Do not exists private information
	@Test
	public void testUpdateUserCommandPropagatePrivateToOtherUserChannels()
			throws JsonParseException, JsonMappingException, IOException {
		testSetup();

		String phone = "NEWPHONE";
		UserEdit params = new UserEdit();
		params.setId(user1.getUUID());
		params.setPhone(phone);

		Command updateUser = new Command();
		updateUser.setChannelId(channel11.getUUID());
		updateUser.setMethod(Command.METHOD_UPDATE_USER);
		updateUser.setParams(jsonMapper.convertValue(params, ObjectNode.class));

		utils.login(user1);
		commandService.executeCommand(updateUser, null, false);

		// channel11 must receive an update
		List<Command> pendingCommands = commandService.getPendingCommands(
				channel11.getUUID(), 0L);
		Assert.assertTrue(pendingCommands.size() == 1);
		updateUser = pendingCommands.get(0);
		Assert.assertTrue(Command.METHOD_UPDATE_USER.equals(updateUser
				.getMethod()));
		UserReadResponse userEditParams = jsonMapper.convertValue(
				updateUser.getParams(), UserReadResponse.class);
		Assert.assertTrue(userEditParams.getPhone().equals(phone));

		// channel12 must receive an update
		pendingCommands = commandService.getPendingCommands(
				channel12.getUUID(), 0L);
		Assert.assertTrue(pendingCommands.size() == 1);
		updateUser = pendingCommands.get(0);
		Assert.assertTrue(Command.METHOD_UPDATE_USER.equals(updateUser
				.getMethod()));
		userEditParams = jsonMapper.convertValue(updateUser.getParams(),
				UserReadResponse.class);
		Assert.assertTrue(userEditParams.getPhone().equals(phone));

		// channel2 must not receive updates
		utils.login(user2);
		pendingCommands = commandService.getPendingCommands(channel2.getUUID(),
				0L);
		Assert.assertTrue(pendingCommands.size() == 0);

	}

	@Test
	public void testUpdateUserCommandDoNotPropagatePasswordToOtherUserChannels()
			throws IOException {
		testSetup();

		// send command to update user data
		String password = "NEWPASSWORD";

		UserEdit params = new UserEdit();
		params.setId(user1.getUUID());
		params.setPassword(password);

		Command updateUser = new Command();
		updateUser.setChannelId(channel11.getUUID());
		updateUser.setMethod(Command.METHOD_UPDATE_USER);
		updateUser.setParams(jsonMapper.convertValue(params, ObjectNode.class));

		utils.login(user1);
		commandService.executeCommand(updateUser, null, false);

		List<Command> pendingCommands = commandService.getPendingCommands(
				channel11.getUUID(), 0L);
		Assert.assertTrue(pendingCommands.size() == 0);

		pendingCommands = commandService.getPendingCommands(
				channel12.getUUID(), 0L);
		Assert.assertTrue(pendingCommands.size() == 0);

		utils.login(user2);
		pendingCommands = commandService.getPendingCommands(channel2.getUUID(),
				0L);
		Assert.assertTrue(pendingCommands.size() == 0);

	}

	@Test
	public void testUpdateUserCommandPropagatePublicToOtherUserChannelsAndGroupMembers()
			throws JsonParseException, JsonMappingException, IOException {
		// send command to update user data

		testSetup();

		String name = user1.getName() + "_NEW_NAME";

		UserEdit params = new UserEdit();
		params.setId(user1.getUUID());
		params.setName(name);

		Command updateUser = new Command();
		updateUser.setChannelId(channel11.getUUID());
		updateUser.setMethod(Command.METHOD_UPDATE_USER);
		updateUser.setParams(jsonMapper.convertValue(params, ObjectNode.class));

		utils.login(user1);
		commandService.executeCommand(updateUser, null, false);

		// channel11 must receive an update
		List<Command> pendingCommands = commandService.getPendingCommands(
				channel11.getUUID(), 0L);
		Assert.assertTrue(pendingCommands.size() == 1);
		updateUser = pendingCommands.get(0);
		Assert.assertTrue(Command.METHOD_UPDATE_USER.equals(updateUser
				.getMethod()));
		UserReadResponse userEditParams = jsonMapper.convertValue(
				updateUser.getParams(), UserReadResponse.class);
		Assert.assertTrue(userEditParams.getName().equals(name));

		// channel12 must receive an update
		pendingCommands = commandService.getPendingCommands(
				channel12.getUUID(), 0L);
		Assert.assertTrue(pendingCommands.size() == 1);
		updateUser = pendingCommands.get(0);
		Assert.assertTrue(Command.METHOD_UPDATE_USER.equals(updateUser
				.getMethod()));
		userEditParams = jsonMapper.convertValue(updateUser.getParams(),
				UserReadResponse.class);
		Assert.assertTrue(userEditParams.getName().equals(name));

		// channel2 must receive an update
		utils.login(user2);
		pendingCommands = commandService.getPendingCommands(channel2.getUUID(),
				0L);
		Assert.assertTrue(pendingCommands.size() == 1);
		updateUser = pendingCommands.get(0);
		Assert.assertTrue(Command.METHOD_UPDATE_CONTACT.equals(updateUser
				.getMethod()));
		userEditParams = jsonMapper.convertValue(updateUser.getParams(),
				UserReadResponse.class);
		Assert.assertTrue(userEditParams.getName().equals(name));

	}

	@Test
	public void testUpdateUserCommandPropagateAvatarChangeToOtherUserChannelsAndGroupMembers()
			throws IOException {

		testSetup();

		UserEdit params = new UserEdit();
		params.setId(user1.getUUID());

		Command updateUser = new Command();
		updateUser.setChannelId(channel11.getUUID());
		updateUser.setMethod(Command.METHOD_UPDATE_USER);
		updateUser.setParams(jsonMapper.convertValue(params, ObjectNode.class));

		InputStream picture = utils.getImage();
		Content avatar = contentService.saveContent(picture, "image/jpeg");

		utils.login(user1);
		commandService.executeCommand(updateUser, avatar, false);

		// channel11 must receive an update
		List<Command> pendingCommands = commandService.getPendingCommands(
				channel11.getUUID(), 0L);
		Assert.assertTrue(pendingCommands.size() == 1);
		updateUser = pendingCommands.get(0);
		Assert.assertTrue(Command.METHOD_UPDATE_USER.equals(updateUser
				.getMethod()));
		UserReadResponse userEditParams = jsonMapper.convertValue(
				updateUser.getParams(), UserReadResponse.class);
		Assert.assertTrue(userEditParams.getPicture() != null);

		// channel12 must receive an update
		pendingCommands = commandService.getPendingCommands(
				channel12.getUUID(), 0L);
		Assert.assertTrue(pendingCommands.size() == 1);
		updateUser = pendingCommands.get(0);
		Assert.assertTrue(Command.METHOD_UPDATE_USER.equals(updateUser
				.getMethod()));
		userEditParams = jsonMapper.convertValue(updateUser.getParams(),
				UserReadResponse.class);
		Assert.assertTrue(userEditParams.getPicture() != null);

		// channel2 must receive an update
		utils.login(user2);
		pendingCommands = commandService.getPendingCommands(channel2.getUUID(),
				0L);
		Assert.assertTrue(pendingCommands.size() == 1);
		updateUser = pendingCommands.get(0);
		Assert.assertTrue(Command.METHOD_UPDATE_CONTACT.equals(updateUser
				.getMethod()));
		userEditParams = jsonMapper.convertValue(updateUser.getParams(),
				UserReadResponse.class);
		Assert.assertTrue(userEditParams.getPicture() != null);
	}

	@Test
	public void testUpdateUserPhoneWithExistingPhone() throws IOException {
		testSetup();

		UserEdit params = new UserEdit();
		params.setId(user1.getUUID());
		params.setPhone(user2.getPhone());

		Command updateUser = new Command();
		updateUser.setChannelId(channel11.getUUID());
		updateUser.setMethod(Command.METHOD_UPDATE_USER);
		updateUser.setParams(jsonMapper.convertValue(params, ObjectNode.class));

		utils.login(user1);
		try {
			commandService.executeCommand(updateUser, null, false);
		} catch (KhcInvalidDataException e) {
			Assert.assertTrue(Code.PHONE_ALREADY_USED.equals(e.getCode()));
			return;
		}
		Assert.assertTrue(false);
	}

	@Test
	public void testUpdateUserPhoneWithItsOwnPhone() throws IOException {
		testSetup();

		UserEdit params = new UserEdit();
		params.setId(user1.getUUID());
		params.setPhone(user1.getPhone());

		Command updateUser = new Command();
		updateUser.setChannelId(channel11.getUUID());
		updateUser.setMethod(Command.METHOD_UPDATE_USER);
		updateUser.setParams(jsonMapper.convertValue(params, ObjectNode.class));

		utils.login(user1);
		commandService.executeCommand(updateUser, null, false);
	}

	@Test
	public void testUpdateUserEmailWithExistingEmail() throws IOException {
		testSetup();

		UserEdit params = new UserEdit();
		params.setId(user1.getUUID());
		params.setEmail(user2.getEmail());

		Command updateUser = new Command();
		updateUser.setChannelId(channel11.getUUID());
		updateUser.setMethod(Command.METHOD_UPDATE_USER);
		updateUser.setParams(jsonMapper.convertValue(params, ObjectNode.class));

		utils.login(user1);
		try {
			commandService.executeCommand(updateUser, null, false);
		} catch (KhcInvalidDataException e) {
			Assert.assertTrue(Code.EMAIL_ALREADY_USED.equals(e.getCode()));
			return;
		}
		Assert.assertTrue(false);
	}

	@Test
	public void testUpdateUserEmailWithItsOwnEmail() throws IOException {
		testSetup();

		UserEdit params = new UserEdit();
		params.setId(user1.getUUID());
		params.setEmail(user1.getEmail());

		Command updateUser = new Command();
		updateUser.setChannelId(channel11.getUUID());
		updateUser.setMethod(Command.METHOD_UPDATE_USER);
		updateUser.setParams(jsonMapper.convertValue(params, ObjectNode.class));

		utils.login(user1);
		commandService.executeCommand(updateUser, null, false);
	}

	private void testSetup() throws IOException {
		String name = String.valueOf(rnd.nextInt());
		acc = utils.createAutomanagedAccount();
		accExt = utils.createAutomanagedAccount();
		user1 = utils.createUser(name, acc, false);
		channel11 = utils.createChannel(user1);
		channel12 = utils.createChannel(user1);
		user2 = utils.createUser(name + "2", acc, false);
		channel2 = utils.createChannel(user2);
		group = utils.createGroup(name, acc, user1, false);
		utils.addGroupMember(group, user2);

		accAdmin = utils.createUser(name + "accAdmin", acc, false);
		root = utils.createRootUser(name + "root", acc);
		rootExt = utils.createRootUser(name + "extRoot", accExt);
		utils.addAccountAdmin(acc, accAdmin);
		channels = new HashMap<Long, ChannelEntity>();
		channels.put(accAdmin.getUUID(), utils.createChannel(accAdmin));
		channels.put(root.getUUID(), utils.createChannel(root));
		channels.put(rootExt.getUUID(), utils.createChannel(rootExt));

	}

	private Command getUserUpdateCommandChangePhone(ChannelEntity channel) {
		name = null;
		surname = null;
		password = null;
		email = null;
		phone = "911407506";
		return createCommand(channel);

	}

	private Command getUserUpdateCommandChangeEmail(ChannelEntity channel) {
		name = null;
		surname = null;
		password = null;
		email = "newemail@new.com";
		phone = null;
		return createCommand(channel);
	}

	private Command getUserUpdateCommandChangePassword(ChannelEntity channel) {
		name = user1.getName() + "_NEW_NAME";
		surname = user1.getSurname() + "_NEW_SURNAME";
		password = "NEWPASSWORD";
		email = null;
		phone = null;
		return createCommand(channel);
	}

	private Command createCommand(ChannelEntity channel) {
		UserEdit params = new UserEdit();
		params.setId(user1.getUUID());
		params.setName(name);
		params.setSurname(surname);
		params.setPassword(password);
		params.setPhone(phone);
		params.setEmail(email);

		Command updateUser = new Command();
		updateUser.setChannelId(channel.getUUID());
		updateUser.setMethod(Command.METHOD_UPDATE_USER);
		updateUser.setParams(jsonMapper.convertValue(params, ObjectNode.class));
		return updateUser;
	}

	private void assertIsImage(User user) throws FileNotFoundException {
		// Check images
		Content content = administrativeService.getUserAvatar(user.getId());
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
