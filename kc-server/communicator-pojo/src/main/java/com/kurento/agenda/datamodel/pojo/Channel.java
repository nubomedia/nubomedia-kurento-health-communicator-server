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

import com.kurento.agenda.services.pojo.ChannelCreateResponse;


public class Channel extends BasePojo {

	private static final long serialVersionUID = 1L;

	// CONSTANTS
	public static final String GCM = "gcm";
	public static final String APNS = "apns";
	public static final String WEB_POLL = "webPoll";
	public static final String WS = "ws";
	public static final String DEV_NULL = "devNull";

	// Attributes
	private Long id;

	private Long userId;

	private String instanceId;

	private String registerId;

	private String registerType;

	private String locale;

	// Getters & Setters

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

	public String getRegisterId() {
		return registerId;
	}

	public void setRegisterId(String registerId) {
		this.registerId = registerId;
	}

	public String getRegisterType() {
		return registerType;
	}

	public void setRegisterType(String registerType) {
		this.registerType = registerType;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	// ///////// FORMAT CONVERTERS //////////

	public ChannelCreateResponse buildNotificationChannelCreateResponse() {

		ChannelCreateResponse notchResponse = null;
		notchResponse = new ChannelCreateResponse();
		notchResponse.setChannelId(this.getId());
		return notchResponse;
	}

}
