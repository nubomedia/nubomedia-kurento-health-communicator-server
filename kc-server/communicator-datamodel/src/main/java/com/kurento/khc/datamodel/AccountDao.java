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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.kurento.agenda.datamodel.pojo.KhcInvalidDataInfo.Code;
import com.kurento.agenda.datamodel.pojo.KhcNotFoundInfo;
import com.kurento.khc.KhcInvalidDataException;
import com.kurento.khc.KhcNotFoundException;
import com.kurento.khc.utils.FileRepository;

@Component("accountDao")
public class AccountDao extends BaseDao {

	@Autowired
	private GroupDao groupDao;

	@Autowired
	private UserDao userDao;

	@Autowired
	private FileRepository repository;

	@Autowired
	private ContentDao contentDao;

	@Transactional
	public AccountEntity createAccount(AccountEntity account) {
		super.save(account);
		return account;
	}

	private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
	private Integer HOURS24_IN_MILISECONDS = 24 * 3600000;

	@SuppressWarnings("unchecked")
	@Transactional
	public void deleteAccount(AccountEntity account) {

		AccountEntity dbAccount = attach(account);

		// Clear account admins
		for (UserEntity admin : dbAccount.getAdmins().values()) {
			admin.setAdministeredAccount(null);
		}
		dbAccount.getAdmins().clear();
		em.flush();

		// Delete account user records
		List<UserEntity> accMembers;
		do {
			Query q = em.createNamedQuery(UserEntity.NQ_NAME_GET_ACCOUNT_USERS)
					.setParameter(UserEntity.NQ_PARAM_ACCOUNT, dbAccount)
					.setMaxResults(1000);
			accMembers = q.getResultList();
			for (UserEntity user : accMembers) {
				userDao.deleteUser(user);
			}
		} while (accMembers.size() > 0);

		// Delete account groups
		List<GroupEntity> accGroups;
		do {
			Query q = em
					.createNamedQuery(GroupEntity.NQ_NAME_GET_ACCOUNT_GROUPS)
					.setParameter(GroupEntity.NQ_PARAM_ACCOUNT, dbAccount)
					.setMaxResults(1000);
			accGroups = q.getResultList();
			for (GroupEntity group : accGroups) {
				groupDao.deleteGroup(group);
			}
		} while (accGroups.size() > 0);

		// Delete Account record if not referenced by commands
		super.delete(dbAccount);
		em.flush();
	}

	@Transactional(noRollbackFor = { KhcNotFoundException.class,
			FileNotFoundException.class })
	public ContentEntity getPicture(AccountEntity account)
			throws FileNotFoundException {
		final AccountEntity dbAccount = attach(account);
		ContentEntity picture;
		if ((picture = dbAccount.getPicture()) != null) {
			repository.getMediaFile(picture.getContentUrl());
			return picture;
		} else {
			throw new KhcNotFoundException("Unable to find account avatar",
					KhcNotFoundInfo.Code.SYNCHRONIZATION_IGNORE,
					ContentEntity.class.getSimpleName(),
					new HashMap<String, String>() {
						private static final long serialVersionUID = 1L;
						{
							put("uuid", "" + dbAccount.getUUID());
						}
					});
		}
	}

	@Transactional
	public void setPicture(AccountEntity account, ContentEntity picture) {
		AccountEntity dbAccount = attach(account);
		ContentEntity currentPic = dbAccount.getPicture();
		ContentEntity dbPicture = null;

		log.debug("The Id when setPicture on accountDao is:: " + picture);
		log.debug("The contentUrl when setPicture on accountDao is:: "
				+ picture.getContentUrl());
		log.debug("The Id when setPicture on accountDao is:: "
				+ picture.getId());

		if (picture != null) {
			dbPicture = contentDao.attach(picture);
		}
		dbAccount.setPicture(dbPicture);

		if (currentPic != null) {
			contentDao.deleteContent(currentPic);
		}
	}

	@Transactional
	public List<AccountEntity> getAccounts() {
		Query q = em.createNamedQuery(AccountEntity.NQ_NAME_GET_ACCOUNTS);
		@SuppressWarnings("unchecked")
		List<AccountEntity> accounts = q.getResultList();
		return accounts;
	}

	@Transactional(noRollbackFor = KhcNotFoundException.class)
	public AccountEntity findAccountByUUID(Long uuid) {
		return findSingle(AccountEntity.class, new String[] { "uuid" },
				new Object[] { uuid });
	}

	@Transactional(noRollbackFor = KhcNotFoundException.class)
	public AccountEntity findAnyAccountByUUID(Long uuid) {
		return findSingle(AccountEntity.class, new String[] { "uuid" },
				new Object[] { uuid });
	}

