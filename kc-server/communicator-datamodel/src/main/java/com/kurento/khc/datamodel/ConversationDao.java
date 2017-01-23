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

import java.util.HashMap;
import java.util.List;

import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.kurento.agenda.datamodel.pojo.KhcNotFoundInfo;
import com.kurento.khc.KhcNotFoundException;

@Component("ConversationDao")
public class ConversationDao extends BaseDao {

	@Autowired
	MessageDao messageDao;
	@Autowired
	TimelineDao timelineDao;

	@Transactional
	protected ConversationEntity createConversation(
			ConversationEntity conversation) {
		super.save(conversation);
		return conversation;
	}

	@SuppressWarnings("unchecked")
	@Transactional
	protected void deleteConversation(ConversationEntity conversation) {

		ConversationEntity dbConversation = attach(conversation);

		// Remove relationship with timeline
		Query q = em.createNamedQuery(
				ConversationEntity.NQ_NAME_FIND_CONVERSATION_TIMELINES)
				.setParameter(ConversationEntity.NQ_PARAM_CONVERSATION,
						dbConversation);

		for (TimelineEntity timeline : (List<TimelineEntity>) q.getResultList()) {
			timelineDao.deleteTimeline(timeline);
		}

		// Remove relationship with messages
		List<MessageEntity> messages;
		do {
			q = em
					.createNamedQuery(
							MessageEntity.NQ_NAME_GET_CONVERSATION_FROM_START)
					.setMaxResults(500)
					.setParameter(MessageEntity.NQ_PARAM_CONVERSATION_ID,
							dbConversation.getId());
			messages = (List<MessageEntity>) q.getResultList();
			for (MessageEntity message : messages) {
				message.setConversation(null);
				messageDao.deleteMessage(message);
			}
			em.flush();
		} while (messages.size() > 0);
		super.delete(dbConversation);

	}

	@Transactional
	public List<MessageEntity> getConversationMessages(
			ConversationEntity conversation, Integer maxMessage) {

		ConversationEntity dbConversation = attach(conversation);

		Query q = em
				.createNamedQuery(
						MessageEntity.NQ_NAME_GET_CONVERSATION_FROM_START)
				.setParameter(MessageEntity.NQ_PARAM_CONVERSATION_ID,
						dbConversation.getId()).setMaxResults(maxMessage);
		@SuppressWarnings("unchecked")
		List<MessageEntity> messages = q.getResultList();
		return messages;
	}

	@Transactional
	public List<MessageEntity> getConversationMessages(
			ConversationEntity conversation, MessageEntity lastMessage,
			Integer maxMessage) {

		ConversationEntity dbConversation = attach(conversation);
		MessageEntity dbLastMessage = messageDao.attach(lastMessage);

		Query q = em
				.createNamedQuery(
						MessageEntity.NQ_NAME_GET_CONVERSATION_FROM_MSG)
				.setParameter(MessageEntity.NQ_PARAM_CONVERSATION_ID,
						dbConversation.getId())
				.setParameter(MessageEntity.NQ_PARAM_MESSAGE_ID,
						dbLastMessage.getId()).setMaxResults(maxMessage);
		@SuppressWarnings("unchecked")
		List<MessageEntity> messages = q.getResultList();

		return messages;
	}

	// /////////////////
	//
	// HELPERS
	//
	// /////////////////

	@Transactional
	protected ConversationEntity attach(final ConversationEntity conversation) {

		Assert.notNull(conversation);
		if (em.contains(conversation)) {
			return conversation;
		} else {
			ConversationEntity dbConversation;
			if ((dbConversation = em.find(ConversationEntity.class,
					conversation.getId())) == null) {
				throw new KhcNotFoundException(
						"Request to attach unknown conversation to JPA",
						KhcNotFoundInfo.Code.SYNCHRONIZATION_ERROR,
						TimelineEntity.class.getSimpleName(),
						new HashMap<String, String>() {
							private static final long serialVersionUID = 1L;
							{
								put("uuid", "" + conversation.getUUID());
							}
						});
			} else {
				return dbConversation;
			}
		}
	}

}
