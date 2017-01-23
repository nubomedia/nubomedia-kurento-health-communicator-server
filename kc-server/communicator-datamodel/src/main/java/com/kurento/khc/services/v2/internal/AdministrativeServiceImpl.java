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

package com.kurento.khc.services.v2.internal;

import java.io.FileNotFoundException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.kurento.agenda.datamodel.pojo.Account;
import com.kurento.agenda.datamodel.pojo.Call;
import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.datamodel.pojo.Content;
import com.kurento.agenda.datamodel.pojo.Group;
import com.kurento.agenda.datamodel.pojo.KhcInvalidDataInfo.Code;
import com.kurento.agenda.datamodel.pojo.PasswordRecovery;
import com.kurento.agenda.datamodel.pojo.User;
import com.kurento.agenda.services.pojo.GroupInfo;
import com.kurento.agenda.services.pojo.UserId;
import com.kurento.khc.KhcInvalidDataException;
import com.kurento.khc.KhcNotFoundException;
import com.kurento.khc.datamodel.AccountDao;
import com.kurento.khc.datamodel.AccountEntity;
import com.kurento.khc.datamodel.AccountSecureDao;
import com.kurento.khc.datamodel.CallEntity;
import com.kurento.khc.datamodel.ContentDao;
import com.kurento.khc.datamodel.ContentEntity;
import com.kurento.khc.datamodel.GroupDao;
import com.kurento.khc.datamodel.GroupEntity;
import com.kurento.khc.datamodel.GroupSecureDao;
import com.kurento.khc.datamodel.MessageDao;
import com.kurento.khc.datamodel.UserDao;
import com.kurento.khc.datamodel.UserEntity;
import com.kurento.khc.datamodel.UserSecureDao;
import com.kurento.khc.qos.QosServer;
import com.kurento.khc.services.v2.AdministrativeService;
import com.kurento.khc.services.v2.CallService;
import com.kurento.khc.services.v2.CommandService;
import com.kurento.khc.services.v2.ContentService;
import com.kurento.khc.services.v2.MessageService;
import com.kurento.khc.utils.FileRepository;
import com.kurento.khc.utils.GuiAdminPanelUtils;
import com.kurento.khc.utils.MailUtils;
import com.kurento.khc.utils.SecurityUtils;

@Service("khcAdministrativeServiceTransactional")
public class AdministrativeServiceImpl implements AdministrativeService {

	private static Logger log = LoggerFactory
			.getLogger(AdministrativeServiceImpl.class);

	private static final Long pwdRecoveryTimeout = Long.valueOf(5 * 60 * 1000); // 5
																				// mins
	private static final SecureRandom rnd = new SecureRandom();

	@Autowired
	private CommandService commandService;
	@Autowired
	private ContentService contentService;
	@Autowired
	private AdministrativeServiceTransactionalImpl admTran;
	@Autowired
	private MessageService messageService;
	@Autowired
	private CallService callService;

	@Autowired
	private AccountSecureDao accountSecureDao;
	@Autowired
	private GroupSecureDao groupSecureDao;
	@Autowired
	private UserSecureDao userSecureDao;

	@Autowired
	private AccountDao accountDao;
	@Autowired
	private GroupDao groupDao;
	@Autowired
	private UserDao userDao;
	@Autowired
	private ContentDao contentDao;
	@Autowired
	private MessageDao messageDao;

	@Autowired
	private SecurityUtils securityUtils;
	@Autowired
	private FileRepository repository;
	@Autowired
	private MailUtils mailService;
	@Autowired
	private GuiAdminPanelUtils adminPanelService;

	@Autowired
	private QosServer qosServer;

	private final Map<String, PasswordRecovery> pwdRecoveryRequests = new HashMap<String, PasswordRecovery>();

	// ///////////////////////////////////////////////////////////
	// Account services
	// ///////////////////////////////////////////////////////////