	@Transactional(noRollbackFor = KhcNotFoundException.class)
	public AccountEntity findAccountByName(String name) {
		return findSingle(AccountEntity.class, new String[] { "name" },
				new Object[] { name });
	}

	@Transactional
	public void addAccountAdmin(AccountEntity account, UserEntity user) {
		UserEntity dbUser = userDao.attach(user);
		AccountEntity dbAccount = user.getAccount();
		dbUser.setAdministeredAccount(dbAccount);
	}

	@Transactional
	public void removeAccountAdmin(AccountEntity account, UserEntity user) {
		UserEntity dbUser = userDao.attach(user);
		dbUser.setAdministeredAccount(null);
	}

	@Transactional
	public List<UserEntity> getAccountAdmins(AccountEntity account) {
		AccountEntity dbAccount = attach(account);
		Map<Long, UserEntity> admins = dbAccount.getAdmins();
		return new ArrayList<UserEntity>(admins.values());
	}

	@SuppressWarnings("unchecked")
	@Transactional
	public List<UserEntity> getAccountUsers(AccountEntity account,
			Integer firstResult, Integer maxResult) {
		AccountEntity dbAccount = attach(account);
		Query q = em.createNamedQuery(UserEntity.NQ_NAME_GET_ACCOUNT_USERS)
				.setParameter(UserEntity.NQ_PARAM_ACCOUNT, dbAccount)
				.setFirstResult(firstResult).setMaxResults(maxResult);
		return q.getResultList();
	}

	@SuppressWarnings("unchecked")
	@Transactional
	public List<GroupEntity> getAccountGroups(AccountEntity account,
			Integer firstResult, Integer maxResult) {
		AccountEntity dbAccount = attach(account);
		Query q = em.createNamedQuery(GroupEntity.NQ_NAME_GET_ACCOUNT_GROUPS)
				.setParameter(GroupEntity.NQ_PARAM_ACCOUNT, dbAccount)
				.setFirstResult(firstResult).setMaxResults(maxResult);
		return q.getResultList();
	}

	@SuppressWarnings("unchecked")
	@Transactional
	public List<GroupEntity> searchAccountGroupsByFilter(AccountEntity account,
			String pattern, Integer firstResult, Integer maxResult) {
		AccountEntity dbAccount = attach(account);
		pattern = pattern.replaceAll("\\s+", "%");
		Query q = em
				.createNamedQuery(
						GroupEntity.NQ_NAME_SEARCH_ACCOUNT_GROUPS_BY_FILTER)
				.setParameter(UserEntity.NQ_PARAM_ACCOUNT, dbAccount)
				.setParameter(AccountEntity.NQ_PARAM_PATTERN,
						pattern.isEmpty() ? "%" : "%" + pattern + "%")
				.setFirstResult(firstResult).setMaxResults(maxResult);
		return q.getResultList();
	}

	@SuppressWarnings("unchecked")
	@Transactional
	public List<CallEntity> searchAccountCallByDate(AccountEntity account,
			Date startDate, Date endDate, Integer firstResult, Integer maxResult) {
		AccountEntity dbAccount = attach(account);
		try {
			startDate = sdf.parse(sdf.format(startDate));
		} catch (ParseException e) {
			log.warn("Error:: ", e);
		}
		Long endDatePlus24 = endDate.getTime() + HOURS24_IN_MILISECONDS;

		Query q = em
				.createNamedQuery(
						CallEntity.NQ_NAME_GET_CALLS_BY_RANGE_OF_DATES)
				.setParameter(CallEntity.NQ_PARAM_TIMESTAMP,
						startDate.getTime())
				.setParameter(CallEntity.NQ_PARAM_END_TIMESTAMP_PLUS_TIME,
						endDatePlus24)
				.setParameter(UserEntity.NQ_PARAM_ACCOUNT, dbAccount)
				.setFirstResult(firstResult).setMaxResults(maxResult);
		return q.getResultList();
	}

	@SuppressWarnings("unchecked")
	@Transactional
	public List<CallEntity> searchAccountAllCallByDate(AccountEntity account,
			Date startDate, Date endDate) {
		AccountEntity dbAccount = attach(account);

		try {
			startDate = sdf.parse(sdf.format(startDate));
		} catch (ParseException e) {
			log.warn("Error:: ", e);
		}
		Long endDatePlus24 = endDate.getTime() + HOURS24_IN_MILISECONDS;

		Query q = em
				.createNamedQuery(
						CallEntity.NQ_NAME_GET_CALLS_BY_RANGE_OF_DATES)
				.setParameter(CallEntity.NQ_PARAM_TIMESTAMP,
						startDate.getTime())
				.setParameter(CallEntity.NQ_PARAM_END_TIMESTAMP_PLUS_TIME,
						endDatePlus24)
				.setParameter(UserEntity.NQ_PARAM_ACCOUNT, dbAccount);
		return q.getResultList();
	}

