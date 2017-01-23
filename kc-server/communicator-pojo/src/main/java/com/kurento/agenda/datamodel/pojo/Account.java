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

import com.kurento.agenda.services.pojo.AccountReadInfoResponse;

public class Account extends BasePojo {

	private static final long serialVersionUID = 1L;

	// Attributes. POJO does not contain relationships

	private String name;

	private Boolean userAutoregister;

	private Boolean groupAutoregister;

	private Boolean pub;

	private Boolean active;
	
	private Long picture;

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
		return this.groupAutoregister;
	}

	public void setGroupAutoregister(Boolean groupAutoregister) {
		this.groupAutoregister = groupAutoregister;
	}

	public Boolean isPub() {
		return this.pub;
	}

	public void setPub(Boolean pub) {
		this.pub = pub;
	}

	public Boolean isActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public Long getPicture() {
		return picture;
	}

	public void setPicture(Long picture) {
		this.picture = picture;
	}

	// ///////// FORMAT CONVERTERS ///////////

	public AccountReadInfoResponse buildAccountReadInfo() {
		AccountReadInfoResponse accountReadInfo = null;
		accountReadInfo = new AccountReadInfoResponse();
		accountReadInfo.setId(this.getId());
		accountReadInfo.setName(this.getName());
		accountReadInfo.setServerTime(System.currentTimeMillis());
		accountReadInfo.setUserAutoregister(this.isUserAutoregister());
		accountReadInfo.setGroupAutoregister(this.isGroupAutoregister());

		return accountReadInfo;
	}
}
