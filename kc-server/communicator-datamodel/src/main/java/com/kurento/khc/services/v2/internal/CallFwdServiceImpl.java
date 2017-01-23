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

package com.kurento.khc.services.v2.internal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.kurento.agenda.datamodel.pojo.CallFwd;
import com.kurento.khc.datamodel.CallFwdDao;
import com.kurento.khc.datamodel.CallFwdEntity;
import com.kurento.khc.datamodel.CallFwdEntity.State;
import com.kurento.khc.services.v2.AdministrativeService;
import com.kurento.khc.services.v2.CallFwdService;

@Service
public class CallFwdServiceImpl implements CallFwdService {

	@Autowired
	CallFwdDao callFwdDao;
	@Autowired
	private AdministrativeService admService;

	@Override
	public CallFwd buildCallFwdSetupPojo(CallFwdEntity fwdEntity) {
		return buildCallFwdPojo(fwdEntity, State.SETUP);
	}

	@Override
	public CallFwd buildCallFwdAckPojo(CallFwdEntity fwdEntity) {
		return buildCallFwdPojo(fwdEntity, State.ACK);
	}

	@Override
	public CallFwd buildCallFwdEstablishedPojo(CallFwdEntity fwdEntity) {
		return buildCallFwdPojo(fwdEntity, State.ESTABLISHED);
	}

	@Override
	public CallFwd buildCallFwdTerminatedPojo(CallFwdEntity fwdEntity) {
		return buildCallFwdPojo(fwdEntity, State.TERMINATED);
	}

	private CallFwd buildCallFwdPojo(CallFwdEntity fwdEntity, State expectedState) {
		Assert.notNull(fwdEntity);
		Assert.isTrue(fwdEntity.getState().compareTo(expectedState) == 0);
		CallFwd fwd = new CallFwd();
		fwd.setId(fwdEntity.getUUID());
		fwd.setLocalId(fwdEntity.getLocalId());
		fwd.setFailed(fwdEntity.getFailed());
		fwd.setDispatcher(admService.buildUserPojo(callFwdDao.getDispatchUser(fwdEntity)));
		fwd.setFrom(admService.buildUserPojo(callFwdDao.getFromUser(fwdEntity)));
		fwd.setTo(admService.buildUserPojo(callFwdDao.getToUser(fwdEntity)));
		return fwd;
	}
}
