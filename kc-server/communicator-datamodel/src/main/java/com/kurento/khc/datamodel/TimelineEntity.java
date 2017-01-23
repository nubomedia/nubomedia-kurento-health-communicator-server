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

/*
 * This class provides a view of a conversation for a given user (timeline's owner)
 */
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import com.kurento.agenda.datamodel.pojo.Timeline.State;
import com.kurento.agenda.datamodel.pojo.TimelineParty.PartyType;

@Entity
@NamedQueries({ @NamedQuery(name = TimelineEntity.NQ_NAME_FIND_TIMELINE_WITH_PARTY, query = ""
		+ "SELECT tl from TimelineEntity tl "
		+ " WHERE tl.partyUuid = :"
		+ TimelineEntity.NQ_PARAM_TIMELINE_PARTY_UUID
		+ " AND tl.partyType = :"
		+ TimelineEntity.NQ_PARAM_TIMELINE_PARTY_TYPE) })
public class TimelineEntity extends BaseEntity {
	private static final long serialVersionUID = 1L;

	public static final String NQ_NAME_FIND_TIMELINE_WITH_PARTY = "TimelineEntity.FindTimelineWithParty";
	public static final String NQ_NAME_FIND_TIMELINE_MESSAGES = "TimelineEntity.FindTimelineMessages";
	public static final String NQ_PARAM_TIMELINE_ID = "timelineId";
	public static final String NQ_PARAM_TIMELINE_PARTY_UUID = "partyUuid";
	public static final String NQ_PARAM_TIMELINE_PARTY_TYPE = "partyType";

	// Attributes

	@ManyToOne(fetch = FetchType.LAZY)
	private UserEntity user;

	private Long localId;

	private Long partyUuid;

	private PartyType partyType;

	private State state = State.HIDDEN;

	@ManyToOne(fetch = FetchType.LAZY)
	private ConversationEntity conversation;

	// Getters & setters

	public Long getLocalId() {
		return localId;
	}

	public void setLocalId(Long localId) {
		this.localId = localId;
	}

	public Long getPartyUUID() {
		return partyUuid;
	}

	public void setPartyUUID(Long partyUuid) {
		this.partyUuid = partyUuid;
	}

	public PartyType getPartyType() {
		return partyType;
	}

	public void setPartyType(PartyType partyType) {
		this.partyType = partyType;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	protected UserEntity getUser() {
		return user;
	}

	protected void setUser(UserEntity user) {
		this.user = user;
	}

	protected ConversationEntity getConversation() {
		return conversation;
	}

	protected void setConversation(ConversationEntity conversation) {
		this.conversation = conversation;
	}

}
