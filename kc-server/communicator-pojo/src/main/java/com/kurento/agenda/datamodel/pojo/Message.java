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

/**
 *
 */
package com.kurento.agenda.datamodel.pojo;

import com.kurento.agenda.services.pojo.MessageReadResponse;
import com.kurento.agenda.services.pojo.MessageSendResponse;

public class Message extends BasePojo {

	private static final long serialVersionUID = 1L;

	// Attributes

	private Long localId;

	private Long timestamp;

	private User from;

	private Long to;

	private Object app;

	private String body;

	private Content content;

	// Getters & Setters

	public Long getLocalId() {
		return localId;
	}

	public void setLocalId(Long localId) {
		this.localId = localId;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	public User getFrom() {
		return from;
	}

	public void setFrom(User from) {
		this.from = from;
	}

	public Long getTo() {
		return to;
	}

	public void setTo(Long to) {
		this.to = to;
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

	public Content getContent() {
		return content;
	}

	public void setContent(Content content) {
		this.content = content;
	}

	// //////////////////////////////////////////
	// Format converters
	// //////////////////////////////////////////

	public MessageReadResponse buildMessageReadResponse() {
		MessageReadResponse messageRead = new MessageReadResponse();
		messageRead.setId(this.id);
		messageRead.setLocalId(this.localId);
		messageRead.setTimestamp(this.timestamp);
		if (this.from != null) {
			messageRead.setFrom(this.from.buildUserReadAvatarResponse());
		}
		messageRead.setBody(this.body);
		messageRead.setApp(this.app);
		if (this.content != null) {
			messageRead.setContent(this.content.buildContentReadResponse());
		}
		return messageRead;
	}


	protected MessageSendResponse buildMessageSendResponse() {
		MessageSendResponse messageResponse = null;
		messageResponse = new MessageSendResponse();
		messageResponse.setMessageId(this.getId());
		return messageResponse;
	}
}