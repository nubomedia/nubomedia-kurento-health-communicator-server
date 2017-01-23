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

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class MessageReadResponse implements Serializable {

	private static final long serialVersionUID = 1L;

	@XmlElement(required = true)
	private Long id;

	@XmlElement
	private Long localId;

	@XmlElement(required = true)
	private Long timestamp;

	@XmlElement(required = true)
	private TimelinePartyUpdate timeline;

	@XmlElement(required = true)
	private UserReadAvatarResponse from;

	@XmlElement
	private Object app;

	@XmlElement
	private String body;

	@XmlElement
	private ContentReadResponse content;

	// /////////

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getLocalId() {
		return localId;
	}

	public void setLocalId(Long localId) {
		this.localId = localId;
	}

	public UserReadAvatarResponse getFrom() {
		return from;
	}

	public void setFrom(UserReadAvatarResponse from) {
		this.from = from;
	}

	public TimelinePartyUpdate getTimeline() {
		return timeline;
	}

	public void setTimeline(TimelinePartyUpdate timeline) {
		this.timeline = timeline;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	public Object getApp() {
		return app;
	}

	public void setApp(Object app) {
		this.app = app;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public ContentReadResponse getContent() {
		return content;
	}

	public void setContent(ContentReadResponse content) {
		this.content = content;
	}

}
