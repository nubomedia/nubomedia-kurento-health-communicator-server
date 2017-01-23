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

import java.util.HashMap;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;

@Entity
@NamedQueries({
		@NamedQuery(name = GroupEntity.NQ_NAME_GET_ACCOUNT_GROUPS, query = ""
				+ "SELECT group FROM GroupEntity group WHERE group.account = :"
				+ UserEntity.NQ_PARAM_ACCOUNT),

		@NamedQuery(name = GroupEntity.NQ_NAME_SEARCH_ACCOUNT_GROUPS_BY_FILTER, query = ""
				+ "SELECT group FROM GroupEntity group WHERE group.account = :"
				+ UserEntity.NQ_PARAM_ACCOUNT
				+ " AND group.name LIKE :"
				+ AccountEntity.NQ_PARAM_PATTERN + " ORDER BY group.name") })
public class GroupEntity extends BaseEntity {
	private static final long serialVersionUID = 1L;

	public final static String ANYONE_NAME = "ANYONE";
	public static final String NQ_PARAM_ACCOUNT = "account";
	public static final String NQ_NAME_GET_ACCOUNT_GROUPS = "GroupEntity.getGroups";
	public static final String NQ_NAME_SEARCH_ACCOUNT_GROUPS_BY_FILTER = "GroupEntity.searchGroupsByFilter";

	// Attributes

	@Column(nullable = false)
	private String name;

	private Long localId;

	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	private ContentEntity picture;

	@Column(columnDefinition = "BIT")
	private Boolean automanaged = false;

	private String phone;

	private String phoneRegion;

	// This side is owner
	@ManyToOne(fetch = FetchType.LAZY)
	private AccountEntity account;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "GroupEntity_members_UserEntity")
	// Means owner
	@MapKey
	private Map<Long, UserEntity> members = new HashMap<Long, UserEntity>();

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "GroupEntity_admins_UserEntity")
	// Means owner
	@MapKey
	private Map<Long, UserEntity> admins = new HashMap<Long, UserEntity>();

	// Each group has just one conversation
	@OneToOne(fetch = FetchType.LAZY)
	private ConversationEntity conversation;

	// Getters & Setters

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getLocalId() {
		return localId;
	}

	public void setLocalId(Long localId) {
		this.localId = localId;
	}

	public Boolean isAutomanaged() {
		return automanaged;
	}

	public void setAutomanaged(Boolean automanaged) {
		this.automanaged = automanaged;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getPhoneRegion() {
		return this.phoneRegion;
	}

	public void setPhoneRegion(String phoneRegion) {
		this.phoneRegion = phoneRegion;
	}

	protected ContentEntity getPicture() {
		return this.picture;
	}

	protected void setPicture(ContentEntity picture) {
		this.picture = picture;
	}

	protected void setAccount(AccountEntity account) {
		this.account = account;
	}

	protected AccountEntity getAccount() {
		return account;
	}

	protected Map<Long, UserEntity> getMembers() {
		return members;
	}

	protected Map<Long, UserEntity> getAdmins() {
		return admins;
	}

	protected ConversationEntity getConversation() {
		return conversation;
	}

	protected void setConversation(ConversationEntity conversation) {
		this.conversation = conversation;
	}

	// Helpers

	public String getImplicitRolename() {
		return "ROLE_GROUP_" + id;
	}

	protected String getImplicitRoleDisplayname() {
		return ANYONE_NAME;
	}

	@Override
	public String toString() {
		String str = "|-- GROUP RECORD\n" + "|\tID:" + getId() + "\n"
				+ "|\tName: " + getName() + "\n";

		return str;
	}

}