	// Do not add @Transactional to this method as it requires two transactions
	// if creation fails due to constraint violation
	@Override
	public Account createAccount(Account account) {
		Assert.notNull(account);
		Assert.notNull(account.getName());
		// Build Entity from POJO
		AccountEntity accountEntity = new AccountEntity();

		// Set entity data
		accountEntity.setActive(true);
		accountEntity.setName(account.getName());
		accountEntity
				.setGroupAutoregister(account.isGroupAutoregister() != null ? account
						.isGroupAutoregister() : false);
		accountEntity
				.setUserAutoregister(account.isUserAutoregister() != null ? account
						.isUserAutoregister() : false);
		accountEntity.setPub(account.isPub() != null ? account.isPub() : false);

		try {
			return buildAccountPojo(admTran.createAccount(accountEntity));
		} catch (RuntimeException e) {
			accountDao.verifyAccountConstraints(accountEntity);
			throw e;
		}
	}

	@Override
	@Transactional
	public void deleteAccount(Long accountId) {

		AccountEntity account = accountDao.findAccountByUUID(accountId);
		accountSecureDao.deleteAccount(account);
	}

	@Override
	@Transactional
	public void updateAccount(Account account) {
		AccountEntity accountEntity = accountDao.findAccountByUUID(account
				.getId());
		if (account.getName() != null) {
			accountEntity.setName(account.getName());
		}
		if (account.isGroupAutoregister() != null) {
			accountEntity.setGroupAutoregister(account.isGroupAutoregister());
		}
		if (account.isUserAutoregister() != null) {
			accountEntity.setUserAutoregister(account.isUserAutoregister());
		}
		if (account.isActive() != null) {
			accountEntity.setActive(account.isActive());
		}
		accountSecureDao.updateAccount(accountEntity);
	}

	@Override
	@Transactional
	public void updateAccountAvatar(Account account, Content content) {
		AccountEntity accountEntity = accountDao.findAccountByUUID(account
				.getId());

		accountSecureDao.updateAvatarAccount(accountEntity,
				contentService.buildContentEntity(content));
	}

	@Override
	@Transactional
	public Account getAccount(String accountName) {
		AccountEntity accountEntity = accountSecureDao
				.findAccountByName(accountName);
		return buildAccountPojo(accountEntity);
	}

	@Override
	@Transactional
	public Account getAccount(Long accountId) {
		AccountEntity accountEntity = accountSecureDao
				.findAccountByUUID(accountId);
		return buildAccountPojo(accountEntity);

	}

	@Override
	@Transactional
	public List<Account> getAccounts() {
		List<Account> accounts = new ArrayList<Account>();
		for (AccountEntity account : accountSecureDao.getAccounts()) {
			accounts.add(buildAccountPojo(account));
		}
		return accounts;
	}

	@Override
	@Transactional
	public Account getAccountInfo(Long accountId) {
		AccountEntity accountEntity = accountDao.findAccountByUUID(accountId);
		return buildAccountPojo(accountEntity);

	}

	@Override
	@Transactional
	public Account getAccountInfo(String accountName) {
		AccountEntity accountEntity = accountSecureDao
				.getAccountInfo(accountName);
		return buildAccountPojo(accountEntity);
	}

	@Override
	@Transactional
	public List<User> getAccountAdmins(Long accountId) {
		AccountEntity account = accountDao.findAccountByUUID(accountId);

		List<User> users = new ArrayList<User>();
		for (UserEntity userEntity : accountSecureDao.getAccountAdmins(account)) {
			User user = buildUserPojo(userEntity);
			users.add(user);
		}
		return users;
	}

	@Override
	@Transactional
	public List<User> getAccountUsers(Long accountId, Integer firstResult,
			Integer maxResult) {
		AccountEntity account = accountDao.findAccountByUUID(accountId);

		List<User> users = new ArrayList<User>();
		for (UserEntity userEntity : accountSecureDao.getAccountUsers(account,
				firstResult, maxResult)) {
			User user = buildUserPojo(userEntity);
			users.add(user);
		}
		return users;
	}

	@Override
	@Transactional
	public List<User> getAccountUsersNotAdmins(Long accountId) {
		AccountEntity account = accountDao.findAccountByUUID(accountId);

		List<User> users = new ArrayList<User>();
		for (UserEntity userEntity : accountSecureDao
				.getAccountUsersNotAccountAdmins(account)) {
			User user = buildUserPojo(userEntity);
			users.add(user);
		}
		return users;
	}

	@Override
	@Transactional
	public void addAccountAdmin(Long accountId, Long userId) {
		AccountEntity account = accountDao.findAccountByUUID(accountId);
		UserEntity user = userDao.findUserByUUID(userId);
		accountSecureDao.addAccountAdmin(account, user);
	}

