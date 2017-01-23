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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import com.kurento.agenda.datamodel.pojo.TimelineParty.PartyType;

@Component
public class TimelineSecureDao {

	@Autowired
	private TimelineDao timelineDao;
	@Autowired
	private UserDao userDao;

	@PreAuthorize("#owner.id == principal.user.id"
			+ " and @groupDao.isGroupMember(#party, #owner)")
	public TimelineEntity createTimeline(TimelineEntity timeline, UserEntity owner, GroupEntity party) {
		return timelineDao.createTimeline(timeline, owner, party);
	}

	@PreAuthorize("#owner.id == principal.user.id"
			+ " and @userDao.canSeeContact(#party, #owner)")
	public TimelineEntity createTimeline(TimelineEntity timeline,
			UserEntity owner, UserEntity party) {
		return timelineDao.createTimeline(timeline, owner, party);
	}

	@PreAuthorize("@timelineDao.isTimelineAdmin(#timeline, principal.user)")
	public TimelineEntity updateTimeline(TimelineEntity timeline) {
		return timelineDao.updateTimeline(timeline);
	}

	@PostAuthorize("@timelineDao.isTimelineAdmin(returnObject, principal.user)")
	public TimelineEntity findTimelineByUUID(Long uuid) {

		return timelineDao.findTimelineByUUID(uuid);
	}

	@PostAuthorize("@timelineDao.isTimelineAdmin(returnObject, principal.user)")
	public TimelineEntity findTimelineByOwner(UserEntity owner, Long partyUUID,
			PartyType type) {

		return timelineDao.findTimelineByOwner(owner, partyUUID, type);
	}

	@PreAuthorize("@timelineDao.isTimelineAdmin(#timeline, principal.user)")
	public List<MessageEntity> getTimelineMessages(TimelineEntity timeline,
			Integer maxMessage) {
		return timelineDao.getTimelineMessages(timeline, maxMessage);
	}

	@PreAuthorize("@timelineDao.isTimelineAdmin(#timeline, principal.user)")
	public List<MessageEntity> getTimelineMessages(TimelineEntity timeline,
			MessageEntity lastMessage, Integer maxMessage) {
		return timelineDao.getTimelineMessages(timeline, lastMessage,
				maxMessage);
	}

	@PreAuthorize("@timelineDao.isTimelineAdmin(#timeline, principal.user)")
	public MessageEntity findTimelineMessageByUUID(TimelineEntity timeline,
			Long messageId) {
		return timelineDao.findTimelineMessageByUUID(timeline, messageId);
	}

}
