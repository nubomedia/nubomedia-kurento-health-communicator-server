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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.kurento.agenda.datamodel.pojo.Call;
import com.kurento.khc.datamodel.CallDao;
import com.kurento.khc.datamodel.CallEntity;
import com.kurento.khc.datamodel.UserEntity;
import com.kurento.khc.services.v2.AdministrativeService;
import com.kurento.khc.services.v2.CallService;

@Service
public class CallServiceImpl implements CallService {

	@Autowired
	private CallDao callDao;
	@Autowired
	private AdministrativeService admService;

	@Override
	@Transactional
	public Call buildCallOfferPojo(CallEntity callEntity) {
		Call call = buildCallPojo(callEntity);
		call.setSdp(callEntity.getOffer());
		return call;
	}

	@Override
	@Transactional
	public Call buildCallOfferPojo(CallEntity callEntity, boolean includeSdp) {
		if (includeSdp)
			return buildCallOfferPojo(callEntity);
		return buildCallPojo(callEntity);
	}

	@Override
	@Transactional
	public Call buildCallAnswerPojo(CallEntity callEntity) {
		Call call = buildCallPojo(callEntity);
		call.setSdp(callEntity.getAnswer());
		return call;
	}

	@Override
	@Transactional
	public Call buildCallTerminatePojo(CallEntity callEntity) {
		return buildCallPojo(callEntity);
	}

	private Call buildCallPojo(CallEntity callEntity) {
		Assert.notNull(callEntity);
		Call call = new Call();
		UserEntity from = callDao.getFromUser(callEntity);
		UserEntity to = callDao.getToUser(callEntity);
		call.setFrom(admService.buildUserPojo(from));
		call.setTo(admService.buildUserPojo(to));
		call.setLocalId(callEntity.getLocalId());
		call.setId(callEntity.getUUID());
		call.setDuration(Long.valueOf(callEntity.getDuration()));
		call.setTimestamp(callEntity.getTimestamp());
		call.setTimestampAccepted(callEntity.getTimeStampAccepted());
		call.setVideoOff(callEntity.getVideoOff());
		call.setSoundOff(callEntity.getSoundOff());
		call.setCallFwd(callEntity.getCallFwd());
		return call;
	}

}
