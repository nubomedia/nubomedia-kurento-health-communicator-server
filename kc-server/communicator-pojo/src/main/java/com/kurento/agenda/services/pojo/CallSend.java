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

import com.kurento.agenda.datamodel.pojo.Call;
import com.kurento.agenda.datamodel.pojo.User;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class CallSend implements Serializable {

	private static final long serialVersionUID = 1L;

	@XmlElement(required = true)
	private Long localId;

	@XmlElement(required = true)
	private Long id;

	@XmlElement(required = true)
	private Long to;

	@XmlElement
	private String sdp;

	@XmlElement
	private Boolean videoOff;

	@XmlElement
	private Boolean soundOff;

	@XmlElement
	private Boolean callFwd;

	// Attributes

	public Long getLocalId() {
		return localId;
	}

	public void setLocalId(Long localId) {
		this.localId = localId;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getTo() {
		return to;
	}

	public void setTo(Long to) {
		this.to = to;
	}

	public String getSdp() {
		return sdp;
	}

	public void setSdp(String sdp) {
		this.sdp = sdp;
	}

	public Boolean getVideoOff() {
		return videoOff;
	}

	public void setVideoOff(Boolean videoOff) {
		this.videoOff = videoOff;
	}

	public Boolean getSoundOff() {
		return soundOff;
	}

	public void setSoundOff(Boolean soundOff) {
		this.soundOff = soundOff;
	}

	public Boolean getCallFwd() {
		return callFwd;
	}

	public void setCallFwd(Boolean callFwd) {
		this.callFwd = callFwd;
	}

	// /////////////////////////////////
	// Format converters
	// /////////////////////////////////

	public Call buildCallPojo() {
		Call call = new Call();
		call.setId(this.id);
		call.setLocalId(this.localId);
		User to = new User();
		to.setId(this.to);
		call.setTo(to);
		call.setCallFwd(this.callFwd);
		call.setSdp(this.sdp);
		call.setVideoOff(this.videoOff);
		call.setSoundOff(this.soundOff);
		return call;
	}

}
