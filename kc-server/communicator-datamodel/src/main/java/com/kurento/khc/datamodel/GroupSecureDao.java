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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class GroupSecureDao {

	@Autowired
	private GroupDao groupDao;
	@Autowired
	private AccountDao accountDao;

	// //////////////
	// Group methods
	// //////////////

	@PreAuthorize("@accountDao.isAccountAdmin(#account,principal.user)")
	public GroupEntity createAccountGroup(GroupEntity group,
			AccountEntity account) {
		return groupDao.createAccountGroup(group, account);
	}

	@PreAuthorize("#owner.id == principal.user.id"
			+ " and #account.groupAutoregister == true "
			+ " and @accountDao.isAccountMember(#account, principal.user)")
	public GroupEntity createAutomanagedGroup(GroupEntity group,
			AccountEntity account, UserEntity owner) {
		return groupDao.createAutomanagedGroup(group, account, owner);
	}

	@PreAuthorize("@groupDao.isGroupAdmin(#group, principal.user)")
	public void deleteGroup(GroupEntity group) {
		groupDao.deleteGroup(group);
	}

	@PreAuthorize("@groupDao.isGroupAdmin(#group, principal.user)")
	public GroupEntity updateGroup(GroupEntity group) {
		return groupDao.updateGroup(group);
	}

	@PreAuthorize("@groupDao.isGroupAdmin(#group, principal.user)")
	public void setPicture(GroupEntity group, ContentEntity picture) {
		groupDao.setPicture(group, picture);
	}

	@PostAuthorize("@groupDao.isGroupAdmin(returnObject, principal.user)"
			+ " or @groupDao.isGroupMember(returnObject, principal.user)")
	public GroupEntity findGroupByUUID(Long uuid) {
		return groupDao.findGroupByUUID(uuid);
	}

	// //////////////
	// Member methods
	// //////////////

	@PreAuthorize("@groupDao.isGroupAdmin(#group, principal.user) "
			+ " and @groupDao.hasSameAccount(#group,#user)")
	public void addGroupMember(GroupEntity group, UserEntity user) {
		groupDao.addGroupMember(group, user);
	}

	@PreAuthorize("@groupDao.isGroupMember(#group,#user) and ( "
			+ "   @groupDao.isGroupAdmin(#group, principal.user)"
			+ "   or (#user.email == authentication.name and #group.automanaged == true)"
			+ " ) ")
	public void removeGroupMember(GroupEntity group, UserEntity user) {
		groupDao.removeGroupMember(group, user);
	}

	@PreAuthorize("@groupDao.isGroupMember(#group, principal.user) or "
			+ "@groupDao.isGroupAdmin(#group, principal.user)")
	public List<UserEntity> getGroupMembers(GroupEntity group) {
		return groupDao.getGroupMembers(group);
	}

	// //////////////
	// Admin methods
	// //////////////

	@PreAuthorize("@groupDao.isGroupAdmin(#group, principal.user)"
			+ " and @groupDao.hasSameAccount(#group, #user)")
	public void addGroupAdmin(GroupEntity group, UserEntity user) {
		groupDao.addGroupAdmin(group, user);
	}

	@PreAuthorize("@groupDao.isGroupAdmin(#group, principal.user)"
			+ " and @groupDao.isGroupMember(#group, #user)"
			+ " and principal.user.id != #user.id")
	public void removeGroupAdmin(GroupEntity group, UserEntity user) {
		groupDao.removeGroupAdmin(group, user);
	}

	@PreAuthorize("@groupDao.isGroupAdmin(#group, principal.user)")
	public List<UserEntity> getGroupAdmins(GroupEntity group) {
		return groupDao.getGroupAdmins(group);
	}

	@PreAuthorize("@groupDao.isGroupAdmin(#group, principal.user)")
	public List<UserEntity> getGroupMembersNotAdmins(GroupEntity group) {
		return groupDao.getGroupMembersNotAdmins(group);
	}

}