	@Override
	@Transactional
	public void removeAccountAdmin(Long accountId, Long userId) {
		AccountEntity account = accountDao.findAccountByUUID(accountId);
		UserEntity user = userDao.findUserByUUID(userId);
		accountSecureDao.removeAccountAdmin(account, user);
	}

	@Override
	@Transactional
	public Boolean hasPermissionAdminAccount(Long accountId) {
		try {
			AccountEntity account = accountDao.findAccountByUUID(accountId);
			return accountSecureDao.hasPermissionAdminAccount(account);
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	@Transactional
	public List<Group> getAccountGroups(Long accountId, Integer firstResult,
			Integer maxResult) {
		AccountEntity account = accountDao.findAccountByUUID(accountId);

		List<Group> groups = new ArrayList<Group>();
		for (GroupEntity groupEntity : accountSecureDao.getAccountGroups(
				account, firstResult, maxResult)) {
			Group group = buildGroupPojo(groupEntity);
			groups.add(group);
		}
		return groups;
	}

	@Override
	@Transactional
	public List<Group> searchAccountGroupsByFilter(Long accountId,
			String pattern, Integer firstResult, Integer maxResult) {
		AccountEntity account = accountDao.findAccountByUUID(accountId);

		List<Group> groups = new ArrayList<Group>();
		for (GroupEntity groupEntity : accountSecureDao
				.searchAccountGroupsByFilter(account, pattern, firstResult,
						maxResult)) {
			Group group = buildGroupPojo(groupEntity);
			groups.add(group);
		}
		return groups;
	}

	// ///////////////////////////////////////////////////////////
	// call services
	// ///////////////////////////////////////////////////////////
	@Override
	@Transactional
	public List<Call> searchAccountCallsByDate(Long accountId, Date startDate,
			Date endDate, Integer firstResult, Integer maxResult) {
		AccountEntity account = accountDao.findAccountByUUID(accountId);

		List<Call> callList = new ArrayList<Call>();
		for (CallEntity callEntity : accountSecureDao.searchAccountCallByDate(
				account, startDate, endDate, firstResult, maxResult)) {
			Call callItem = callService.buildCallTerminatePojo(callEntity);

			callList.add(callItem);
		}

		return callList;
	}

	@Override
	@Transactional
	public List<Call> searchAccountAllCallsByDate(Long accountId,
			Date startDate, Date endDate) {
		AccountEntity account = accountDao.findAccountByUUID(accountId);

		List<Call> callList = new ArrayList<Call>();
		for (CallEntity callEntity : accountSecureDao
				.searchAccountAllCallByDate(account, startDate, endDate)) {
			Call callItem = callService.buildCallTerminatePojo(callEntity);

			callList.add(callItem);
		}

		return callList;
	}

	// ///////////////////////////////////////////////////////////
	// Organization services
	// ///////////////////////////////////////////////////////////

	@Override
	@Transactional
	public List<User> getAccountUsersNotInGroup(Long accountId, Long groupId,
			Integer firstResult, Integer maxResult) {

		AccountEntity account = accountDao.findAccountByUUID(accountId);
		GroupEntity group = groupDao.findGroupByUUID(groupId);

		List<User> users = new ArrayList<User>();

		for (UserEntity userEntity : accountSecureDao
				.getAccountUsersNotInGroup(account, group, firstResult,
						maxResult)) {
			users.add(buildUserPojo(userEntity));
		}
		return users;
	}

	@Override
	@Transactional
	public List<User> searchAccountUsersByFilter(Long accountId,
			String pattern, Integer firstResult, Integer maxResult) {
		AccountEntity account = accountDao.findAccountByUUID(accountId);
		List<User> users = new ArrayList<User>();
		for (UserEntity userEntity : accountSecureDao
				.searchAccountUsersByFilter(account, pattern, firstResult,
						maxResult)) {
			User user = buildUserPojo(userEntity);
			users.add(user);
		}
		return users;
	}

	// ///////////////////////////////////////////////////////////
	// Group services
	// ///////////////////////////////////////////////////////////

	@Override
	@Transactional
	public Group createAccountGroup(Group group, Long accountId) {
		// Build Entity from POJO
		GroupEntity groupEntity = buildGroupEntity(group);

		// Find account
		AccountEntity account = accountDao.findAccountByUUID(accountId);

		// Create group
		groupEntity = groupSecureDao.createAccountGroup(groupEntity, account);
		return buildGroupPojo(groupEntity);

	}

	@Override
	@Transactional
	public Group getGroup(Long groupId) {
		GroupEntity groupEntity = groupSecureDao.findGroupByUUID(groupId);
		return buildGroupPojo(groupEntity);

	}

	// Do not declare this service as transactional as it requires two
	// transactions in case of FileNotFoundException is hit
	public Content getGroupAvatar(final Long groupId)
			throws FileNotFoundException {
		GroupEntity groupEntity = groupSecureDao.findGroupByUUID(groupId);
		try {
			return contentService.buildContentPojo(groupDao
					.getPicture(groupEntity));
		} catch (KhcNotFoundException | FileNotFoundException e) {
			// Delete group avatar to avoid problems in the future
			GroupInfo groupParam = new GroupInfo();
			groupParam.setId(groupId);
			Command command = new Command();
			command.setMethod(Command.METHOD_DELETE_GROUP_AVATAR);
			command.setParams(groupParam);
			commandService.executeCommand(command, null, true);
			throw e;
		}
	}

	@Override
	@Transactional
	public List<User> getGroupMembers(Long groupId) {
		GroupEntity group = groupDao.findGroupByUUID(groupId);
		List<User> users = new ArrayList<User>();
		for (UserEntity userEntity : groupSecureDao.getGroupMembers(group)) {
			User user = buildUserPojo(userEntity);
			users.add(user);
		}
		return users;
	}

	@Override
	@Transactional
	public List<User> getGroupAdmins(Long groupId) {
		GroupEntity group = groupDao.findGroupByUUID(groupId);
		List<User> users = new ArrayList<User>();
		for (UserEntity userEntity : groupSecureDao.getGroupAdmins(group)) {
			User user = buildUserPojo(userEntity);
			users.add(user);
		}
		return users;
	}

	@Override
	@Transactional
	public List<User> getGroupMembersNotAdmins(Long groupId) {
		GroupEntity group = groupDao.findGroupByUUID(groupId);
		List<User> users = new ArrayList<User>();
		for (UserEntity userEntity : groupSecureDao
				.getGroupMembersNotAdmins(group)) {
			User user = buildUserPojo(userEntity);
			users.add(user);
		}
		return users;
	}

	// ///////////////////////////////////////////////////////////
	// User services
	// ///////////////////////////////////////////////////////////

	// Do not make this service transactional as it requires two transactions
	// when user can not be created
	@Override
	public User createUserInAccount(User user, Long accountId) {
		// Build Entity from POJO
		UserEntity userEntity = buildUserEntity(user);

		// Find parent account
		AccountEntity parentAccount = accountDao.findAccountByUUID(accountId);

		// Create user entity
		try {
			return buildUserPojo(admTran.createUserInAccount(userEntity,
					parentAccount));
		} catch (RuntimeException e) {
			log.debug("catch a runTimeException:: " + e.getMessage());
			userDao.verifyUserConstraints(userEntity);
			throw e;
		}
	}

	// Do not declare this method as @Transactional as it requires 2
	// transactions to operate
	@Override
	public User createUserInAutoregister(User user, Long accountId) {

		return createUserInAutoregister(user, accountId, null);
	}

	// Do not declare this method as @Transactional as it requires 2
	// transactions to operate
	@Override
	public User createUserInAutoregister(User user, Long accountId,
			Content content) {
		UserEntity userEntity = buildUserEntity(user);
		AccountEntity parentAccount = accountDao.findAccountByUUID(accountId);
		try {
			admTran.createUserInAutoregister(userEntity, parentAccount, content);
			return buildUserPojo(userEntity);
		} catch (RuntimeException e) {
			userDao.verifyUserConstraints(userEntity);
			throw e;
		}
	}

	@Override
	@Transactional
	public void deleteUser(Long userId) {

		UserEntity user = userDao.findUserByUUID(userId);
		userSecureDao.deleteUser(user);
	}

	@Override
	@Transactional
	public User getUser(Long userId) {

		UserEntity userEntity = userSecureDao.findContactByUUID(userId);
		return buildUserPojo(userEntity);
	}

	@Override
	@Transactional
	public Content getUserAvatar(final Long userId)
			throws FileNotFoundException {
		try {
			UserEntity userEntity = userSecureDao.findContactByUUID(userId);
			ContentEntity contentEntity = userDao.getPicture(userEntity);
			return contentService.buildContentPojo(contentEntity);
		} catch (KhcNotFoundException | FileNotFoundException e) {
			// Delete contact avatar to avoid problems in the future
			UserId userParam = new UserId();
			userParam.setId(userId);
			Command command = new Command();
			command.setMethod(Command.METHOD_DELETE_CONTACT_AVATAR);
			command.setParams(userParam);
			commandService.executeCommand(command, null, true);
			throw e;
		}
	}

	@Override
	@Transactional
	public Content getAccountAvatar(final Long accountId)
			throws FileNotFoundException {
		try {
			AccountEntity accountEntity = accountSecureDao
					.findAccountByUUID(accountId);
			ContentEntity contentEntity = accountDao.getPicture(accountEntity);
			return contentService.buildContentPojo(contentEntity);
		} catch (KhcNotFoundException | FileNotFoundException e) {
			throw e;
		}
	}

	@Override
	@Transactional
	public User getMe() {
		UserEntity userEntity = securityUtils.getPrincipal();
		User user = buildUserPojo(userEntity);
		return user;
	}

	@Override
	public Content getMeAvatar() throws FileNotFoundException {
		UserEntity userEntity = securityUtils.getPrincipal();
		try {
			ContentEntity contentEntity = userDao.getPicture(userEntity);
			return contentService.buildContentPojo(contentEntity);
		} catch (KhcNotFoundException | FileNotFoundException e) {
			// Delete contact avatar to avoid problems in the future
			UserId userParam = new UserId();
			userParam.setId(userEntity.getUUID());
			Command command = new Command();
			command.setMethod(Command.METHOD_DELETE_CONTACT_AVATAR);
			command.setParams(userParam);
			commandService.executeCommand(command, null, true);
			throw e;
		}
	}

	@Override
	@Transactional
	public User getUserContact(long userId) {
		UserEntity contact = userSecureDao.findContactByUUID(userId);
		return buildUserPojo(contact);
	}

	@Override
	@Transactional
	public Content getUserContactAvatar(final Long userId)
			throws FileNotFoundException {

		try {
			UserEntity contact = userSecureDao.findContactByUUID(userId);
			ContentEntity avatar = userDao.getPicture(contact);
			return contentService.buildContentPojo(avatar);
		} catch (KhcNotFoundException | FileNotFoundException e) {
			// Delete contact avatar to avoid problems in the future
			UserId userParam = new UserId();
			userParam.setId(userId);
			Command command = new Command();
			command.setMethod(Command.METHOD_DELETE_CONTACT_AVATAR);
			command.setParams(userParam);
			commandService.executeCommand(command, null, true);
			throw e;
		}

	}

	@Override
	@Transactional
	public List<User> getRootUsers() {
		List<User> roots = new ArrayList<User>();

		for (UserEntity root : userSecureDao.getRootUsers()) {
			roots.add(buildUserPojo(root));
		}
		return roots;
	}

	@Override
	@Transactional
	public List<Group> getGroupsWhereUserIsMember(Long userId) {
		UserEntity user = userDao.findUserByUUID(userId);

		List<Group> groups = new ArrayList<Group>();
		List<GroupEntity> groupEntities = userSecureDao
				.getGroupsWhereUserIsMember(user);

		for (GroupEntity groupEntity : groupEntities) {
			groups.add(buildGroupPojo(groupEntity));
		}
		return groups;
	}

	@Override
	@Transactional
	public List<Group> getGroupsWhereUserIsAdmin(Long userId) {
		UserEntity user = userDao.findUserByUUID(userId);

		List<Group> groups = new ArrayList<Group>();
		List<GroupEntity> groupEntities = userSecureDao
				.getGroupsWhereUserIsAdmin(user);

		for (GroupEntity groupEntity : groupEntities) {
			groups.add(buildGroupPojo(groupEntity));
		}
		return groups;
	}

	@Override
	@Transactional
	public Account getAdministeredAccount() {

		UserEntity userEntity = securityUtils.getPrincipal();
		AccountEntity accountEntity = userSecureDao
				.getAccountWhereUserIsAdmin(userEntity);
		return buildAccountPojo(accountEntity);
	}

	@Override
	@Transactional
	public Boolean hasPermissionRoot() {
		return userSecureDao.hasPermissionRoot();
	}

	@Override
	@Transactional
	public Boolean isPhoneAvailable(String phone, Long userId,
			String defaultRegion) {
		UserEntity userEntity;
		try {
			userEntity = userDao.findUserByPhone(phone, defaultRegion);
			if (userEntity.getUUID().equals(userId)) {
				log.debug("return true");
				return true;
			}
			return false;
		} catch (KhcNotFoundException e) {
			log.info("The phone is not assigned yet");
			return true;
		} catch (Exception e) {
			log.warn("Error:: ", e);
			return false;
		}
	}

	@Override
	@Transactional
	public Boolean isEmailAvailable(String email) {
		try {
			userDao.findUserByEmail(email);
			return false;
		} catch (KhcNotFoundException e) {
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	// ///////////////////////////////////////////////////////////
	// Password recovery services
	// ///////////////////////////////////////////////////////////

	@Override
	@Transactional
	public PasswordRecovery getSecurityCode(String username) {
		UserEntity user;
		try {
			user = userDao.findUserByEmail(username);
		} catch (KhcNotFoundException e) {
			// Unable to find the user from email
			throw new KhcInvalidDataException("",
					Code.SECURITY_TOKEN_REQUEST_FROM_UNKNOWN_USER);
		}
		// Generate security code
		// 65 = A + 90 = Z + xxxx
		String secCode = new StringBuilder().append(65 + rnd.nextInt(90 - 65))
				.append(65 + rnd.nextInt(90 - 65))
				.append(String.valueOf(rnd.nextInt(9999))).toString();

		// Add request to map
		PasswordRecovery pwdRecovery = new PasswordRecovery();
		pwdRecovery.setSecurityCode(secCode);
		pwdRecovery.setUserId(user.getId());
		pwdRecoveryRequests.put(secCode, pwdRecovery);

		return pwdRecovery;

	}

	@Override
	@Transactional
	public void sendSecurityCode(PasswordRecovery pwdRecovery, String subject,
			String message) {

		UserEntity user;
		try {
			user = userDao.findUserById(pwdRecovery.getUserId());
		} catch (KhcNotFoundException e) {
			// Unable to find the user from email
			throw new KhcInvalidDataException("",
					Code.SECURITY_TOKEN_REQUEST_FROM_UNKNOWN_USER);
		}
		mailService.sendMail(user.getEmail(), subject, message);
	}

	@Override
	@Transactional
	public String getIconHeadWeb() {
		return adminPanelService.getIconHeadWeb();
	}

	@Override
	@Transactional
	public String getTitleHeadWeb() {
		return adminPanelService.getTitleHeadWeb();
	}

	@Override
	@Transactional
	public void changePassword(String securityCode, String newPassword) {
		if (securityCode == null || securityCode.isEmpty()) {
			throw new KhcInvalidDataException(
					"Invalid empty or null security code for password recovery",
					Code.INVALID_SECURITY_TOKEN);
		}

		if (newPassword == null || newPassword.isEmpty()) {
			throw new KhcInvalidDataException(
					"Invalid null or empty password for password recovery",
					Code.NO_CREDENTIALS);
		}

		PasswordRecovery pwdRecovery = pwdRecoveryRequests.remove(securityCode);

		if (pwdRecovery == null
				|| pwdRecovery.getTimestamp() + pwdRecoveryTimeout < System
						.currentTimeMillis()) {
			throw new AccessDeniedException(
					"Invalid or expired security code for password recovery:");
		}

		UserEntity user = userDao.findUserById(pwdRecovery.getUserId());
		user.setPassword(newPassword);
		userDao.updateUser(user);

	}

	// ///////////
	//
	// HELPERS
	//
	// ///////////

	@Override
	@Transactional
	public GroupEntity buildGroupEntity(Group group) {
		Assert.notNull(group);
		GroupEntity groupEntity;
		if (group.getId() != null) {
			groupEntity = groupDao.findGroupByUUID(group.getId());
		} else {
			groupEntity = new GroupEntity();
		}
		groupEntity.setName(group.getName() != null ? group.getName()
				: groupEntity.getName());
		groupEntity.setLocalId(group.getLocalId() != null ? group.getLocalId()
				: groupEntity.getLocalId());
		groupEntity.setPhone(group.getPhone() != null ? group.getPhone()
				: groupEntity.getPhone());
		groupEntity.setPhoneRegion(group.getPhoneRegion() != null ? group
				.getPhoneRegion() : groupEntity.getPhoneRegion());

		return groupEntity;
	}

	@Override
	@Transactional
	public UserEntity buildUserEntity(User user) {

		Assert.notNull(user);
		UserEntity userEntity;
		if (user.getId() != null) {
			userEntity = userDao.findUserByUUID(user.getId());
		} else {
			userEntity = new UserEntity();
		}

		userEntity.setPassword(user.getPassword() != null ? user.getPassword()
				: userEntity.getPassword());
		userEntity.setEmail(user.getEmail() != null ? user.getEmail()
				: userEntity.getEmail());
		userEntity.setQos(user.getQos() != null ? user.getQos() : userEntity
				.isQos());
		userEntity.setPhone(user.getPhone() != null ? user.getPhone()
				: userEntity.getPhone());
		userEntity.setPhoneRegion(user.getPhoneRegion() != null ? user
				.getPhoneRegion() : userEntity.getPhoneRegion());
		userEntity.setName(user.getName() != null ? user.getName() : userEntity
				.getName());
		userEntity.setSurname(user.getSurname() != null ? user.getSurname()
				: userEntity.getSurname());
		userEntity.setUri(user.getUri() != null ? user.getUri() : userEntity
				.getUri());
		userEntity.setIsRoot(user.isRoot() != null ? user.isRoot() : userEntity
				.isRoot());

		return userEntity;
	}

	@Override
	@Transactional
	public Account buildAccountPojo(AccountEntity accountEntity) {
		Account account = null;
		if (accountEntity != null) {
			account = new Account();
			account.setId(accountEntity.getUUID());
			account.setName(accountEntity.getName());
			account.setUserAutoregister(accountEntity.isUserAutoregister());
			account.setGroupAutoregister(accountEntity.isGroupAutoregister());

			try {
				account.setPicture(accountDao.getPicture(accountEntity)
						.getUUID());
			} catch (KhcNotFoundException | FileNotFoundException e) {
				account.setPicture(null);
			}
		}
		return account;
	}

	@Override
	@Transactional
	public Group buildGroupPojo(GroupEntity groupEntity) {
		Assert.notNull(groupEntity);
		Group group = new Group();
		group.setId(groupEntity.getUUID());
		group.setLocalId(groupEntity.getLocalId());
		group.setName(groupEntity.getName());
		group.setPhone(groupEntity.getPhone());
		group.setPhoneRegion(groupEntity.getPhoneRegion());

		try {
			ContentEntity picture;

			if ((picture = groupDao.getPicture(groupEntity)) != null) {
				group.setPicture(picture.getUUID());
			}
		} catch (KhcNotFoundException | FileNotFoundException e) {
			log.debug("Missing avatar file for group {}", group.getId());
		}
		return group;
	}

	@Override
	@Transactional
	public User buildUserPojo(UserEntity userEntity) {
		Assert.notNull(userEntity);
		User user = new User();
		user.setId(userEntity.getUUID());
		user.setEmail(userEntity.getEmail());
		user.setQos(userEntity.isQos());
		user.setName(userEntity.getName());
		user.setSurname(userEntity.getSurname());
		user.setPhone(userEntity.getPhone());
		user.setPhoneRegion(userEntity.getPhoneRegion());
		user.setIsRoot(userEntity.isRoot());
		try {
			user.setPicture(userDao.getPicture(userEntity).getUUID());
		} catch (KhcNotFoundException | FileNotFoundException e) {
			user.setPicture(null);
		}

		return user;
	}

	@Override
	public void clearQosChannels() {
		qosServer.removeAllInstances();
	}

	@Override
	public void reloadQosChannels() {
		qosServer.reloadInstances();
	}

}
