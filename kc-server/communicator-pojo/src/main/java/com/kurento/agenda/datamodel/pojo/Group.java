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

import com.kurento.agenda.services.pojo.GroupCreate;
import com.kurento.agenda.services.pojo.GroupInfo;
import com.kurento.agenda.services.pojo.GroupReadResponse;
import com.kurento.agenda.services.pojo.GroupUpdate;

public class Group extends BasePojo {

	private static final long serialVersionUID = 1L;

	// Attributes

	private Long localId;

	private String name;

	private String phone;

	private String phoneRegion;

	private Long picture;

	// Getters & Setters

	public Long getLocalId() {
		return localId;
	}

	public void setLocalId(Long localId) {
		this.localId = localId;
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

	// //////////////////////////////////////
	// Format converters
	// //////////////////////////////////////

	public GroupCreate buildGroupCreate() {
		GroupCreate groupCreate = new GroupCreate();
		groupCreate.setLocalId(this.getLocalId());
		groupCreate.setName(this.getName());
		groupCreate.setPhone(this.getPhone());
		groupCreate.setPhoneRegion(this.getPhoneRegion());
		return groupCreate;
	}

	public GroupUpdate buildGroupUpdate() {
		GroupUpdate groupUpdate = new GroupUpdate();
		groupUpdate.setId(this.getId());
		groupUpdate.setLocalId(this.getLocalId());
		groupUpdate.setName(this.getName());
		groupUpdate.setPhone(this.getPhone());
		groupUpdate.setPhoneRegion(this.getPhoneRegion());
		groupUpdate.setPicture(this.getPicture());
		return groupUpdate;
	}

	public GroupReadResponse buildGroupReadResponse() {
		GroupReadResponse groupRead = new GroupReadResponse();
		groupRead.setId(this.getId());
		groupRead.setName(this.getName());
		groupRead.setPhone(this.getPhone());
		groupRead.setPhoneRegion(this.getPhoneRegion());
		groupRead.setPicture(this.getPicture());
		return groupRead;
	}

	public GroupInfo buildGroupInfo() {
		GroupInfo groupInfo = new GroupInfo();
		groupInfo.setId(this.id);
		groupInfo.setName(this.name);
		groupInfo.setPhone(this.getPhone());
		groupInfo.setPhoneRegion(this.getPhoneRegion());
		groupInfo.setPicture(this.picture);
		return groupInfo;
	}

}
