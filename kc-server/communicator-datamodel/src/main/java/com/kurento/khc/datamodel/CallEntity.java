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
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Transient;

@Entity
@NamedQueries({
		@NamedQuery(name = CallEntity.NQ_NAME_DELETE_OLD_CALLS, query = ""
				+ "DELETE FROM CallEntity call WHERE call.state = :"
				+ CallEntity.NQ_PARAM_STATE + " AND  call.timestamp < :"
				+ CallEntity.NQ_PARAM_TIMESTAMP),
		@NamedQuery(name = CallEntity.NQ_NAME_DELETE_CHANNEL_CALLS, query = ""
				+ "DELETE FROM CallEntity call WHERE call.invoker = :"
				+ CallEntity.NQ_PARAM_CHANNEL + " OR call.receiver = :"
				+ CallEntity.NQ_PARAM_CHANNEL),
		@NamedQuery(name = CallEntity.NQ_NAME_GET_CHANNEL_CALLS, query = ""
				+ "SELECT call FROM CallEntity call WHERE call.state = :"
				+ CallEntity.NQ_PARAM_STATE + " AND ( call.invoker = :"
				+ CallEntity.NQ_PARAM_CHANNEL + " OR call.receiver = :"
				+ CallEntity.NQ_PARAM_CHANNEL + " )"),
		@NamedQuery(name = CallEntity.NQ_NAME_GET_CALLS, query = ""
				+ "SELECT call FROM CallEntity call WHERE call.state = :"
				+ CallEntity.NQ_PARAM_STATE + " AND ( call.from = :"
				+ CallEntity.NQ_PARAM_USER + " OR call.to = :"
				+ CallEntity.NQ_PARAM_USER + ")"),
		@NamedQuery(name = CallEntity.NQ_NAME_GET_CALLS_BY_RANGE_OF_DATES, query = ""
				+ "SELECT cll FROM CallEntity cll JOIN cll.from fr WHERE fr.account = :"
				+ UserEntity.NQ_PARAM_ACCOUNT
				+ " AND cll.timestamp >= :"
				+ CallEntity.NQ_PARAM_TIMESTAMP
				+ " AND cll.timestamp <= :"
				+ CallEntity.NQ_PARAM_END_TIMESTAMP_PLUS_TIME
				+ " ORDER BY cll.timestamp DESC"),
		@NamedQuery(name = CallEntity.NQ_NAME_UPDATE_RECEIVER_AND_INVOKER, query = ""
				+ "UPDATE CallEntity SET invoker=:"
				+ CallEntity.NQ_NEW_PARAM_CHANNEL
				+ ", receiver=:"
				+ CallEntity.NQ_NEW_PARAM_CHANNEL
				+ " WHERE invoker = :"
				+ CallEntity.NQ_PARAM_CHANNEL
				+ " OR receiver =:"
				+ CallEntity.NQ_PARAM_CHANNEL)

})
public class CallEntity extends BaseEntity {
	private static final long serialVersionUID = 1L;

	public static final String NQ_NAME_DELETE_OLD_CALLS = "CallEntity.deleteOldCalls";
	public static final String NQ_NAME_DELETE_CHANNEL_CALLS = "CallEntity.deleteChannelCalls";
	public static final String NQ_NAME_GET_CHANNEL_CALLS = "CallEntity.getChannelCalls";
	public static final String NQ_NAME_GET_CALLS = "CallEntity.getCalls";
	public static final String NQ_NAME_GET_CALLS_BY_RANGE_OF_DATES= "CallEntity.getCallsByRangeOfDates";
	public static final String NQ_NAME_UPDATE_RECEIVER_AND_INVOKER= "CallEntity.updateReceiverAndInvoker";

	public static final String NQ_PARAM_STATE = "state";
	public static final String NQ_PARAM_CHANNEL = "invoker";
	public static final String NQ_NEW_PARAM_CHANNEL = "newParamChannel";
	public static final String NQ_PARAM_USER = "user";
	public static final String NQ_PARAM_TIMESTAMP = "timestamp";
	public static final String NQ_PARAM_END_TIMESTAMP_PLUS_TIME = "endTimestampPlusTime";

	public enum State {
		RINGING, CONFIRMED, TERMINATED
	}

	private Long timestamp;

	private Long localId;

	private State state;

	@Lob
	@Column(length = 10000)
	private String offer;

	@Lob
	@Column(length = 10000)
	private String answer;

	@ManyToOne
	private ChannelEntity invoker;

	@ManyToOne
	private UserEntity from;

	@ManyToOne
	private ChannelEntity receiver;

	@ManyToOne
	private UserEntity to;

	@Column(columnDefinition = "BIT")
	private Boolean callFwd = false;

	private Long timeStampAccepted;

	private Long duration;

	@Transient
	private Boolean videoOff;

	@Transient
	private Boolean soundOff;

	// Methods

	public CallEntity() {
		this.timestamp = System.currentTimeMillis();
		this.duration = Long.valueOf(0);
	}

	public Long getLocalId() {
		return localId;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public void setLocalId(Long localId) {
		this.localId = localId;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public String getOffer() {
		return offer;
	}

	public void setOffer(String offer) {
		this.offer = offer;
	}

	public String getAnswer() {
		return answer;
	}

	public void setAnswer(String answer) {
		this.answer = answer;
	}

	protected UserEntity getFrom() {
		return from;
	}

	protected void setFrom(UserEntity from) {
		this.from = from;
	}

	protected UserEntity getTo() {
		return to;
	}

	protected void setTo(UserEntity to) {
		this.to = to;
	}

	protected ChannelEntity getInvoker() {
		return invoker;
	}

	protected void setInvoker(ChannelEntity invoker) {
		this.invoker = invoker;
	}

	protected ChannelEntity getReceiver() {
		return receiver;
	}

	protected void setReceiver(ChannelEntity receiver) {
		this.receiver = receiver;
	}

	public Boolean getCallFwd() {
		return callFwd;
	}

	public void setCallFwd(Boolean callFwd) {
		this.callFwd = callFwd;
	}

	public Long getDuration() {
		return duration;
	}

	public void setDuration(Long duration) {
		this.duration = duration;
	}

	public Long getTimeStampAccepted() {
		return timeStampAccepted;
	}

	public void setTimeStampAccepted(Long timeStampAccepted) {
		this.timeStampAccepted = timeStampAccepted;
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

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		if (localId != null)
			builder.append("localId=").append(localId).append(", ");
		if (state != null)
			builder.append("state=").append(state).append(", ");
		builder.append("invoker=").append(invoker).append(", ");
		if (from != null)
			builder.append("fromUser=").append(from.getId()).append(", ");
		builder.append("receiver=").append(receiver).append(", ");
		if (to != null)
			builder.append("toUser=").append(to.getId());
		builder.append("]");
		return builder.toString();
	}
}
