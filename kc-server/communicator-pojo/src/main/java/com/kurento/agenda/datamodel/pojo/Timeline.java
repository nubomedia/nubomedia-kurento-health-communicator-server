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

package com.kurento.agenda.datamodel.pojo;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

import com.kurento.agenda.services.pojo.TimelineCreate;
import com.kurento.agenda.services.pojo.TimelineCreateResponse;
import com.kurento.agenda.services.pojo.TimelinePartyUpdate;
import com.kurento.agenda.services.pojo.TimelineReadResponse;

public class Timeline extends BasePojo {

	private static final long serialVersionUID = 1L;

	public static final String PARAM_LAST_MESSAGE = "lastMessage";
	public static final String PARAM_MAX_MESSAGE = "maxMessage";

	@XmlType
	@XmlEnum(String.class)
	public enum State {
		ENABLED("enabled"), DISABLED("disabled"), HIDDEN("hidden");

		private String value;

		private State(String value) {
			this.value = value;
		}

		public String getValue() {
			return this.value;
		}

		public static State create(String val) {
			for (State state : State.values()) {
				if (state.getValue().equalsIgnoreCase(val)) {
					return state;
				}
			}
			return ENABLED;
		}
	};

	// Attributes
	
	private Long localId;

	private Long ownerId;

	private State state;

	private TimelineParty party;

	// Getters & Setters

	public Long getLocalId() {
		return localId;
	}

	public void setLocalId(Long localId) {
		this.localId = localId;
	}

	public Long getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(Long ownerId) {
		this.ownerId = ownerId;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public TimelineParty getParty() {
		return party;
	}

	public void setParty(TimelineParty party) {
		this.party = party;
	}

	// //////////////////////////////////////////
	// Format converters
	// //////////////////////////////////////////

	public TimelineCreate buildTimelineCreate() {
		TimelineCreate timelineCreate = new TimelineCreate();
		timelineCreate.setLocalId(this.localId);
		timelineCreate.setOwnerId(this.ownerId);
		if (this.party != null) {
			timelineCreate.setParty(this.party.buildTimelinePartyUpdate());
		}
		return timelineCreate;
	}

	public TimelineReadResponse buildTimelineReadResponse() {
		TimelineReadResponse timelineRead = new TimelineReadResponse();

		timelineRead.setId(this.id);
		timelineRead.setLocalId(this.localId);
		timelineRead.setState(this.state);
		timelineRead.setOwnerId(this.ownerId);

		if (this.party != null) {
			timelineRead.setParty(this.party.buildTimelinePartyUpdate());
		}
		return timelineRead;
	}

	public TimelinePartyUpdate buildTimelinePartyUpdate() {
		TimelinePartyUpdate timelineUpdate = new TimelinePartyUpdate();

		timelineUpdate.setId(this.id);
		timelineUpdate.setLocalId(this.localId);
		timelineUpdate.setName(this.party.getName());
		timelineUpdate.setType(this.party.getType().getValue());

		return timelineUpdate;
	}

	public TimelineCreateResponse buildTimelineCreateResponse() {
		TimelineCreateResponse timelineResponse = null;
			timelineResponse = new TimelineCreateResponse();
			timelineResponse.setTimelineId(this.getId());

		return timelineResponse;
	}
}
