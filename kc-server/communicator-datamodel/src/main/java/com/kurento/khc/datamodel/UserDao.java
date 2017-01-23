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

package com.kurento.khc.datamodel;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.kurento.agenda.datamodel.pojo.KhcInvalidDataInfo.Code;
import com.kurento.agenda.datamodel.pojo.KhcNotFoundInfo;
import com.kurento.agenda.datamodel.pojo.TimelineParty.PartyType;
import com.kurento.khc.KhcInvalidDataException;
import com.kurento.khc.KhcNotFoundException;
import com.kurento.khc.utils.FileRepository;

@Component
public class UserDao extends BaseDao {

	@Autowired
	private AccountDao accountDao;

	@Autowired
	private GroupDao groupDao;

	@Autowired
	private ChannelDao notificationChannelDao;

	@Autowired
	private ContentDao contentDao;

	@Autowired
	private MessageDao messageDao;

	@Autowired
	private TimelineDao timelineDao;

	@Autowired
	private CommandDao commandDao;

	@Autowired
	private FileRepository repository;

	@Value("${kurento.command.admin.allowImpersonation:#{null}}")
	private Boolean adminAllowToImpersonate = false;

	/* Create user entity not associated to any organization */
	@Transactional(noRollbackFor = RuntimeException.class)
	public UserEntity createAccountUser(UserEntity user, AccountEntity account) {

		// Set implicit user data
		user.setActive(true);

		// Create User entity
		log.debug("User on CreateAccountUser:: " + user.toString());
		super.save(user);

		// Set account
		AccountEntity dbAccount = accountDao.attach(account);
		user.setAccount(dbAccount);
		
		user.verifyRecord();

		return user;
	}

	@SuppressWarnings("unchecked")
	@Transactional
	public void deleteUser(UserEntity user) {
		UserEntity dbUser = attach(user);

		// Delete user messages
		List<MessageEntity> messages;
		do {
			messages = (List<MessageEntity>) em
					.createNamedQuery(MessageEntity.NQ_NAME_GET_USER_MSG)
					.setParameter(MessageEntity.NQ_PARAM_FROM, dbUser)
					.setMaxResults(1000).getResultList();
			for (MessageEntity message : messages) {
				messageDao.delete(message);
			}
		} while (messages.size() > 0);

		for (ChannelEntity nc : dbUser.getNotificationChannels().values()) {
			notificationChannelDao.deleteNotificationChannel(nc);
		}
		dbUser.getNotificationChannels().clear();

		for (GroupEntity dbGroup : getUserGroups(dbUser)) {
			groupDao.removeGroupMember(dbGroup, dbUser);
		}
		dbUser.getGroups().clear();

		for (GroupEntity dbAdministeredGroup : dbUser.getAdministeredGroups()
				.values()) {
			groupDao.removeGroupAdmin(dbAdministeredGroup, dbUser);
		}
		dbUser.getAdministeredGroups().clear();

		// By now this list only contains user timelines
		for (TimelineEntity dbTimeline : new ArrayList<TimelineEntity>(dbUser
				.getTimelines().values())) {
			dbUser.getTimelines().remove(dbTimeline.getPartyUUID());
			timelineDao.deleteTimeline(dbTimeline);
		}
		dbUser.getTimelines().clear();

		dbUser.setAdministeredAccount(null);
		dbUser.setAccount(null);

		dbUser.getLocalContacts().clear();

		super.delete(dbUser);
	}

	@Transactional
	public UserEntity updateUser(UserEntity user) {
		em.detach(user);
		UserEntity dbUser = attach(user);

		if (user.isActive() != null) {
			dbUser.setActive(user.isActive());
		}
		if (user.getPassword() != null) {
			dbUser.setPassword(user.getPassword());
		}
		if (user.getEmail() != null) {
			dbUser.setEmail(user.getEmail());
		}
		if (user.isQos() != null) {
			dbUser.setQos(user.isQos());
		}
		if (user.getPhone() != null) {
			dbUser.setPhone(user.getPhone());
		}
		if (user.getPhoneRegion() != null) {
			dbUser.setPhoneRegion(user.getPhoneRegion());
		}
		if (user.getName() != null) {
			dbUser.setName(user.getName());
		}
		if (user.getSurname() != null) {
			dbUser.setSurname(user.getSurname());
		}
		if (user.getUri() != null) {
			dbUser.setUri(user.getUri());
		}
		dbUser.verifyRecord();
		em.flush();
		return dbUser;

	}

	@Transactional(noRollbackFor = KhcNotFoundException.class)
	public UserEntity findUserById(Long id) {
		return findSingle(UserEntity.class, new String[] { "id" },
				new Object[] { id });
	}

	@Transactional(noRollbackFor = KhcNotFoundException.class)
	public UserEntity findUserByUUID(Long uuid) {
		return findSingle(UserEntity.class, new String[] { "uuid" },
				new Object[] { uuid });
	}

	@Transactional(noRollbackFor = KhcNotFoundException.class)
	public UserEntity findUserByEmail(String email) {
		return findSingle(UserEntity.class, new String[] { "email" },
				new Object[] { email });
	}

