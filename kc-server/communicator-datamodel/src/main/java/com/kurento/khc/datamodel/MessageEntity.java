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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;

import com.kurento.agenda.datamodel.pojo.TimelineParty.PartyType;

@Entity
@NamedQueries({
		@NamedQuery(name = MessageEntity.NQ_NAME_GET_LAST_MSGID, query = ""
				+ "SELECT msg FROM MessageEntity msg ORDER BY msg.id DESC"),

		@NamedQuery(name = MessageEntity.NQ_NAME_GET_CONVERSATION_FROM_MSG, query = ""
				+ "SELECT msg FROM MessageEntity msg JOIN msg.conversation cv WHERE cv.id = :"
				+ MessageEntity.NQ_PARAM_CONVERSATION_ID
				+ " AND msg.id < :"
				+ MessageEntity.NQ_PARAM_MESSAGE_ID + " ORDER BY msg.id DESC"),

		@NamedQuery(name = MessageEntity.NQ_NAME_GET_CONVERSATION_FROM_START, query = ""
				+ "SELECT msg FROM MessageEntity msg JOIN msg.conversation cv WHERE cv.id = :"
				+ MessageEntity.NQ_PARAM_CONVERSATION_ID
				+ " ORDER BY msg.id DESC"),

		@NamedQuery(name = MessageEntity.NQ_NAME_GET_USER_MSG, query = ""
				+ "SELECT msg FROM MessageEntity msg WHERE msg.from = :"
				+ MessageEntity.NQ_PARAM_FROM)

})
public class MessageEntity extends BaseEntity {
	private static final long serialVersionUID = 1L;

	public static final String NQ_NAME_GET_LAST_MSGID = "MessageEntity.getLastMessageId";
	public static final String NQ_NAME_GET_CONVERSATION_FROM_MSG = "MessageEntity.getConversationFromMessage";
	public static final String NQ_NAME_GET_CONVERSATION_FROM_START = "MessageEntity.getConversationFromStart";
	public static final String NQ_NAME_GET_USER_MSG = "MessageEntity.getUserMessages";
	public static final String NQ_NAME_GET_TIMESTAMP_MSG = "MessageEntity.getTimestampMessages";

	public static final String NQ_PARAM_MESSAGE_ID = "messageId";
	public static final String NQ_PARAM_CONVERSATION_ID = "conversationId";
	public static final String NQ_PARAM_FROM = "from";
	public static final String NQ_PARAM_TO = "to";

	// Attributes

	private Long timestamp;

	private Long localId;

	// This side is owner
	@ManyToOne(fetch = FetchType.LAZY)
	private UserEntity from;

	@Column(nullable = false)
	private Long toUUID;

	@Column(nullable = false)
	private PartyType toType;

	@Lob
	@Column(length = 10000)
	private String body;

	@Column(length = 500)
	private String payload;

	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	private ContentEntity content;

	@ManyToOne(fetch = FetchType.LAZY)
	private ConversationEntity conversation;


	// Getters & setters

	public Long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	public Long getLocalId() {
		return localId;
	}

	public void setLocalId(Long localId) {
		this.localId = localId;
	}

	public Long getToUUID() {
		return toUUID;
	}

	public void setToUUID(Long toUUID) {
		this.toUUID = toUUID;
	}

	public PartyType getToType() {
		return toType;
	}

	public void setToType(PartyType toType) {
		this.toType = toType;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}

	protected UserEntity getFrom() {
		return from;
	}

	protected void setFrom(UserEntity from) {
		this.from = from;
	}

	protected ContentEntity getContent() {
		return content;
	}

	protected void setContent(ContentEntity content) {
		this.content = content;
	}

	protected ConversationEntity getConversation() {
		return conversation;
	}

	protected void setConversation(ConversationEntity conversation) {
		this.conversation = conversation;
	}
}