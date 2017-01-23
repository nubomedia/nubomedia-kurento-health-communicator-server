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

@Component("messageDao")
public class MessageDao extends BaseDao {

	@Autowired
	private AccountDao accountDao;
	@Autowired
	private UserDao userDao;
	@Autowired
	private GroupDao groupDao;
	@Autowired
	private ConversationDao conversationDao;
	@Autowired
	private TimelineDao timelineDao;
	@Autowired
	private ContentDao contentDao;

	@Transactional
	public MessageEntity sendMessage(MessageEntity message, UserEntity from,
			GroupEntity to, ContentEntity content) {
		UserEntity dbFrom = userDao.attach(from);
		GroupEntity dbTo = groupDao.attach(to);

		// Group conversation must exists
		ConversationEntity dbConversation = dbTo.getConversation();

		return saveMessage(message, content, dbFrom, dbConversation);
	}

	public MessageEntity sendMessage(MessageEntity message, UserEntity from,
			UserEntity to, ContentEntity content) {
		UserEntity dbFrom = userDao.attach(from);
		UserEntity dbTo = userDao.attach(to);
		// Get user conversation or create a new one
		TimelineEntity dbTimeline;

		if ((dbTimeline = userDao.getUserTimelineWithParty(dbFrom,
				dbTo.getUUID())) == null) {
			dbTimeline = timelineDao.createTimeline(dbFrom, dbTo);
		}

		ConversationEntity dbConversation = dbTimeline.getConversation();

		return saveMessage(message, content, dbFrom, dbConversation);
	}

	private MessageEntity saveMessage(MessageEntity message,
			ContentEntity content, UserEntity dbFrom,
			ConversationEntity dbConversation) {
		ContentEntity dbContent = null;
		if (content != null) {
			// Content is optional and can be null
			dbContent = contentDao.attach(content);
		}
		super.save(message);
		message.setFrom(dbFrom);
		message.setContent(dbContent);
		message.setConversation(dbConversation);
		em.flush();
		return message;
	}

	@Transactional
	public void deleteMessage(MessageEntity message) {

		MessageEntity dbMessage = attach(message);

		// Remove relationship with user
		dbMessage.setFrom(null);

		// Remove relationship with conversation
		dbMessage.setConversation(null);

		// Content deletion is cascaded

		super.delete(dbMessage);
	}

	@Transactional
	public MessageEntity updateMessage(MessageEntity message) {
		em.detach(message);
		MessageEntity dbMessage = attach(message);
		if (message.getPayload() != null)
			dbMessage.setPayload(message.getPayload());
		return dbMessage;

	}

	@Transactional
	public MessageEntity findMessageById(Long id) {
		return findSingle(MessageEntity.class, new String[] { "id" },
				new Object[] { id });
	}

	@Transactional
	public MessageEntity findMessageByUUID(Long uuid) {
		return findSingle(MessageEntity.class, new String[] { "uuid" },
				new Object[] { uuid });
	}

	@Transactional
	public UserEntity getMessageSender(MessageEntity message) {

		MessageEntity dbMessage = attach(message);
		UserEntity user = dbMessage.getFrom();
		user.getId(); // Force object creation
		return user;
	}

	@Transactional
	public ContentEntity getMessageContent(MessageEntity message) {

		MessageEntity dbMessage = attach(message);
		ContentEntity content = dbMessage.getContent();
		if (content != null) {
			content.getId(); // Force object creation
		}
		return content;
	}

	@SuppressWarnings("unchecked")
	@Transactional
	public List<TimelineEntity> getMessageTimelines(MessageEntity message) {
		MessageEntity dbMessage = attach(message);
		ConversationEntity dbConversation = dbMessage.getConversation();
		Query q = em.createNamedQuery(
				ConversationEntity.NQ_NAME_FIND_CONVERSATION_TIMELINES)
				.setParameter(ConversationEntity.NQ_PARAM_CONVERSATION,
						dbConversation);
		return (List<TimelineEntity>) q.getResultList();
	}

	@Transactional
	public ConversationEntity getMessageConversation(MessageEntity message) {
		MessageEntity dbMessage = attach(message);
		return dbMessage.getConversation();
	}

	// ////////////////////////
	// Security verifications
	// ////////////////////////

	@Transactional
	public Boolean canReadMessage(MessageEntity message, UserEntity owner) {
		try {
			MessageEntity dbMessage = attach(message);
			UserEntity dbOwner = userDao.attach(owner);
			for (TimelineEntity dbTimeline : getMessageTimelines(dbMessage)) {
				if (dbTimeline.getUser().getId().equals(dbOwner.getId())) {
					return true;
				}
			}
			return false;
		} catch (Exception e) {
			return false;
		}
	}

	@Transactional
	public Boolean canUpdateMessage(MessageEntity message, UserEntity owner) {
		try {
			boolean canReadMsg = canReadMessage(message, owner);
			MessageEntity dbMessage = attach(message);
			boolean asAdminAllowed = userDao.isAdminAllowToImpersonate()
					&& userDao.isUserAdmin(dbMessage.getFrom(), owner);
			return canReadMsg || asAdminAllowed;
		} catch (Exception e) {
			return false;
		}
	}

	@Transactional
	public Boolean canSendMessageToGroup(UserEntity fromUser, GroupEntity toGroup, UserEntity owner) {
		try {
			UserEntity dbOwner = userDao.attach(owner);
			UserEntity dbFromUser = userDao.attach(fromUser);
			boolean toAllowed = groupDao.isGroupMember(toGroup, fromUser);
			boolean fromAllowed = dbFromUser.getId() == dbOwner.getId();
			boolean asAdminAllowed = userDao.isAdminAllowToImpersonate()
					&& userDao.isUserAdmin(fromUser, owner);
			return toAllowed && (fromAllowed || asAdminAllowed);
		} catch (Exception e) {
			return false;
		}
	}

	// /////////////////
	//
	// HELPERS
	//
	// /////////////////

	protected MessageEntity attach(final MessageEntity message) {

		Assert.notNull(message);
		if (em.contains(message)) {
			return message;
		} else {
			MessageEntity dbMessage;
			if ((dbMessage = em.find(MessageEntity.class, message.getId())) == null) {
				throw new KhcNotFoundException(
						"Unable to attach unknown message to JPA",
						KhcNotFoundInfo.Code.SYNCHRONIZATION_ERROR,
						MessageEntity.class.getSimpleName(),
						new HashMap<String, String>() {
							private static final long serialVersionUID = 1L;
							{
								put("uuid", "" + message.getUUID());
							}
						});
			} else {
				return dbMessage;
			}
		}

	}
}