	@Transactional(noRollbackFor = KhcNotFoundException.class)
	public UserEntity findUserByPhone(final String phone, String defaultRegion) {

		// Parse phone number
		PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
		String phoneNumber;
		try {
			phoneNumber = phoneUtil.format(phoneUtil.parse(phone,
					defaultRegion != null ? defaultRegion : "ES"),
					PhoneNumberFormat.E164);

			return findSingle(UserEntity.class, new String[] { "phone" },
					new Object[] { phoneNumber, });

		} catch (NumberParseException e1) {
			throw new KhcNotFoundException(
					"Unable to find user by phone due to invalid number: "
							+ phone,
					KhcNotFoundInfo.Code.SYNCHRONIZATION_ERROR,
					UserEntity.class.getSimpleName(),
					new HashMap<String, String>() {
						private static final long serialVersionUID = 1L;
						{
							put("uuid", "" + phone);
						}
					});
		}
	}

	@Transactional
	public AccountEntity getUserAccount(UserEntity user) {

		UserEntity dbUser = attach(user);
		AccountEntity account;
		if ((account = dbUser.getAccount()) != null) {
			account.getId();
		}
		return account;
	}

	@Transactional
	public List<GroupEntity> getUserGroups(UserEntity user) {

		UserEntity dbUser = attach(user);
		List<GroupEntity> groups = new ArrayList<GroupEntity>();
		Collection<GroupEntity> groupCollection = dbUser.getGroups().values();
		groups.addAll(groupCollection);

		return groups;

	}

	@Transactional
	public List<ChannelEntity> getNotificationChannels(UserEntity user) {

		UserEntity dbUser = attach(user);
		return new ArrayList<ChannelEntity>(dbUser.getNotificationChannels()
				.values());
	}

	@Transactional(noRollbackFor = { KhcNotFoundException.class,
			FileNotFoundException.class })
	public ContentEntity getPicture(UserEntity user)
			throws FileNotFoundException {

		final UserEntity dbUser = attach(user);
		ContentEntity picture;
		if ((picture = dbUser.getPicture()) != null) {
			repository.getMediaFile(picture.getContentUrl());
		} else {
			throw new KhcNotFoundException("User avatar not found",
					KhcNotFoundInfo.Code.SYNCHRONIZATION_IGNORE,
					ContentEntity.class.getSimpleName(),
					new HashMap<String, String>() {
						private static final long serialVersionUID = 1L;
						{
							put("UserEntity.uuid", "" + dbUser.getUUID());
						}
					});
		}
		return picture;
	}

	@Transactional
	public void setPicture(UserEntity user, ContentEntity picture) {

		UserEntity dbUser = attach(user);
		ContentEntity currentPic = dbUser.getPicture();
		ContentEntity dbPicture = null;
		if (picture != null) {
			dbPicture = contentDao.attach(picture);
		}

		dbUser.setPicture(dbPicture);

		if (currentPic != null) {
			contentDao.deleteContent(currentPic);
		}
	}

	@Transactional
	public List<TimelineEntity> getUserTimelines(UserEntity user) {
		UserEntity dbUser = attach(user);
		return new ArrayList<TimelineEntity>(dbUser.getTimelines().values());
	}

	@Transactional
	public List<TimelineEntity> getUserTimelinesWithGroups(UserEntity user) {
		UserEntity dbUser = attach(user);
		List<TimelineEntity> timelines = new ArrayList<TimelineEntity>();
		for (TimelineEntity tl : dbUser.getTimelines().values()) {
			if (PartyType.GROUP.equals(tl.getPartyType())) {
				timelines.add(tl);
			}
		}
		return timelines;
	}

	@Transactional
	public List<TimelineEntity> getUserTimelinesWithUsers(UserEntity user) {
		UserEntity dbUser = attach(user);
		List<TimelineEntity> timelines = new ArrayList<TimelineEntity>();
		for (TimelineEntity tl : dbUser.getTimelines().values()) {
			if (PartyType.USER.equals(tl.getPartyType())) {
				timelines.add(tl);
			}
		}
		return timelines;
	}

	@Transactional
	public TimelineEntity getUserTimelineWithParty(UserEntity user,
			Long partyUUID) {
		UserEntity dbUser = attach(user);
		return dbUser.getTimelines().get(partyUUID);
	}

	@Transactional
	public AccountEntity getAccountWhereUserIsAdmin(UserEntity userEntity) {
		UserEntity dbUser = attach(userEntity);
		AccountEntity administeredAccount = dbUser.getAdministeredAccount();
		if (administeredAccount != null) {
			administeredAccount.getId();// This line to avoid lazy behavior
		}
		return administeredAccount;
	}

	@Transactional
	public List<GroupEntity> getGroupsWhereUserIsAdmin(UserEntity userEntity) {
		UserEntity dbUser = attach(userEntity);
		return new ArrayList<GroupEntity>(dbUser.getAdministeredGroups()
				.values());
	}

