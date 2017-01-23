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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.MapKey;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

@Entity
@NamedQueries({
		@NamedQuery(name = AccountEntity.NQ_NAME_GET_ACCOUNTS, query = ""
				+ "SELECT acc FROM AccountEntity acc"),

		@NamedQuery(name = AccountEntity.NQ_NAME_GET_ACCOUNT_USERS_NOT_ACC_ADMIN, query = ""
				+ "SELECT user FROM UserEntity user WHERE user.account = :"
				+ AccountEntity.NQ_PARAM_ACCOUNT
				+ " AND user.administeredAccount IS NULL"
				+ " ORDER by user.name, user.surname"),

		@NamedQuery(name = AccountEntity.NQ_NAME_SEARCH_ACCOUNT_USERS_NOT_ACC_ADMIN, query = ""
				+ "SELECT user FROM UserEntity user WHERE user.account = :"
				+ AccountEntity.NQ_PARAM_ACCOUNT
				+ " AND user.administeredAccount IS NULL"
				+ " AND ("
				+ " user.name LIKE :"
				+ AccountEntity.NQ_PARAM_PATTERN
				+ " OR user.surname LIKE :"
				+ AccountEntity.NQ_PARAM_PATTERN
				+ ")" + " ORDER by user.name, user.surname"),

		@NamedQuery(name = AccountEntity.NQ_NAME_GET_ACCOUNT_USERS_NOT_IN_GROUP, query = ""
				+ "SELECT DISTINCT user FROM UserEntity user "
				+ " WHERE"
				+ " user.account = :"
				+ AccountEntity.NQ_PARAM_ACCOUNT
				+ " AND :"
				+ AccountEntity.NQ_PARAM_GROUP
				+ " NOT MEMBER OF user.groups"
				+ " ORDER by user.name, user.surname"),

		@NamedQuery(name = AccountEntity.NQ_NAME_SEARCH_ACCOUNT_USERS_BY_FILTER, query = ""
				+ "SELECT user FROM UserEntity user "
				+ " WHERE"
				+ " user.account = :"
				+ AccountEntity.NQ_PARAM_ACCOUNT
				+ " AND ("
				+ " UPPER(user.name) LIKE UPPER (:"
				+ AccountEntity.NQ_PARAM_PATTERN
				+ ") OR UPPER(user.surname) LIKE UPPER (:"
				+ AccountEntity.NQ_PARAM_PATTERN
				+ ") OR UPPER(user.email) LIKE UPPER (:"
				+ AccountEntity.NQ_PARAM_PATTERN
				+ ") OR UPPER(user.phone) LIKE UPPER (:"
				+ AccountEntity.NQ_PARAM_PATTERN
				+ "))"
				+ " ORDER by user.name, user.surname") })
public class AccountEntity extends BaseEntity {
	private static final long serialVersionUID = 1L;

	public static final String NQ_NAME_GET_ACCOUNTS = "AccountEntity.getAccouts";


	public static final String NQ_NAME_GET_ACCOUNT_USERS_NOT_ACC_ADMIN = "AccountEntity.getUserNotAccAdmin";
	public static final String NQ_NAME_SEARCH_ACCOUNT_USERS_NOT_ACC_ADMIN = "AccountEntity.searchUserNotAccAdmin";

	public static final String NQ_NAME_GET_ACCOUNT_USERS_NOT_IN_GROUP = "AccountEntity.searchUserNotInGroup";

	public static final String NQ_NAME_SEARCH_ACCOUNT_USERS_BY_FILTER = "AccountEntity.searchUserByFilter";

	public static final String NQ_PARAM_ACCOUNT = "account";
	public static final String NQ_PARAM_GROUP = "group";
	public static final String NQ_PARAM_PATTERN = "pattern";

	// Attributes

	@Column(unique = true)
	private String name;

	@Column(nullable = false, columnDefinition = "BIT")
	private Boolean userAutoregister = false;

	@Column(nullable = false, columnDefinition = "BIT")
	private Boolean groupAutoregister = false;

	@Column(nullable = false, columnDefinition = "BIT")
	private Boolean active;

	@Column(nullable = false, columnDefinition = "BIT")
	private Boolean pub = true;

	// Accounts's admins. OneToMany force other side to be owner
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "administeredAccount")
	@MapKey
	private Map<Long, UserEntity> admins = new HashMap<Long, UserEntity>();
	
	@OneToOne(fetch = FetchType.LAZY)
	private ContentEntity picture;

	// Getters & Setters

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Boolean isUserAutoregister() {
		return this.userAutoregister;
	}

	public void setUserAutoregister(Boolean userAutoregister) {
		this.userAutoregister = userAutoregister;
	}

	public Boolean isGroupAutoregister() {
		return groupAutoregister;
	}

	public void setGroupAutoregister(Boolean groupAutoregister) {
		this.groupAutoregister = groupAutoregister;
	}

	public Boolean isActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public Boolean isPub() {
		return pub;
	}

	public void setPub(Boolean pub) {
		this.pub = pub;
	}

	protected Map<Long, UserEntity> getAdmins() {
		return admins;
	}
	
	protected void setPicture(ContentEntity picture) {
		this.picture = picture;
	}

	protected ContentEntity getPicture() {
		return picture;
	}

	// Helpers

	public String getAdminAccountRolename() {
		return "ROLE_ADMIN_ACCOUNT_" + id;
	}

}
