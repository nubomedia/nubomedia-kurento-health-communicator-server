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

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class UserSecureDao {

	@Autowired
	private UserDao userDao;

	@Autowired
	private TimelineDao timelineDao;

	@PersistenceContext
	protected EntityManager em;

	@PreAuthorize("@accountDao.isAccountAdmin(#account, principal.user)")
	public UserEntity createAccountUser(UserEntity user, AccountEntity account) {
		return userDao.createAccountUser(user, account);
	}

	@PreAuthorize("#account.userAutoregister == true and isAnonymous()")
	public UserEntity createAutoregisterUser(UserEntity user,
			AccountEntity account) {
		return userDao.createAccountUser(user, account);
	}

	@PreAuthorize("@userDao.canDeleteUser(#user, principal.user)")
	public void deleteUser(UserEntity user) {
		userDao.deleteUser(user);
	}

	@PreAuthorize("@userDao.canEditUser(#user, principal.user)")
	public UserEntity updateUser(UserEntity user) {
		return userDao.updateUser(user);
	}

	@PreAuthorize("@userDao.canEditUser(#user, principal.user)")
	public void setPicture(UserEntity user, ContentEntity picture) {
		userDao.setPicture(user, picture);
	}

	@PostAuthorize("@userDao.isUserAdmin(returnObject, principal.user)")
	public UserEntity findUserByUUID(Long uuid) {
		return userDao.findUserByUUID(uuid);
	}

	@PostAuthorize("@userDao.isUserAdmin(returnObject, principal.user)")
	public UserEntity findUserByEmail(String email) {
		return userDao.findUserByEmail(email);
	}

	@PostAuthorize("@userDao.canSeeContact(returnObject, principal.user)")
	public UserEntity findContactByUUID(Long uuid) {
		return userDao.findUserByUUID(uuid);
	}

	@PostFilter("@groupDao.isGroupAdmin(filterObject, principal.user)")
	public List<GroupEntity> getGroupsWhereUserIsMember(UserEntity user) {
		List<GroupEntity> groups = userDao.getUserGroups(user);
		return groups;
	}

	@PostFilter("@groupDao.isGroupAdmin(filterObject, principal.user)")
	public List<GroupEntity> getGroupsWhereUserIsAdmin(UserEntity user) {
		List<GroupEntity> groups = userDao.getGroupsWhereUserIsAdmin(user);
		return groups;
	}

	@PostAuthorize("@accountDao.isAccountAdmin(returnObject, principal.user)")
	public AccountEntity getAccountWhereUserIsAdmin(UserEntity userEntity) {
		return userDao.getAccountWhereUserIsAdmin(userEntity);
	}

	@PostFilter("@groupDao.canSendMessage(filterObject,#user)"
			+ " and @groupDao.isGroupMember (filterObject, principal.user)")
	public List<GroupEntity> getGroupsWhereUserCanSendMessage(UserEntity user) {
		return userDao.getUserGroups(user);
	}

	@PostFilter("@groupDao.isGroupMember(filterObject, principal.user)")
	public List<GroupEntity> getGroupsWhereUserCanReadMessage(UserEntity user) {
		return userDao.getUserGroups(user);
	}

	@PreAuthorize("@userDao.isRoot(principal.user)")
	public List<UserEntity> getRootUsers() {
		return userDao.getRootUsers();
	}

	@PreAuthorize("@userDao.isRoot(principal.user)")
	public boolean hasPermissionRoot() {
		return true;
	}

}