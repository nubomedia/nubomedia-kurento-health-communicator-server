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

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.datamodel.pojo.Content;
import com.kurento.agenda.datamodel.pojo.Timeline;
import com.kurento.agenda.datamodel.pojo.Timeline.State;
import com.kurento.agenda.services.pojo.TimelineCreate;
import com.kurento.khc.datamodel.ChannelEntity;
import com.kurento.khc.datamodel.TimelineEntity;
import com.kurento.khc.datamodel.UserEntity;

@Component(Command.METHOD_DELETE_TIMELINE)
public class CmdDeleteTimeline extends AbstractCommand {

	@Override
	public void exec(Command command, Boolean asServer) {
		// Get params
		Timeline timelineParam = parseParam(command, TimelineCreate.class)
				.buildTimelinePojo();

		ChannelEntity invoker = asServer ? null : getInvoker(command
				.getChannelId());

		// Execute command
		// Find entities
		UserEntity owner = userDao.findUserByUUID(timelineParam.getOwnerId());
		TimelineEntity timeline = userDao.getUserTimelineWithParty(owner,
				timelineParam.getParty().getId());
		// This command deletes timeline from user's view, but timeline remains
		// untouched. Update timeline state to IDLE
		timeline.setState(State.HIDDEN);
		timelineDao.updateTimeline(timeline);
		if (asServer) {
			timelineDao.updateTimeline(timeline);
		} else {
			timelineSecureDao.updateTimeline(timeline);
		}
		sendUpdateTimeline(invoker, timeline);
	}

	@Override
	public void exec(Command command, Content content, Boolean asServer) {
		exec(command, asServer);
	}

}
