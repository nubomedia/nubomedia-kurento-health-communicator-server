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

import com.kurento.agenda.datamodel.pojo.Channel;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ChannelCreate implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// PROPERTIES
	// Do not annoate with @XmlElement as this is not a parameter
	private Long id;

	@XmlElement(required = true)
	private Long userId;

	@XmlElement(required = true)
	private String instanceId;

	@XmlElement(required = true)
	private String registerId;

	@XmlElement(required = true)
	private String registerType;

	@XmlElement(required = true)
	private String locale;

	// GETTERS & SETTERS

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

	public Channel buildNoticicationChannelPojo() {
		Channel notch = null;
		notch = new Channel();
		notch.setRegisterId(this.getRegisterId());
		notch.setRegisterType(this.getRegisterType());
		notch.setUserId(this.getUserId());
		notch.setInstanceId(this.getInstanceId());
		notch.setLocale(this.getLocale());
		return notch;
	}

}