	@SuppressWarnings("unchecked")
	@Transactional
	public List<UserEntity> getAccountUsersNotAccountAdmins(
			AccountEntity account) {
		AccountEntity dbAccount = attach(account);
		Query q = em
				.createNamedQuery(AccountEntity.NQ_NAME_GET_ACCOUNT_USERS_NOT_ACC_ADMIN);
		q.setParameter(AccountEntity.NQ_PARAM_ACCOUNT, dbAccount);

		List<UserEntity> users = q.getResultList();

		return users;
	}

	@SuppressWarnings("unchecked")
	@Transactional
	public List<UserEntity> getAccountUsersNotInGroup(AccountEntity account,
			GroupEntity group, Integer firstResult, Integer maxResult) {

		AccountEntity dbAccount = attach(account);
		GroupEntity dbGroup = groupDao.attach(group);

		Query q = em
				.createNamedQuery(
						AccountEntity.NQ_NAME_GET_ACCOUNT_USERS_NOT_IN_GROUP)
				.setParameter(AccountEntity.NQ_PARAM_ACCOUNT, dbAccount)
				.setParameter(AccountEntity.NQ_PARAM_GROUP, dbGroup)
				.setFirstResult(firstResult).setMaxResults(maxResult);

		List<UserEntity> users = q.getResultList();
		return users;

	}

	@SuppressWarnings("unchecked")
	@Transactional
	public List<UserEntity> searchAccountUsersByFilter(AccountEntity account,
			String pattern, Integer firstResult, Integer maxResult) {
		AccountEntity dbAccount = attach(account);
		pattern = pattern.replaceAll("\\s+", "%");

		Query q = em
				.createNamedQuery(
						AccountEntity.NQ_NAME_SEARCH_ACCOUNT_USERS_BY_FILTER)
				.setParameter(AccountEntity.NQ_PARAM_ACCOUNT, dbAccount)
				.setParameter(AccountEntity.NQ_PARAM_PATTERN,
						pattern.isEmpty() ? '%' : '%' + pattern + '%')
				.setFirstResult(firstResult).setMaxResults(maxResult);
		return q.getResultList();
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void verifyAccountConstraints(AccountEntity account) {
		Assert.notNull(account);
		try {
			findAccountByName(account.getName());
			throw new KhcInvalidDataException("Account already exists",
					Code.ACCOUNT_ALREADY_EXISTS);
		} catch (KhcNotFoundException e) {
			log.warn("Error:: ", e);
			// Account does not exists ==> problem must be different
		}
	}

	// /////////////////////////////////////
	// Security verifications
	// /////////////////////////////////////

	@Transactional
	public Boolean isAccountAdmin(AccountEntity account, UserEntity admin) {
		UserEntity dbAdmin;
		AccountEntity dbAccount;
		try {
			dbAdmin = userDao.attach(admin);
			dbAccount = attach(account);
		} catch (Exception e) {
			log.warn("Error:: ", e);
			return false;
		}

		if (dbAccount.getAdmins().containsKey(dbAdmin.getId())
				|| dbAdmin.isRoot()) {
			return true;
		} else {
			return false;
		}
	}

	@Transactional
	public Boolean isAccountMember(AccountEntity account, UserEntity member) {
		UserEntity dbUser;
		AccountEntity dbAccount;
		try {
			dbUser = userDao.attach(member);
			dbAccount = attach(account);
		} catch (Exception e) {
			return false;
		}

		if (dbUser.getAccount().getId() == dbAccount.getId()) {
			return true;
		} else {
			return false;
		}
	}

	@Transactional
	public Boolean isPublicAccount(AccountEntity account) {
		try {
			AccountEntity dbAccount = attach(account);
			return dbAccount.isPub();
		} catch (Exception e) {
			return false;
		}
	}

	// /////////////////
	//
	// HELPERS
	//
	// /////////////////

	protected AccountEntity attach(final AccountEntity account) {
		Assert.notNull(account);

		if (em.contains(account)) {
			return account;
		} else {
			AccountEntity dbAccount;
			if ((dbAccount = em.find(AccountEntity.class, account.getId())) == null) {
				throw new KhcNotFoundException(
						"Unable to attach unknown account to JPA",
						KhcNotFoundInfo.Code.SYNCHRONIZATION_ERROR,
						AccountEntity.class.getSimpleName(),
						new HashMap<String, String>() {
							private static final long serialVersionUID = 1L;
							{
								put("uuid", "" + account.getUUID());
							}
						});
			} else {
				return dbAccount;
			}
		}
	}
}
