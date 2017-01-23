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

package com.kurento.khc.services.v2.internal.command;

import org.springframework.stereotype.Component;

import com.kurento.agenda.datamodel.pojo.Call;
import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.datamodel.pojo.Content;
import com.kurento.agenda.services.pojo.CallSend;
import com.kurento.khc.KhcNotFoundException;
import com.kurento.khc.datamodel.CallEntity;
import com.kurento.khc.datamodel.ChannelEntity;
import com.kurento.khc.datamodel.UserEntity;

@Component(Command.METHOD_CALL_MUTE)
public class CmdCallMute extends AbstractCommand {

	@Override
	public void exec(Command command, Boolean asServer) {
		Call call = parseParam(command, CallSend.class).buildCallPojo();
		ChannelEntity invoker = asServer ? null : notchSecureDao
				.findNotificationChannelByUUID(command.getChannelId());
		CallEntity callEntity;
		try {
			callEntity = callDao.findCallByUUID(call.getId());
		} catch (KhcNotFoundException e) {
			callEntity = callDao.findCallByLocalId(call.getLocalId());
		}
		UserEntity to = userDao.findUserByUUID(call.getTo().getId());

		if (asServer) {
			callDao.muteCall(callEntity);
		} else {
			callSecureDao.muteCall(callEntity);
		}
		callEntity.setVideoOff(call.getVideoOff());
		callEntity.setSoundOff(call.getSoundOff());
		callEntity.setCallFwd(call.getCallFwd());

		propagateMuteCall(invoker, callEntity, to);
	}

	@Override
	public void exec(Command command, Content content, Boolean asServer) {
		exec(command, asServer);
	}
}
