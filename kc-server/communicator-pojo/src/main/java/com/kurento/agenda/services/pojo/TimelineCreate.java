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

package com.kurento.agenda.services.pojo;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.kurento.agenda.datamodel.pojo.Timeline;
import com.kurento.agenda.datamodel.pojo.TimelineParty;
import com.kurento.agenda.datamodel.pojo.TimelineParty.PartyType;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class TimelineCreate implements Serializable {

	private static final long serialVersionUID = 1L;

	// Attributes
	@XmlElement(required = true)
	private Long localId;

	@XmlElement(required = true)
	private Long ownerId;

	@XmlElement(required = true)
	private TimelinePartyUpdate party;

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

	public TimelinePartyUpdate getParty() {
		return party;
	}

	public void setParty(TimelinePartyUpdate party) {
		this.party = party;
	}

	// //////////////////////////////////////////////
	// Format converters
	// //////////////////////////////////////////////

	public Timeline buildTimelinePojo() {
		Timeline timeline = new Timeline();
		timeline.setLocalId(this.getLocalId());
		timeline.setOwnerId(this.ownerId);
		if (party != null) {
			timeline.setParty(this.party.buildTimelinePartyPojo());
		}
		return timeline;
	}

	protected Timeline buildTimeline() {
		Timeline timeline = null;
		timeline = new Timeline();
		timeline.setLocalId(this.getLocalId());
		TimelineParty party = new TimelineParty();

		timeline.setOwnerId(this.getOwnerId());
		timeline.setParty(party);

		party.setId(this.getParty().getId());
		party.setType(PartyType.create(this.getParty().getType()));

		return timeline;
	}

}
