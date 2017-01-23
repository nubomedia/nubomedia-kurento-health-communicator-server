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
public class CallFwdSecureDao {

	@Autowired
	private CallFwdDao callFwdDao;

	@PreAuthorize("@userDao.canSeeContact(#from, principal.user) and @userDao.canSeeContact(#to, principal.user)")
	public CallFwdEntity createCallFwd(CallFwdEntity fwd, ChannelEntity invoker,
			UserEntity from, UserEntity to) {
		return callFwdDao.createCallFwd(fwd, invoker, from, to);
	}

	@PreAuthorize("@callFwdDao.isCallee(#fwd, principal.user)")
	public void ackCallFwd(CallFwdEntity fwd, ChannelEntity callee) {
		callFwdDao.ackCallFwd(fwd, callee);
	}

	@PreAuthorize("@callFwdDao.isCallee(#fwd, principal.user)")
	public void establishCallFwd(CallFwdEntity fwd, ChannelEntity callee) {
		callFwdDao.establishCallFwd(fwd, callee);
	}

	@PreAuthorize("@callFwdDao.isMember(#fwd, principal.user)")
	public void terminateCallFwd(CallFwdEntity fwd) {
		callFwdDao.terminateCallFwd(fwd);
	}
}
