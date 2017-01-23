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

package com.kurento.khc.test.utils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.stereotype.Component;

import com.kurento.agenda.datamodel.pojo.Channel;
import com.kurento.agenda.datamodel.pojo.Content;
import com.kurento.agenda.datamodel.pojo.Timeline.State;
import com.kurento.agenda.datamodel.pojo.TimelineParty.PartyType;
import com.kurento.agenda.datamodel.pojo.User;
import com.kurento.khc.KhcNotFoundException;
import com.kurento.khc.datamodel.AccountDao;
import com.kurento.khc.datamodel.AccountEntity;
import com.kurento.khc.datamodel.ChannelDao;
import com.kurento.khc.datamodel.ChannelEntity;
import com.kurento.khc.datamodel.CommandDao;
import com.kurento.khc.datamodel.ContentDao;
import com.kurento.khc.datamodel.ContentEntity;
import com.kurento.khc.datamodel.GroupDao;
import com.kurento.khc.datamodel.GroupEntity;
import com.kurento.khc.datamodel.MessageDao;
import com.kurento.khc.datamodel.MessageEntity;
import com.kurento.khc.datamodel.TimelineDao;
import com.kurento.khc.datamodel.TimelineEntity;
import com.kurento.khc.datamodel.UserDao;
import com.kurento.khc.datamodel.UserEntity;
import com.kurento.khc.security.KhcUserDetails;
import com.kurento.khc.security.KhcUserDetailsService;
import com.kurento.khc.services.v2.ContentService;
import com.kurento.khc.services.v2.internal.command.AbstractCommand;
import com.kurento.khc.utils.FileRepository;

@Component("khcTestUtils")
public class KhcTestUtils {

	private static final String SECRETA = "secreta";
	private final Random rnd = new SecureRandom();

	@Autowired
	private UserDao userDao;

	@Autowired
	private AccountDao accountDao;

	@Autowired
	private GroupDao groupDao;

	@Autowired
	private MessageDao messageDao;

	@Autowired
	private TimelineDao timelineDao;

	@Autowired
	private ChannelDao channelDao;

	@Autowired
	private CommandDao commandDao;

	@Autowired
	private ContentDao contentDao;

	@Autowired
	private Map<String, AbstractCommand> commandReference = new HashMap<String, AbstractCommand>();

	@Autowired
	private KhcUserDetailsService agendaUserDetails;

	@Autowired
	private ContentService contentService;

	@Autowired
	private FileRepository repo;

	AtomicLong sequence = new AtomicLong(0L);
	Integer phoneSeed = rnd.nextInt(99999);

	public AccountEntity createAccount() {
		// Create root account
		AccountEntity rootAccount = new AccountEntity();
		rootAccount.setActive(true);
		accountDao.createAccount(rootAccount);
		return rootAccount;
	}

	public AccountEntity createAutomanagedAccount() {
		// Create root account
		AccountEntity rootAccount = new AccountEntity();
		rootAccount.setName("account-" + rnd.nextInt()
				+ sequence.getAndIncrement());
		rootAccount.setActive(true);
		rootAccount.setGroupAutoregister(true);
		rootAccount.setUserAutoregister(true);
		accountDao.createAccount(rootAccount);
		return rootAccount;
	}

	public UserEntity createRootUser(String username, AccountEntity account) {

		// Create root user
		UserEntity rootUser = new UserEntity();
		rootUser.setEmail(username + "@kurentotest.com");
		rootUser.setPassword(md5Hex(SECRETA));
		rootUser.setName(username);
		rootUser.setActive(true);
		rootUser.setIsRoot(true);
		userDao.createAccountUser(rootUser, account);

		return rootUser;
	}

	public UserEntity createUser(String username, AccountEntity account,
			Boolean avatar, Boolean qos) throws IOException {
		UserEntity user = new UserEntity();
		user.setEmail(username + "@kurentotest.com");
		user.setPassword(md5Hex(SECRETA));
		user.setName(username + "_name");
		user.setSurname(username + "_surname");
		user.setPhoneRegion("ES");
		user.setPhone("916"
				+ String.format("%06d", phoneSeed + sequence.getAndIncrement()));
		user.setActive(true);
		user.setQos(qos);

		user = userDao.createAccountUser(user, account);

		if (avatar) {
			InputStream image = getImage();
			Content picture = contentService.saveContent(image, "image/jpeg");
			ContentEntity pictureEntity = contentService
					.buildContentEntity(picture);
			contentDao.createContent(pictureEntity);
			userDao.setPicture(user, pictureEntity);
		}

		return user;

	}

	public UserEntity createUser(String username, AccountEntity account,
			Boolean avatar) throws IOException {
		return createUser(username, account, avatar, true);
	}

	public UserEntity createUser(UserEntity user, AccountEntity account) {
		return userDao.createAccountUser(user, account);
	}

	public ChannelEntity createChannel(UserEntity user) {
		return createChannel(user, Channel.DEV_NULL);
	}

	public ChannelEntity createChannel(UserEntity user, String type) {
		ChannelEntity notch = new ChannelEntity();
		notch.setInstanceId("" + rnd.nextInt());
		notch.setRegisterId(String.valueOf(rnd.nextInt()));
		notch.setRegisterType(type);
		notch.setLocaleString("es");
		return channelDao.createNotificationChannel(notch, user);
	}

	public List<ChannelEntity> getUserChannels(UserEntity owner) {
		return userDao.getNotificationChannels(owner);
	}

	public void addGroupMember(GroupEntity group, UserEntity user) {
		groupDao.addGroupMember(group, user);
	}

