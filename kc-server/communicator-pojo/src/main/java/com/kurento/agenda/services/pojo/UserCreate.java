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

package com.kurento.agenda.services.pojo;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.kurento.agenda.datamodel.pojo.User;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class UserCreate implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final String QOS_GCM_DATA_KEY = "qos";

	@XmlElement
	private String email;

	@XmlElement
	private Boolean qos;

	@XmlElement
	private String password;

	@XmlElement
	private String name;

	@XmlElement
	private String surname;

	@XmlElement
	private String phone;

	@XmlElement(required = true)
	private String phoneRegion;

	// GETTERS & SETTERS


	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
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

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Boolean getQos() {
		return this.qos;
	}

	public void setQos(Boolean qos) {
		this.qos = qos;
	}

	// ///////////////////
	// Converters
	// ///////////////////

	public User buildUserPojo() {
		User user = new User();
		user.setEmail(this.email);
		user.setQos(this.qos);
		user.setPassword(this.getPassword());
		user.setName(this.getName());
		user.setSurname(this.getSurname());
		user.setPhone(this.getPhone());
		user.setPhoneRegion(this.phoneRegion);
		return user;
	}
}