	@Transactional
	public List<UserEntity> getRootUsers() {
		Query q = em.createNamedQuery(UserEntity.NQ_NAME_GET_ROOT_USERS);
		@SuppressWarnings("unchecked")
		List<UserEntity> roots = q.getResultList();
		return roots;
	}

	@Transactional
	public List<UserEntity> getLocalContacts(UserEntity user) {
		UserEntity dbUser = attach(user);
		return new ArrayList<UserEntity>(dbUser.getLocalContacts().values());
	}

	@Transactional
	public Boolean hasLocalContact(UserEntity user, UserEntity contact) {
		UserEntity dbUser = attach(user);
		UserEntity dbContact = attach(contact);
		return dbUser.getLocalContacts().containsKey(dbContact.getId());
	}

	@Transactional
	public void addLocalContact(UserEntity user, UserEntity contact) {
		UserEntity dbUser = attach(user);
		UserEntity dbContact = attach(contact);
		dbUser.getLocalContacts().put(dbContact.getId(), dbContact);

	}

	// ////////////////////////
	// Security verification
	// ////////////////////////

	@Transactional
	public Boolean isRoot(UserEntity root) {
		try {
			UserEntity dbUser = attach(root);
			log.debug("Verify user {} is root", root.getId());
			return dbUser.isRoot();
		} catch (Exception e) {
			return false;
		}
	}

	@Transactional
	public Boolean isFromAccount(UserEntity user, AccountEntity account) {
		try {
			UserEntity dbUser = attach(user);
			AccountEntity dbAccount = accountDao.attach(account);
			log.debug("Verify user {} belongs to account {}", user.getId(),
					account.getId());
			if (getUserAccount(dbUser).getId().equals(dbAccount.getId())) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
	}

	@Transactional
	public Boolean canDeleteUser(UserEntity deletedUser, UserEntity admin) {
		return accountDao.isAccountAdmin(getUserAccount(deletedUser), admin);
	}

	@Transactional
	public Boolean canEditUser(UserEntity editedUser, UserEntity admin) {
		try {
			UserEntity dbAdmin = attach(admin);
			UserEntity dbUser = attach(editedUser);
			log.debug("Verify user {} can edit user {}", admin.getId(),
					editedUser.getId());
			// Users can edit themselves
			if (dbAdmin.getId().equals(dbUser.getId())) {
				return true;
			}
			return accountDao.isAccountAdmin(getUserAccount(editedUser), admin);
		} catch (Exception e) {
			return false;
		}

	}

	@Transactional
	public Boolean isUserAdmin(UserEntity user, UserEntity admin) {
		try {
			UserEntity dbAdmin = attach(admin);
			UserEntity dbUser = attach(user);
			log.debug("Verify user {} is admin of user {}", admin.getId(),
					user.getId());
			if (dbAdmin.getId().equals(dbUser.getId())) {
				return true;
			}

			return accountDao.isAccountAdmin(dbUser.getAccount(), dbAdmin);
		} catch (Exception e) {
			return false;
		}
	}

	@Transactional
	public Boolean canSeeContact(UserEntity user, UserEntity watcher) {
		try {
			UserEntity dbUser = attach(user);
			UserEntity dbWatcher = attach(watcher);
			log.debug("Verify user {} can see contact {}", watcher.getId(),
					user.getId());

			for (GroupEntity group : getUserGroups(dbUser)) {
				if (groupDao.isGroupMember(group, dbWatcher)) {
					return true;
				}
			}
			return dbWatcher.getLocalContacts().containsKey(dbUser.getId())
					|| isUserAdmin(user, watcher);
		} catch (Exception e) {
			return false;
		}
	}

	public Boolean isAdminAllowToImpersonate() {
		log.trace("Admins are allowed to impersonate other users: {}", adminAllowToImpersonate);
		return adminAllowToImpersonate;
	}

	// /////////////////
	//
	// HELPERS
	//
	// /////////////////

	protected UserEntity attach(final UserEntity user) {
		Assert.notNull(user);
		if (em.contains(user)) {
			return user;
		} else {
			UserEntity dbUser;
			if ((dbUser = em.find(UserEntity.class, user.getId())) == null) {
				throw new KhcNotFoundException(
						"Request to attach unknown user to JPA",
						KhcNotFoundInfo.Code.SYNCHRONIZATION_ERROR,
						UserEntity.class.getSimpleName(),
						new HashMap<String, String>() {
							private static final long serialVersionUID = 1L;
							{
								put("uuid", "" + user.getUUID());
							}
						});
			} else {
				return dbUser;
			}
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void verifyUserConstraints(UserEntity user) {
		Assert.notNull(user);
		// email
		try {
			findUserByEmail(user.getEmail());
			throw new KhcInvalidDataException("Email already registered",
					Code.EMAIL_ALREADY_USED);
		} catch (KhcNotFoundException e1) {
			// Do nothing
		}
		// phone
		try {
			findUserByPhone(user.getPhone(), user.getPhoneRegion());
			throw new KhcInvalidDataException("Phone already registered",
					Code.PHONE_ALREADY_USED);
		} catch (KhcNotFoundException e1) {
			// Do nothing
		}
	}

}