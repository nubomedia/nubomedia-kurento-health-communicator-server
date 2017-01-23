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

import com.kurento.agenda.datamodel.pojo.Message;
import com.kurento.agenda.datamodel.pojo.User;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class MessageSend implements Serializable {

	private static final long serialVersionUID = 1L;

	// Attributes

	@XmlElement
	private Long localId;

	@XmlElement(required = true)
	private Long from;

	@XmlElement(required = true)
	private Long to;

	@XmlElement
	private String body;

	@XmlElement
	private Object app;

	// Getters & setters

	public Long getLocalId() {
		return localId;
	}

	public void setLocalId(Long localId) {
		this.localId = localId;
	}

	public Long getFrom() {
		return from;
	}

	public void setFrom(Long from) {
		this.from = from;
	}

	public Long getTo() {
		return to;
	}

	public void setTo(Long to) {
		this.to = to;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public Object getApp() {
		return app;
	}

	public void setApp(Object app) {
		this.app = app;
	}

	// //////////////////////////////////////////////
	// Format converters
	// //////////////////////////////////////////////

	public Message buildMessagePojo() {
		Message message = new Message();
		User from = new User();
		message.setLocalId(this.localId);
		message.setFrom(from);
		from.setId(this.from);
		message.setTo(this.to);
		message.setBody(this.body);
		message.setApp(this.app);
		return message;
	}

	// FIXME delete if not required (otherwise add payloadJson as method param)
	@Deprecated
	protected Message buildMessage() {
		Message message = new Message();
		User user = new User();
		user.setId(this.getFrom());
		message.setFrom(user);
		message.setTo(this.getTo());
		message.setBody(this.getBody());
		return message;
	}

}
