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

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import com.kurento.agenda.datamodel.pojo.Content;

@Component
public class AccountSecureDao {

	@Autowired
	AccountDao accountDao;
	@Autowired
	GroupDao groupDao;
	@Autowired
	ContentDao contentDao;

	@PreAuthorize("@userDao.isRoot(principal.user)")
	public AccountEntity createAccount(AccountEntity account) {
		return accountDao.createAccount(account);
	}

	@PreAuthorize("@userDao.isRoot(principal.user) and ! @userDao.isFromAccount(principal.user,#account)")
	public void deleteAccount(AccountEntity account) {
		accountDao.deleteAccount(account);
	}

	@PreAuthorize("@accountDao.isAccountAdmin(#account, principal.user)")
	public void updateAccount(AccountEntity account) {
		// TODO implement accountDao.update
	}

	@PreAuthorize("@userDao.isRoot(principal.user)")
	public void updateAvatarAccount(AccountEntity accountEntity, ContentEntity contentEntity){
	if (contentEntity != null) {
		contentDao.createContent(contentEntity);
		accountDao.setPicture(accountEntity, contentEntity);
	}
		
	}

	@PreAuthorize("@userDao.isRoot(principal.user)")
	public List<AccountEntity> getAccounts() {
		return accountDao.getAccounts();
	}

	@PostAuthorize("@accountDao.isAccountAdmin(returnObject, principal.user)")
	public AccountEntity findAccountByUUID(Long uuid) {
		return accountDao.findAccountByUUID(uuid);
	}

	@PostAuthorize("isAccountAdmin(returnObject, principal.user)")
	public AccountEntity findAccountByName(String accountName) {
		return accountDao.findAccountByName(accountName);
	}

	@PreAuthorize("@accountDao.isAccountAdmin(#account, principal.user)")
	public void addAccountAdmin(AccountEntity account, UserEntity user) {
		accountDao.addAccountAdmin(account, user);
	}

	@PreAuthorize("@accountDao.isAccountAdmin(#account, principal.user)"
			+ " and #user.email != authentication.name ")
	public void removeAccountAdmin(AccountEntity account, UserEntity user) {
		accountDao.removeAccountAdmin(account, user);
	}

	@PreAuthorize("@accountDao.isAccountAdmin(#account, principal.user)")
	public List<UserEntity> getAccountAdmins(AccountEntity account) {
		return accountDao.getAccountAdmins(account);
	}

	@PreAuthorize("@accountDao.isAccountAdmin(#account, principal.user)")
	public List<UserEntity> getAccountUsers(AccountEntity account,
			Integer firstResult, Integer maxResult) {
		return accountDao.getAccountUsers(account, firstResult, maxResult);
	}

	@PreAuthorize("@accountDao.isAccountAdmin(#account, principal.user)")
	public List<GroupEntity> getAccountGroups(AccountEntity account,
			Integer firstResult, Integer maxResult) {
		return accountDao.getAccountGroups(account, firstResult, maxResult);
	}

	@PreAuthorize("@accountDao.isAccountAdmin(#account, principal.user)")
	public List<GroupEntity> searchAccountGroupsByFilter(AccountEntity account,
			String pattern, Integer firstResult, Integer maxResult) {
		return accountDao.searchAccountGroupsByFilter(account, pattern,
				firstResult, maxResult);
	}

	@PreAuthorize("@accountDao.isAccountAdmin(#account, principal.user)")
	public List<CallEntity> searchAccountCallByDate(AccountEntity account,
			Date startDate, Date endDate, Integer firstResult, Integer maxResult) {
		return accountDao.searchAccountCallByDate(account, startDate, endDate, firstResult, maxResult);
	}
	
	@PreAuthorize("@accountDao.isAccountAdmin(#account, principal.user)")
	public List<CallEntity> searchAccountAllCallByDate(AccountEntity account,
			Date startDate, Date endDate) {
		return accountDao.searchAccountAllCallByDate(account, startDate, endDate);
	}

	@PreAuthorize("@accountDao.isAccountAdmin(#account, principal.user)")
	public List<UserEntity> getAccountUsersNotAccountAdmins(
			AccountEntity account) {
		return accountDao.getAccountUsersNotAccountAdmins(account);
	}

	@PreAuthorize("@accountDao.isAccountAdmin(#account, principal.user)")
	public List<UserEntity> searchAccountUsersByFilter(AccountEntity account,
			String pattern, Integer firstResult, Integer maxResult) {
		return accountDao.searchAccountUsersByFilter(account, pattern,
				firstResult, maxResult);
	}

	@PreAuthorize("@accountDao.isAccountAdmin(#account, principal.user)")
	public List<UserEntity> getAccountUsersNotInGroup(AccountEntity account,
			GroupEntity group, Integer firstResult, Integer maxResult) {
		return accountDao.getAccountUsersNotInGroup(account, group,
				firstResult, maxResult);
	}

	@PreAuthorize("@accountDao.isAccountAdmin(#account, principal.user)")
	public Boolean hasPermissionAdminAccount(AccountEntity account) {
		return true;
	}

	@PostAuthorize("@accountDao.isPublicAccount(returnObject)")
	public AccountEntity getAccountInfo(Long accountId) {
		return accountDao.findAccountByUUID(accountId);
	}

	@PostAuthorize("@accountDao.isPublicAccount(returnObject)")
	public AccountEntity getAccountInfo(String accountName) {
		return accountDao.findAccountByName(accountName);
	}
}
