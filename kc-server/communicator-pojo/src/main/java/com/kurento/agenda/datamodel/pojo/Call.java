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

import com.kurento.agenda.services.pojo.CallReceive;

public class Call extends BasePojo {

	private static final long serialVersionUID = 1L;

	private Long localId;

	private User from;

	private User to;

	private String sdp;

	private Boolean callFwd;

	private Long timestamp;

	private Long timestampAccepted;

	private Long duration;

	private Boolean videoOff;

	private Boolean soundOff;

	// Getters & Setters

	public Long getLocalId() {
		return localId;
	}

	public void setLocalId(Long localId) {
		this.localId = localId;
	}

	public String getSdp() {
		return sdp;
	}

	public void setSdp(String sdp) {
		this.sdp = sdp;
	}

	public Boolean getCallFwd() {
		return callFwd;
	}

	public void setCallFwd(Boolean callFwd) {
		this.callFwd = callFwd;
	}

	public User getFrom() {
		return from;
	}

	public void setFrom(User from) {
		this.from = from;
	}

	public User getTo() {
		return to;
	}

	public void setTo(User to) {
		this.to = to;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Long timeStamp) {
		this.timestamp = timeStamp;
	}

	public Long getDuration() {
		return duration;
	}

	public void setDuration(Long duration) {
		this.duration = duration;
	}

	public Long getTimestampAccepted() {
		return timestampAccepted;
	}

	public void setTimestampAccepted(Long timestampAccepted) {
		this.timestampAccepted = timestampAccepted;
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

	// /////////////////////////////
	// Format converters
	// /////////////////////////////

	// public CallSend buildCallSendPojo() {
	// CallSend callSend = new CallSend();
	// callSend.setId(this.id);
	// callSend.setLocalId(this.getLocalId());
	// callSend.setTo(this.getTo().getId());
	// callSend.setSdp(this.sdp);
	// callSend.setVideoOff(this.videoOff);
	// callSend.setSoundOff(this.soundOff);
	// return callSend;
	// }

	public CallReceive buildCallReceivePojo() {
		CallReceive callReceive = new CallReceive();
		callReceive.setId(this.id);
		callReceive.setLocalId(this.getLocalId());
		callReceive.setFrom(this.getFrom().buildUserReadAvatarResponse());
		callReceive.setTo(this.getTo().getId());
		callReceive.setSdp(this.sdp);
		callReceive.setVideoOff(this.videoOff);
		callReceive.setSoundOff(this.soundOff);
		callReceive.setCallFwd(this.callFwd);
		return callReceive;
	}

}
