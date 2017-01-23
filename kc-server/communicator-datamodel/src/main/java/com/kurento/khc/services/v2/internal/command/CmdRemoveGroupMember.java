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

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.datamodel.pojo.Content;
import com.kurento.agenda.datamodel.pojo.Group;
import com.kurento.agenda.datamodel.pojo.Timeline.State;
import com.kurento.agenda.datamodel.pojo.User;
import com.kurento.agenda.services.pojo.GroupInfo;
import com.kurento.agenda.services.pojo.UserReadAvatarResponse;
import com.kurento.khc.datamodel.ChannelEntity;
import com.kurento.khc.datamodel.GroupEntity;
import com.kurento.khc.datamodel.TimelineEntity;
import com.kurento.khc.datamodel.UserEntity;

@Component(Command.METHOD_REMOVE_GROUP_MEMBER)
public class CmdRemoveGroupMember extends AbstractCommand {

	@Override
	@Transactional
	public void exec(Command command, Boolean asServer) {
		// Get params
		User userParam = parseParam(command, Command.PARAM_USER,
				UserReadAvatarResponse.class).buildUserPojo();
		Group groupParam = parseParam(command, Command.PARAM_GROUP,
				GroupInfo.class).buildGroupPojo();
		ChannelEntity invoker = asServer ? null : getInvoker(command
				.getChannelId());

		GroupEntity group = groupDao.findGroupByUUID(groupParam.getId());
		UserEntity user = userDao.findUserByUUID(userParam.getId());

		// Propagate timeline deletion before actual deletion
		TimelineEntity timeline = userDao.getUserTimelineWithParty(user,
				group.getUUID());
		if (timeline.getState().equals(State.HIDDEN) || invoker != null
				&& notchDao.isOwner(invoker, user)) {
			sendDeleteTimeline(invoker, timeline);
		} else {
			timeline.setState(State.DISABLED);
			timelineDao.updateTimeline(timeline);
			sendUpdateTimeline(invoker, timeline);
		}

		// Get list of watchers before user is removed from group
		List<UserEntity> watchers = getUserWatchersOfContactInGroup(user, group);

		if (asServer) {
			groupDao.removeGroupMember(group, user);
		} else {
			groupSecureDao.removeGroupMember(group, user);
		}

		// Propagate group removal
		propagateRemoveGroupMember(invoker, group, user, watchers);
	}

	@Override
	@Transactional
	public void exec(Command command, Content content, Boolean asServer) {
		exec(command, asServer);
	}

}
