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

import com.kurento.agenda.services.pojo.CallFwdRecv;


public class CallFwd extends BasePojo {

	private static final long serialVersionUID = 1L;

	private Long localId;

	private User dispatcher;

	private User from;

	private User to;

	private Boolean failed;

	public Long getLocalId() {
		return localId;
	}

	public void setLocalId(Long localId) {
		this.localId = localId;
	}

	public User getDispatcher() {
		return dispatcher;
	}

	public void setDispatcher(User dispatcher) {
		this.dispatcher = dispatcher;
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

	public Boolean getFailed() {
		return failed;
	}

	public void setFailed(Boolean failed) {
		this.failed = failed;
	}

	// /////////////////////////////
	// Format converters
	// /////////////////////////////

	public CallFwdRecv buildCallFwdRecvPojo() {
		CallFwdRecv recv = new CallFwdRecv();
		recv.setId(id);
		recv.setLocalId(localId);
		recv.setDispatcher(dispatcher.getId());
		recv.setFrom(from.getId());
		recv.setTo(to.getId());
		recv.setFailed(failed);
		return recv;
	}
}
