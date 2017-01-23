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
import org.springframework.transaction.annotation.Transactional;

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.datamodel.pojo.Content;
import com.kurento.agenda.datamodel.pojo.KhcInvalidDataInfo.Code;
import com.kurento.agenda.datamodel.pojo.Timeline;
import com.kurento.agenda.datamodel.pojo.TimelineParty.PartyType;
import com.kurento.agenda.services.pojo.TimelineCreate;
import com.kurento.khc.KhcInvalidDataException;
import com.kurento.khc.datamodel.ChannelEntity;
import com.kurento.khc.datamodel.GroupEntity;
import com.kurento.khc.datamodel.TimelineEntity;
import com.kurento.khc.datamodel.UserEntity;

@Component(Command.METHOD_CREATE_TIMELINE)
public class CmdCreateTimeline extends AbstractCommand {

	@Override
	public void exec(Command command, Boolean asServer) {
		// Get params
		Timeline timelineParam = parseParam(command, TimelineCreate.class)
				.buildTimelinePojo();
		ChannelEntity invoker = asServer ? null : getInvoker(command
				.getChannelId());

		// Execute command
		UserEntity owner = userDao.findUserByUUID(timelineParam.getOwnerId());
		TimelineEntity timeline = new TimelineEntity();
		timeline.setLocalId(timelineParam.getId());
		Long partyId = timelineParam.getParty().getId();
		PartyType partyType = timelineParam.getParty().getType();
		if (asServer) {
			if (partyType.equals(PartyType.GROUP)) {
				GroupEntity party = groupDao.findGroupByUUID(partyId);
				timeline = timelineDao.createTimeline(timeline, owner, party);
			} else if (partyType.equals(PartyType.USER)) {
				UserEntity party = userDao.findUserByUUID(partyId);
				timeline = timelineDao.createTimeline(timeline, owner, party);
			} else {
				throw new KhcInvalidDataException(
						"Unknown party type when creating timeline: "
								+ partyType, Code.INVALID_DATA);
			}
		} else {
			if (partyType.equals(PartyType.GROUP)) {
				GroupEntity party = groupDao.findGroupByUUID(partyId);
				timeline = timelineSecureDao.createTimeline(timeline, owner,
						party);
			} else if (partyType.equals(PartyType.USER)) {
				UserEntity party = userDao.findUserByUUID(partyId);
				timeline = timelineSecureDao.createTimeline(timeline, owner,
						party);
			} else {
				throw new KhcInvalidDataException(
						"Unknown party type when creating timeline: "
								+ partyType, Code.INVALID_DATA);
			}
		}
		propagateTimelineActivation(invoker, timeline);
	}

	@Override
	@Transactional
	public void exec(Command command, Content content, Boolean asServer) {
		exec(command, asServer);
	}
}
