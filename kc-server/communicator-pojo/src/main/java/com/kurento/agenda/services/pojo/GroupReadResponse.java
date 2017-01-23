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

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class GroupReadResponse implements Serializable {

	private static final long serialVersionUID = 1L;

	// Attributes

	@XmlElement(required = true)
	private Long id;

	@XmlElement(required = true)
	private String name;

	@XmlElement
	private String phone;

	@XmlElement
	private String phoneRegion;

	@XmlElement
	private Long picture;

	@XmlElement(required = true)
	private Boolean canRead;

	@XmlElement(required = true)
	private Boolean canSend;

	@XmlElement(required = true)
	private Boolean canLeave;

	@XmlElement(required = true)
	private Boolean admin;

	// Getters & Setters

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	public Long getPicture() {
		return picture;
	}

	public void setPicture(Long picture) {
		this.picture = picture;
	}

	public Boolean getCanRead() {
		return canRead;
	}

	public void setCanRead(Boolean canRead) {
		this.canRead = canRead;
	}

	public Boolean getCanSend() {
		return canSend;
	}

	public void setCanSend(Boolean canSend) {
		this.canSend = canSend;
	}

	public Boolean getCanLeave() {
		return canLeave;
	}

	public void setCanLeave(Boolean canLeave) {
		this.canLeave = canLeave;
	}

	public Boolean isAdmin() {
		return admin;
	}

	public void setIsAdmin(Boolean admin) {
		this.admin = admin;
	}
}