	public List<UserEntity> getGroupMembers(GroupEntity group) {
		return groupDao.getGroupMembers(group);
	}

	public void addGroupAdmin(GroupEntity group, UserEntity user) {
		groupDao.addGroupAdmin(group, user);
	}

	public void addGroupAvatar(GroupEntity group) throws IOException {
		Content picture = contentService.saveContent(getImage(), "image/jpeg");
		ContentEntity pictureEntity = contentService
				.buildContentEntity(picture);
		contentDao.createContent(pictureEntity);

		groupDao.setPicture(group, pictureEntity);
	}

	public ContentEntity getGroupAvatar(GroupEntity group)
			throws FileNotFoundException {
		return groupDao.getPicture(group);
	}

	public void deleteGroupAvatarFile(GroupEntity group)
			throws FileNotFoundException {
		ContentEntity content;
		if ((content = groupDao.getPicture(group)) != null) {
			repo.deleteMedia(content.getContentUrl());
		}
	}


	public void addAccountAdmin(AccountEntity account, UserEntity user) {
		accountDao.addAccountAdmin(account, user);
	}

	public void removeAccountAdmin(AccountEntity account, UserEntity user) {
		accountDao.removeAccountAdmin(account, user);
	}

	public GroupEntity createGroup(String groupName, AccountEntity account,
			UserEntity user, Boolean avatar) throws IOException {
		GroupEntity group = new GroupEntity();
		group.setName(groupName);
		group = groupDao.createAutomanagedGroup(group, account, user);

		if (avatar) {
			InputStream image = getImage();
			Content picture = contentService.saveContent(image, "image/jpeg");
			ContentEntity pictureEntity = contentService
					.buildContentEntity(picture);
			contentDao.createContent(pictureEntity);
			groupDao.setPicture(group, pictureEntity);
		}
		return group;
	}

	public GroupEntity createGroup(String groupName, AccountEntity account) {
		GroupEntity group = new GroupEntity();
		group.setName(groupName);
		return groupDao.createAccountGroup(group, account);
	}

	public void login(UserEntity user) {

		UserDetails userDetails = agendaUserDetails.loadUserByUsername(user
				.getEmail());
		List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>(
				userDetails.getAuthorities());
		Authentication authentication = new TestingAuthenticationToken(
				userDetails, userDetails.getPassword(), authorities);
		authentication.setAuthenticated(true);
		SecurityContextHolder.getContext().setAuthentication(authentication);

	}

	public void login(User user) {
		UserEntity userEntity = userDao.findUserByUUID(user.getId());
		login(userEntity);
	}

	public void anonymousLogin() {
		GrantedAuthority serverRole = new SimpleGrantedAuthority("ROLE_SERVER");
		List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
		authorities.add(serverRole);
		Authentication serverContext = new AnonymousAuthenticationToken(
				"foo-key", new KhcUserDetails(new UserEntity(), false, false,
						true, false, authorities), authorities);
		SecurityContextHolder.getContext().setAuthentication(serverContext);
	}

	public void logout() {
		SecurityContextHolder.getContext().setAuthentication(null);
	}

	public InputStream getImage() throws IOException {
		BufferedImage image = new BufferedImage(100, 100,
				BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		ImageIO.write(image, "gif", os);
		return new ByteArrayInputStream(os.toByteArray());
	}

	public Boolean isGroupMember(GroupEntity group, UserEntity member) {
		return groupDao.isGroupMember(group, member);
	}

	public Boolean isGroupAdmin(GroupEntity group, UserEntity admin) {
		return groupDao.isGroupAdmin(group, admin);
	}

	public UserEntity findUserEntity(Long id) {
		return userDao.findUserById(id);
	}

	public void sendMessage(String body, UserEntity from, GroupEntity to) {
		try {
			sendMessage(body, from, to, false);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendMessage(String body, UserEntity from, GroupEntity to,
			Boolean content) throws IOException {
		MessageEntity message = new MessageEntity();
		message.setBody(body);
		message.setToType(PartyType.GROUP);
		message.setToUUID(to.getUUID());

		Map<Long, TimelineEntity> timelines = new HashMap<Long, TimelineEntity>();
		for (UserEntity receiver : groupDao.getGroupMembers(to)) {
			if (!timelines.containsKey(receiver.getId())) {
				TimelineEntity timeline = timelineDao.createTimeline(receiver,
						to);
				// Change timeline state to emulate command behaviour
				timeline.setState(State.ENABLED);
				timelineDao.updateTimeline(timeline);
				timelines.put(receiver.getId(), timeline);
			}
		}

		ContentEntity pictureEntity = null;
		if (content) {
			InputStream image = getImage();
			Content picture = contentService.saveContent(image, "image/jpeg");
			pictureEntity = contentService.buildContentEntity(picture);
			contentDao.createContent(pictureEntity);
		}

		messageDao.sendMessage(message, from, to, pictureEntity);
	}

	public void disableUserTimeline(UserEntity user, GroupEntity to) {
		TimelineEntity timeline;
		try {
			timeline = timelineDao.findTimelineByOwner(user, to.getUUID(),
					PartyType.GROUP);
			timeline.setState(State.DISABLED);
			timelineDao.updateTimeline(timeline);
		} catch (KhcNotFoundException e) {

		}
	}

	public UserEntity getChannelOwner(ChannelEntity channel) {
		return channelDao.getUser(channel);
	}

	public String md5Hex(String data) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("No MD5 algorithm available!");
		}

		return new String(Hex.encode(digest.digest(data.getBytes())));
	}
}
