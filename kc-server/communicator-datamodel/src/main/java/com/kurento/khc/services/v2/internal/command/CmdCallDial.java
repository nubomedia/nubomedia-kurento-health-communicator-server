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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.kurento.agenda.datamodel.pojo.Call;
import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.datamodel.pojo.Content;
import com.kurento.agenda.services.pojo.CallReceive;
import com.kurento.agenda.services.pojo.CallSend;
import com.kurento.khc.datamodel.CallEntity;
import com.kurento.khc.datamodel.ChannelEntity;
import com.kurento.khc.datamodel.CommandEntity;
import com.kurento.khc.datamodel.UserEntity;

@Component(Command.METHOD_CALL_DIAL)
public class CmdCallDial extends AbstractCommand {

	@Override
	@Transactional
	public void exec(Command command, Boolean asServer) {

		Call call = parseParam(command, CallSend.class).buildCallPojo();
		ChannelEntity invoker = asServer ? null : notchSecureDao
				.findNotificationChannelByUUID(command.getChannelId());
		UserEntity to = userDao.findUserByUUID(call.getTo().getId());

		CallEntity callEntity = new CallEntity();
		callEntity.setOffer(call.getSdp());
		callEntity.setLocalId(call.getLocalId());
		callEntity.setVideoOff(call.getVideoOff());
		callEntity.setSoundOff(call.getSoundOff());
		callEntity.setCallFwd(call.getCallFwd());

		if (asServer) {
			callDao.createCall(callEntity, invoker, to);
		} else {
			callSecureDao.createCall(callEntity, invoker, to);
		}

		if (!callDao.isCallForward(callEntity)) {
			// Add caller as local contact of callee if it is not already
			UserEntity from = notchDao.getUser(invoker);
			if (!userDao.hasLocalContact(to, from)) {
				userDao.addLocalContact(to, from);
				List<ChannelEntity> receivers = userDao.getNotificationChannels(to);
				sendUpdateContact(invoker, receivers, from);
			}
		}
		// Propagate dial command
		propagateCallDial(invoker, to, callEntity);
	}

	@Override
	public void exec(Command command, Content content, Boolean asServer) {
		exec(command, asServer);
	}

	@Override
	public Integer getConsolidationPriority() {
		return 10;
	}

	@Override
	public List<CommandEntity> consolide(List<CommandEntity> commands) {
		Map<Long, CommandEntity> consolided = new LinkedHashMap<Long, CommandEntity>();
		Long index = 0L;
		for (CommandEntity cmd : commands) {
			String method = cmd.getMethod();
			if (method.equals(Command.METHOD_CALL_DIAL)) {
				// Register call
				Long callId = parseParam(commandService.buildCommandPojo(cmd),
						CallReceive.class).getId();
				consolided.put(callId, cmd);
			} else if (method.equals(Command.METHOD_CALL_TERMINATE)) {
				// Unregister call if dialed in the same bunch of commands
				Long callId = parseParam(commandService.buildCommandPojo(cmd),
						CallReceive.class).getId();
				if (consolided.remove(callId) == null) {
					consolided.put(index++, cmd);
				}
			} else {
				// Add to consolided
				consolided.put(index++, cmd);
			}
		}
		return new ArrayList<CommandEntity>(consolided.values());
	}
}
