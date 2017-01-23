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
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.datamodel.pojo.Content;
import com.kurento.agenda.services.pojo.GroupUpdate;
import com.kurento.agenda.services.pojo.TimelineReadResponse;
import com.kurento.khc.KhcNotFoundException;
import com.kurento.khc.datamodel.AccountEntity;
import com.kurento.khc.datamodel.ChannelEntity;
import com.kurento.khc.datamodel.ContentEntity;
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
public class UpdateGroupCommandTest {

	private Random rnd = new SecureRandom();

	private AccountEntity acc, accExt;
	private UserEntity member, groupAdm, accAdm, root, rootExt;
	private GroupEntity group;
	private Content picture;

	private Map<Long, ChannelEntity> channels;

	@Autowired
	private KhcTestUtils utils;

	@Autowired
	private CommandService commandService;
	@Autowired
	private AdministrativeService admService;
	@Autowired
	private ContentService contentService;

	@Autowired
	private FileRepository repo;

	private ObjectMapper jsonMapper = new KhcObjectMapper();

	@Test
	public void testInsertGroupAvatarByGroupAdmin() throws IOException {

		prepareUnmanagedGroupTestWithNoAvatar();
		Command command = getUpdateGroupCmd(groupAdm);
		utils.login(groupAdm);
		commandService.executeCommand(command, picture, false);
		assertIsImage();
	}

	@Test
	public void testInsertGroupAvatarByAccountAdmin() throws IOException {

		prepareUnmanagedGroupTestWithNoAvatar();
		Command command = getUpdateGroupCmd(accAdm);
		utils.login(accAdm);
		commandService.executeCommand(command, picture, false);
		assertIsImage();

	}

	@Test
	public void testInsertGroupAvatarByRoot() throws IOException {

		prepareUnmanagedGroupTestWithNoAvatar();
		Command command = getUpdateGroupCmd(root);
		utils.login(root);
		commandService.executeCommand(command, picture, false);
		assertIsImage();

	}

	@Test
	public void testInsertGroupAvatarByExternalRoot() throws IOException {

		prepareUnmanagedGroupTestWithNoAvatar();
		Command command = getUpdateGroupCmd(rootExt);
		utils.login(rootExt);
		commandService.executeCommand(command, picture, false);
		assertIsImage();
	}

	@Test
	public void testReplaceGroupAvatarByGroupAdmin() throws IOException {
		prepareUnmanagedGroupTestWithAvatar();
		Command command = getUpdateGroupCmd(groupAdm);
		utils.login(groupAdm);
		commandService.executeCommand(command, picture, false);
		assertIsImage();
	}

	@Test
	public void testReplaceGroupAvatarByAccountAdmin() throws IOException {
		prepareUnmanagedGroupTestWithAvatar();
		Command command = getUpdateGroupCmd(accAdm);
		utils.login(accAdm);
		commandService.executeCommand(command, picture, false);
		assertIsImage();
	}

	@Test
	public void testReplaceGroupAvatarByRoot() throws IOException {
		prepareUnmanagedGroupTestWithAvatar();
		Command command = getUpdateGroupCmd(root);
		utils.login(root);
		commandService.executeCommand(command, picture, false);
		assertIsImage();
	}

	@Test
	public void testReplaceGroupAvatarByExternalRoot() throws IOException {
		prepareUnmanagedGroupTestWithAvatar();
		Command command = getUpdateGroupCmd(rootExt);
		utils.login(rootExt);
		commandService.executeCommand(command, picture, false);
		assertIsImage();
	}

	@Test(expected = KhcNotFoundException.class)
	public void testRecoverFromAvatarLoss() throws IOException {
		prepareUnmanagedGroupTestWithWrongAvatar();

		utils.login(member);
		try {
			admService.getGroupAvatar(group.getUUID());
		} catch (FileNotFoundException e) {
			List<Command> pendingCommands = commandService.getPendingCommands(
					channels.get(member.getUUID()).getUUID(), 0L);
			Assert.assertTrue(pendingCommands.size() == 1);
			Assert.assertTrue(pendingCommands.get(0).getMethod()
					.equals(Command.METHOD_DELETE_GROUP_AVATAR));
		}
		admService.getGroupAvatar(group.getUUID());
	}

