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
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@Entity
@NamedQueries({
	@NamedQuery(name = CallFwdEntity.NQ_NAME_DELETE_OLD_CALL_FWDS, query = ""
			+ "DELETE FROM CallFwdEntity fwd WHERE fwd.state = :"
			+ CallFwdEntity.NQ_PARAM_STATE
			+ " AND fwd.timestamp < :"
			+ CallFwdEntity.NQ_PARAM_TIMESTAMP),
		@NamedQuery(name = CallFwdEntity.NQ_NAME_UPDATE_INVOKER, query = ""
				+ "UPDATE CallFwdEntity SET invoker=:"
				+ CallFwdEntity.NQ_PARAM_NEW_CHANNEL_VALUE
				+ " WHERE invoker = :" + CallFwdEntity.NQ_PARAM_CHANNEL),
		@NamedQuery(name = CallFwdEntity.NQ_NAME_UPDATE_CALLER, query = ""
				+ "UPDATE CallFwdEntity SET caller=:"
				+ CallFwdEntity.NQ_PARAM_NEW_CHANNEL_VALUE
				+ " WHERE caller = :" + CallFwdEntity.NQ_PARAM_CHANNEL),
		@NamedQuery(name = CallFwdEntity.NQ_NAME_UPDATE_CALLEE, query = ""
				+ "UPDATE CallFwdEntity SET callee=:"
				+ CallFwdEntity.NQ_PARAM_NEW_CHANNEL_VALUE
				+ " WHERE callee = :" + CallFwdEntity.NQ_PARAM_CHANNEL) })
public class CallFwdEntity extends BaseEntity {
	private static final long serialVersionUID = 1L;

	public static final String NQ_NAME_DELETE_OLD_CALL_FWDS = "CallFwdEntity.deleteOldRecords";
	public static final String NQ_NAME_UPDATE_INVOKER = "CallFwdEntity.updateInvoker";
	public static final String NQ_NAME_UPDATE_CALLER = "CallFwdEntity.updateCaller";
	public static final String NQ_NAME_UPDATE_CALLEE = "CallFwdEntity.updateCallee";

	public static final String NQ_PARAM_STATE = "fwdState";
	public static final String NQ_PARAM_TIMESTAMP = "fwdTimestamp";
	public static final String NQ_PARAM_NEW_CHANNEL_VALUE = "newChannelValue";
	public static final String NQ_PARAM_CHANNEL = "channel";

	public enum State {
		SETUP, ACK, ESTABLISHED, TERMINATED
	}

	private Long localId;

	private Long timestamp;

	private Long timeStampAck;

	private Long timeStampAccepted;

	private State state;

	@Column(nullable = false, columnDefinition = "BIT")
	private Boolean failed = false;

	@ManyToOne
	private ChannelEntity invoker;

	@ManyToOne
	private ChannelEntity caller;

	@ManyToOne
	private ChannelEntity callee;

	@ManyToOne
	private UserEntity dispatch;

	@ManyToOne
	private UserEntity from;

	@ManyToOne
	private UserEntity to;

	// Methods

	public CallFwdEntity() {
		this.timestamp = System.currentTimeMillis();
	}

	public Long getLocalId() {
		return localId;
	}

	public void setLocalId(Long localId) {
		this.localId = localId;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public Long getTimeStampAck() {
		return timeStampAck;
	}

	public void setTimeStampAck(Long timeStampAck) {
		this.timeStampAck = timeStampAck;
	}

	public Long getTimeStampAccepted() {
		return timeStampAccepted;
	}

	public void setTimeStampAccepted(Long timeStampAccepted) {
		this.timeStampAccepted = timeStampAccepted;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public Boolean getFailed() {
		return failed;
	}

	public void setFailed(Boolean failed) {
		this.failed = failed;
	}

	protected ChannelEntity getInvoker() {
		return invoker;
	}

	protected void setInvoker(ChannelEntity invoker) {
		this.invoker = invoker;
	}

	protected ChannelEntity getCaller() {
		return caller;
	}

	protected void setCaller(ChannelEntity caller) {
		this.caller = caller;
	}

	protected ChannelEntity getCallee() {
		return callee;
	}

	protected void setCallee(ChannelEntity callee) {
		this.callee = callee;
	}

	protected UserEntity getDispatch() {
		return dispatch;
	}

	protected void setDispatch(UserEntity dispatch) {
		this.dispatch = dispatch;
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
}
