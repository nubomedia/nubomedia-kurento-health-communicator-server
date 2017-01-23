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
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.kurento.agenda.datamodel.pojo.KhcInvalidDataInfo.Code;
import com.kurento.khc.KhcInvalidDataException;

@Entity
@NamedQueries({
		@NamedQuery(name = UserEntity.NQ_NAME_GET_ACCOUNT_USERS, query = ""
				+ "SELECT user FROM UserEntity user WHERE user.account = :"
				+ UserEntity.NQ_PARAM_ACCOUNT),

		@NamedQuery(name = UserEntity.NQ_NAME_GET_ROOT_USERS, query = ""
				+ "SELECT user FROM UserEntity user WHERE user.isRoot = true") })
public class UserEntity extends BaseEntity {
	private static final long serialVersionUID = 1L;

	public static final String NQ_NAME_GET_ACCOUNT_USERS = "UserEntity.getUsers";
	public static final String NQ_NAME_GET_ROOT_USERS = "UserEntity.getRootUsers";

	public static final String NQ_PARAM_ACCOUNT = "account";

	// Attributes
	@Transient
	private String login;

	@Column(unique = true)
	private String email;
	
	@Column(nullable = false, columnDefinition = "BIT")
	private Boolean qos = false;

	@Column(unique = true)
	private String phone;

	private String password;

	@Column(nullable = false, columnDefinition = "BIT")
	private Boolean active;

	@Column(unique = true)
	private String uri;

	private String name;

	private String surname;

	private String phoneRegion;

	@OneToOne(fetch = FetchType.LAZY)
	private ContentEntity picture;

	@Column(columnDefinition = "BIT")
	private Boolean isRoot = false;

	// Owning account. ManyToOne force this side to be owner
	@ManyToOne(fetch = FetchType.LAZY)
	private AccountEntity account;

	// NON-NULL if account administrator. ManyToOne force this side to be owner
	@ManyToOne(fetch = FetchType.LAZY)
	private AccountEntity administeredAccount;

	/*
	 * Groups this user is member of. Other side is owner because users are
	 * added/removed to/from groups
	 */
	@ManyToMany(fetch = FetchType.LAZY, mappedBy = "members")
	@MapKey
	private Map<Long, GroupEntity> groups = new HashMap<Long, GroupEntity>();

	/*
	 * Group this user is administrator of. Other side is owner because users ar
	 * added/removed administration permissions
	 */
	@ManyToMany(fetch = FetchType.LAZY, mappedBy = "admins")
	@MapKey
	private Map<Long, GroupEntity> administeredGroups = new HashMap<Long, GroupEntity>();

	// User's notification channels. OneToMany force other side to be owner
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
	@MapKey
	private Map<Long, ChannelEntity> notificationChannels = new HashMap<Long, ChannelEntity>();

	// Timelines are views of conversations with users or groups
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
	@MapKey(name = "partyUuid")
	private Map<Long, TimelineEntity> timelines = new HashMap<Long, TimelineEntity>();

	// Local Contacts
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "UserEntity_localContact_UserEntity")
	private Map<Long, UserEntity> localContacts = new HashMap<Long, UserEntity>();

	// Getters & Setters

	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Boolean isActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSurname() {
		return surname;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getUri() {
		return uri;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getPhoneRegion() {
		return phoneRegion;
	}

	public void setPhoneRegion(String phoneRegion) {
		this.phoneRegion = phoneRegion;
	}

	public Boolean isRoot() {
		return isRoot;
	}

	public void setIsRoot(Boolean isRoot) {
		this.isRoot = isRoot;
	}
	
	public Boolean isQos() {
		return qos;
	}

	public void setQos(Boolean qos) {
		this.qos = qos;
	}


	protected AccountEntity getAccount() {
		return account;
	}

	protected void setAccount(AccountEntity account) {
		this.account = account;
	}

	protected AccountEntity getAdministeredAccount() {
		return administeredAccount;
	}

	protected void setAdministeredAccount(AccountEntity administeredAccount) {
		this.administeredAccount = administeredAccount;
	}

	protected Boolean getActive() {
		return active;
	}

	protected Map<Long, GroupEntity> getGroups() {
		return groups;
	}

	protected Map<Long, GroupEntity> getAdministeredGroups() {
		return administeredGroups;
	}

	protected Map<Long, ChannelEntity> getNotificationChannels() {
		return notificationChannels;
	}

	protected void setPicture(ContentEntity picture) {
		this.picture = picture;
	}

	protected ContentEntity getPicture() {
		return picture;
	}

	protected Map<Long, TimelineEntity> getTimelines() {
		return timelines;
	}

	protected Map<Long, UserEntity> getLocalContacts() {
		return localContacts;
	}

	// //////////////
	//
	// HELPERS
	//
	// //////////////

	protected void verifyRecord() {
		// Either mail or phone must be non null & non emtpy
		
		Boolean hasEmail = email != null && !email.isEmpty();
		Boolean hasPhone = phone != null && !phone.isEmpty()
				&& phoneRegion != null && !phoneRegion.isEmpty();
		Boolean hasPassword = password != null && !password.isEmpty();

		if ((!hasEmail && !hasPhone) || !hasPassword) {
			// Username can not be null or empty
			throw new KhcInvalidDataException("User credentials missing",
					Code.NO_CREDENTIALS);
		}

		// Verfy phone number is really a phone
		if (hasPhone) {
			PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
			PhoneNumber phoneNumber;
			try {
				phoneNumber = phoneUtil.parse(phone, phoneRegion);

			} catch (NumberParseException e) {
				throw new KhcInvalidDataException("Invalid phone format: "
						+ phone, Code.PHONE_FORMAT);
			}
			if (phoneUtil.isValidNumber(phoneNumber)) {
				phone = phoneUtil.format(phoneNumber, PhoneNumberFormat.E164);
			} else {
				throw new KhcInvalidDataException("Invalid phone:" + phone,
						Code.PHONE_FORMAT);
			}
		} else {
			// If declared as no phone, delete any value that can contaminate
			// database
			phone = null;
			phoneRegion = null;
		}
	}

	@Override
	public String toString() {
		String str = "|-- USER RECORD\n" + "|\tID:" + getId() + "\n"
				+ "|\tEmail: " + getEmail() + "\n" + "|\tPhone: " + getPhone() + "\n" + "|\tQoS: " + isQos();
		return str;
	}

}