	@Test
	public void testUpdateGroupName() throws IOException {
		prepareUnmanagedGroupTestWithNoAvatar();

		// Update command group
		String newName = "NEWNAME";
		GroupUpdate groupParam = new GroupUpdate();
		groupParam.setId(group.getUUID());
		groupParam.setName(newName);

		Command command = new Command();
		command.setChannelId(channels.get(groupAdm.getUUID()).getUUID());
		command.setMethod(Command.METHOD_UPDATE_GROUP);
		command.setParams(jsonMapper.convertValue(groupParam, ObjectNode.class));

		utils.login(groupAdm);
		commandService.executeCommand(command, null, false);

		// Verify Group name is updated to admin & member
		List<UserEntity> watchers = Arrays.asList(groupAdm, member);
		for (UserEntity user : watchers) {
			utils.login(user);
			Iterator<Command> pendingCommands = commandService
					.getPendingCommands(channels.get(user.getUUID()).getUUID(),
							0L).iterator();
			assertGroupUpdate(pendingCommands.next(), newName);
			assertTimelineUpdate(pendingCommands.next(), newName);
		}

	}

	private Command getUpdateGroupCmd(UserEntity user) {
		GroupUpdate groupParam = new GroupUpdate();
		groupParam.setId(group.getUUID());

		Command command = new Command();
		command.setChannelId(channels.get(user.getUUID()).getUUID());
		command.setMethod(Command.METHOD_UPDATE_GROUP);
		command.setParams(jsonMapper.convertValue(groupParam, ObjectNode.class));
		return command;
	}

	private void prepareUnmanagedGroupTestWithNoAvatar() throws IOException {
		String name = String.valueOf(rnd.nextInt());
		channels = new HashMap<Long, ChannelEntity>();
		acc = utils.createAutomanagedAccount();
		accExt = utils.createAutomanagedAccount();
		member = utils.createUser(name + "member", acc, false);
		channels.put(member.getUUID(), utils.createChannel(member));
		accAdm = utils.createUser(name, acc, false);
		channels.put(accAdm.getUUID(), utils.createChannel(accAdm));
		groupAdm = utils.createUser(name + "grpAdm", acc, false);
		channels.put(groupAdm.getUUID(), utils.createChannel(groupAdm));
		utils.addAccountAdmin(acc, accAdm);
		root = utils.createRootUser(name + "root", acc);
		channels.put(root.getUUID(), utils.createChannel(root));
		rootExt = utils.createRootUser(name + "rootExt", accExt);
		channels.put(rootExt.getUUID(), utils.createChannel(rootExt));

		group = utils.createGroup(name + "group", acc);
		utils.addGroupAdmin(group, groupAdm);
		utils.addGroupMember(group, member);

		InputStream media = utils.getImage();
		picture = contentService.saveContent(media, "image/jpeg");
	}

	private void prepareUnmanagedGroupTestWithAvatar() throws IOException {
		prepareUnmanagedGroupTestWithNoAvatar();
		utils.addGroupAvatar(group);
	}

	private void prepareUnmanagedGroupTestWithWrongAvatar() throws IOException {
		prepareUnmanagedGroupTestWithAvatar();
		// Delete avatar file
		utils.deleteGroupAvatarFile(group);

	}

	void assertIsImage() throws FileNotFoundException {
		// Get images
		ContentEntity content = utils.getGroupAvatar(group);
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

	private void assertGroupUpdate(Command command, String groupName) {

		GroupUpdate updateGroupParam = jsonMapper.convertValue(
				command.getParams(), GroupUpdate.class);
		Assert.assertTrue(command.getMethod().equals(
				Command.METHOD_UPDATE_GROUP));
		Assert.assertTrue(groupName.equals(updateGroupParam.getName()));
	}

	private void assertTimelineUpdate(Command command, String timelineName) {

		TimelineReadResponse timelineUpdateParam = jsonMapper.convertValue(
				command.getParams(), TimelineReadResponse.class);
		Assert.assertTrue(command.getMethod().equals(
				Command.METHOD_UPDATE_TIMELINE));
		Assert.assertTrue(timelineName.equals(timelineUpdateParam.getParty()
				.getName()));
	}
}
