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

package com.kurento.agenda.datamodel.pojo;

import java.io.Serializable;

import com.kurento.agenda.services.pojo.UserCreateResponse;
import com.kurento.agenda.services.pojo.UserId;
import com.kurento.agenda.services.pojo.UserReadAvatarResponse;
import com.kurento.agenda.services.pojo.UserReadContactResponse;
import com.kurento.agenda.services.pojo.UserReadNameResponse;
import com.kurento.agenda.services.pojo.UserReadResponse;

public class User extends BasePojo implements Serializable {

	private static final long serialVersionUID = 1L;

	// Credentials
	private String email;
	private Boolean qos;
	private String password;
	private String name;
	private String surname;
	private Long picture;
	private String uri;
	private String phone;
	private String phoneRegion;
	private Boolean isRoot;


	// /////////////////
	//
	// Getters & Setters
	//
	// /////////////////

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

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Boolean getQos() {
		return qos;
	}

	public void setQos(Boolean qos) {
		this.qos = qos;
	}

	/**
	 * Provides the picture ID associated to this user or null if does not
	 * exists
	 *
	 * @return
	 */
	public Long getPicture() {
		return picture;
	}

	public void setPicture(Long picture) {
		this.picture = picture;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
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

	// ////////////////////////
	// Format Converters
	// ////////////////////////
	public UserReadNameResponse buildUserReadNameResponse() {
		UserReadNameResponse name = new UserReadNameResponse();
		name.setId(this.getId());
		name.setName(this.getName());
		name.setSurname(this.getSurname());
		return name;
	}

	public UserReadAvatarResponse buildUserReadAvatarResponse() {
		UserReadAvatarResponse avatar = new UserReadAvatarResponse();
		avatar.setId(this.getId());
		avatar.setName(this.getName());
		avatar.setSurname(this.getSurname());
		avatar.setPicture(this.getPicture());
		return avatar;
	}

	public UserCreateResponse buildUserCreateResponse() {
		UserCreateResponse userCreate = new UserCreateResponse();
		userCreate.setId(this.getId());

		return userCreate;
	}

	public UserReadContactResponse buildUserReadContactResponse() {

		UserReadContactResponse userContact = new UserReadContactResponse();
		userContact.setId(this.getId());
		userContact.setName(this.getName());
		userContact.setSurname(this.getSurname());
		userContact.setPhone(this.getPhone());
		userContact.setPhoneRegion(this.getPhoneRegion());
		userContact.setPicture(this.getPicture());
		return userContact;
	}

	public UserReadResponse buildUserReadResponse() {

		UserReadResponse userRead = new UserReadResponse();
		userRead.setId(this.getId());
		userRead.setEmail(this.getEmail());
		userRead.setName(this.getName());
		userRead.setSurname(this.getSurname());
		userRead.setPhone(this.getPhone());
		userRead.setPhoneRegion(this.getPhoneRegion());
		userRead.setPicture(this.getPicture());
		userRead.setQos(this.qos);
		return userRead;
	}

	public UserId buildUserId() {
		UserId userId = new UserId();
		userId.setId(this.getId());
		return userId;
	}

	@Override
	public String toString() {
		String str = "|-- USER RECORD\n" + "|\tName: " + getName() + "\n"
				+ "|\tEmail: " + getEmail() + "\n";
		return str;
	}

}
