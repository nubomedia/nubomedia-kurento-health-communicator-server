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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class CallSecureDao {

	@Autowired
	private CallDao callDao;

	@PreAuthorize("@userDao.canSeeContact(#to, principal.user) or @callDao.isCallForward(#call)")
	public CallEntity createCall(CallEntity call, ChannelEntity invoker,
			UserEntity to) {
		return callDao.createCall(call, invoker, to);
	}

	@PreAuthorize("@callDao.isCallReceiver(#call, principal.user)")
	public void acceptCall(CallEntity call, ChannelEntity receiver) {
		callDao.acceptCall(call, receiver);
	}

	@PreAuthorize("@callDao.isCallOwner(#call, principal.user)")
	public void terminateCall(CallEntity call) {
		callDao.terminateCall(call);
	}

	@PreAuthorize("@callDao.isCallOwner(#call, principal.user)")
	public void muteCall(CallEntity call) {
		callDao.muteCall(call);
	}
}
