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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;

@Entity
public class ChannelEntity extends BaseEntity {
	private static final long serialVersionUID = 1L;

	public static final String NQ_NAME_GET_CHANNEL_WITH_PENDING_COMMANDS = "ChannelEntity.GetChannelWithPendingCommands";

	@Column(nullable = false)
	private String instanceId;

	private String registerId;

	@Column(nullable = false)
	private String registerType;

	@Column(nullable = false, columnDefinition = "BIT")
	private Boolean enabled = true;

	private Integer badge = 0;

	private String locale;

	/*
	 * First sequence must be one as on channel init the lastSequence sent is 0.
	 * Command with sequence 0 is implictly assigned to NOP
	 */
	@Column(nullable = false)
	private Long lastSequenceIssued = 1L;

	@Column(nullable = false)
	private Long lastSequenceExec = 0L;

	/*
	 * User owing this Notification channel. This side is owner because notch
	 * are created/deleted from user
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	private UserEntity user;

	// GETTERS & SETTERS

	public String getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

	public String getRegisterType() {
		return registerType;
	}

	public void setRegisterType(String registerType) {
		this.registerType = registerType;
	}

	public String getRegisterId() {
		return registerId;
	}

	public Boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public void setRegisterId(String registerId) {
		this.registerId = registerId;
	}

	public Integer getBadge() {
		return badge;
	}

	public void setBadge(Integer badge) {
		this.badge = badge;
	}

	public void incBadge() {
		badge++;
	}

	public Long getLastSequenceExec() {
		return lastSequenceExec;
	}

	public String getLocaleString() {
		return locale;
	}

	public void setLocaleString(String locale) {
		this.locale = locale;
	}

	public void setLastSequenceExec(Long lastSequence) {
		this.lastSequenceExec = lastSequence;
	}

	synchronized public Long getNextSequence() {
		return lastSequenceIssued++;
	}

	protected UserEntity getUser() {
		return user;
	}

	protected void setUser(UserEntity user) {
		this.user = user;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		if (uuid != null)
			builder.append("uuid=").append(uuid).append(", ");
		if (registerType != null)
			builder.append("registerType=").append(registerType).append(", ");
		if (registerId != null)
			builder.append("registerId=").append(registerId).append(", ");
		if (instanceId != null)
			builder.append("instanceId=").append(instanceId).append(", ");
		if (locale != null)
			builder.append("locale=").append(locale).append(", ");
		if (badge != null)
			builder.append("badge=").append(badge).append(", ");
		if (enabled != null)
			builder.append("enabled=").append(enabled);
		builder.append("]");
		return builder.toString();
	}

}