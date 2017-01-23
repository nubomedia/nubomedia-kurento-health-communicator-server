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
import com.kurento.agenda.datamodel.pojo.TimelineParty.PartyType;
import com.kurento.khc.KhcNotFoundException;

@Component("timelineDao")
public class TimelineDao extends BaseDao {

	@Autowired
	private UserDao userDao;
	@Autowired
	private GroupDao groupDao;
	@Autowired
	private MessageDao messageDao;
	@Autowired
	private ConversationDao conversationDao;

	@Transactional
	public TimelineEntity createTimeline(UserEntity owner, UserEntity party) {
		return createTimeline(new TimelineEntity(), owner, party);
	}

	@Transactional
	public TimelineEntity createTimeline(TimelineEntity timeline,
			UserEntity owner, UserEntity party) {
		UserEntity dbOwner = userDao.attach(owner);
		UserEntity dbParty = userDao.attach(party);

		// Check if owner already has a timeline with party
		TimelineEntity dbTimeline;
		ConversationEntity dbConversation = null;
		if ((dbTimeline = dbOwner.getTimelines().get(dbParty.getUUID())) != null) {
			dbTimeline.setLocalId(timeline.getLocalId());
			return dbTimeline;
		}
		// Check if party already has a timeline with owner
		TimelineEntity dbPartyTimeline;
		if ((dbPartyTimeline = dbParty.getTimelines().get(dbOwner.getUUID())) != null) {
			dbConversation = dbPartyTimeline.getConversation();
		}
		// Create conversation if none exists
		if (dbConversation == null) {
			dbConversation = conversationDao
					.createConversation(new ConversationEntity());
		}
		// Create timeline for owner and party if they don't exist
		if (dbPartyTimeline == null) {
			dbPartyTimeline = new TimelineEntity();
			dbPartyTimeline.setPartyType(PartyType.USER);
			dbPartyTimeline.setPartyUUID(dbOwner.getUUID());
			super.save(dbPartyTimeline);
			dbPartyTimeline.setUser(dbParty);
			dbPartyTimeline.setConversation(dbConversation);

		}
		if (dbTimeline == null) {
			dbTimeline = new TimelineEntity();
			dbTimeline.setLocalId(timeline.getLocalId());
			dbTimeline.setPartyType(PartyType.USER);
			dbTimeline.setPartyUUID(dbParty.getUUID());
			super.save(dbTimeline);
			dbTimeline.setUser(dbOwner);
			dbTimeline.setConversation(dbConversation);
		}
		em.flush();
		return dbTimeline;
	}

	@Transactional
	public TimelineEntity createTimeline(UserEntity owner, GroupEntity party) {
		return createTimeline(new TimelineEntity(), owner, party);
	}

	@Transactional
	public TimelineEntity createTimeline(TimelineEntity timeline,
			UserEntity owner, GroupEntity party) {
		UserEntity dbOwner = userDao.attach(owner);
		GroupEntity dbParty = groupDao.attach(party);
		TimelineEntity dbTimeline;
		if ((dbTimeline = dbOwner.getTimelines().get(dbParty.getUUID())) != null) {
			// Return if already exists
			return dbTimeline;
		}
		ConversationEntity dbConversation = dbParty.getConversation();
		// Create timeline
		timeline.setPartyType(PartyType.GROUP);
		timeline.setPartyUUID(dbParty.getUUID());
		super.save(timeline);
		timeline.setUser(dbOwner);
		timeline.setConversation(dbConversation);
		em.flush();
		return dbTimeline;
	}

	@SuppressWarnings("unchecked")
	@Transactional
	public void deleteTimeline(TimelineEntity timeline) {
		TimelineEntity dbTimeline = attach(timeline);

		// Remove relationship with user
		dbTimeline.setUser(null);

		// Remove relationship with conversation
		ConversationEntity conversation = dbTimeline.getConversation();
		dbTimeline.setConversation(null);

		// Find out how many timelines are linked to conversation
		Query q = em.createNamedQuery(
				ConversationEntity.NQ_NAME_FIND_CONVERSATION_TIMELINES)
				.setParameter(ConversationEntity.NQ_PARAM_CONVERSATION,
						conversation);
		List<TimelineEntity> remainingTimelines = (List<TimelineEntity>) q
				.getResultList();
		// Find out if a group is linked to this conversation
		q = em.createNamedQuery(
				ConversationEntity.NQ_NAME_FIND_CONVERSATION_GROUP)
				.setParameter(ConversationEntity.NQ_PARAM_CONVERSATION,
						conversation);
		List<GroupEntity> remainingGroups = (List<GroupEntity>) q
				.getResultList();
		if (remainingTimelines.size() == 0 && remainingGroups.size() == 0) {
			conversationDao.deleteConversation(conversation);
		}
		super.delete(dbTimeline);
		em.flush();
	}

	@Transactional
	public TimelineEntity updateTimeline(TimelineEntity timeline) {
		em.detach(timeline);
		TimelineEntity dbTimeline = attach(timeline);
		if (timeline.getState() != null) {
			dbTimeline.setState(timeline.getState());
		}
		em.flush();
		return dbTimeline;
	}

	@Transactional(noRollbackFor = KhcNotFoundException.class)
	public TimelineEntity findTimelineById(Long id) {
		return findSingle(TimelineEntity.class, new String[] { "id" },
				new Object[] { id });
	}

	@Transactional
	public TimelineEntity findTimelineByUUID(Long uuid) {
		return findSingle(TimelineEntity.class, new String[] { "uuid" },
				new Object[] { uuid });
	}

	@Transactional(noRollbackFor = KhcNotFoundException.class)
	public TimelineEntity findTimelineByOwner(UserEntity owner, Long partyUUID,
			PartyType type) {

		return findSingle(TimelineEntity.class, new String[] { "user",
				"partyUuid", "partyType" }, new Object[] { owner, partyUUID,
				type });
	}

	@Transactional(noRollbackFor = KhcNotFoundException.class)
	public MessageEntity findTimelineMessageByUUID(TimelineEntity timeline,
			final Long messageId) {
		TimelineEntity dbTimeline = attach(timeline);
		ConversationEntity dbConversation = dbTimeline.getConversation();
		MessageEntity dbMessage = messageDao.findMessageByUUID(messageId);
		if (dbMessage.getConversation().getId().equals(dbConversation.getId())) {
			return dbMessage;
		}
		throw new KhcNotFoundException("Unable to find message in timeline",
				KhcNotFoundInfo.Code.SYNCHRONIZATION_ERROR,
				MessageEntity.class.getSimpleName(),
				new HashMap<String, String>() {
					private static final long serialVersionUID = 1L;
					{
						put("uuid", "" + messageId);
					}
				});
	}

	@Transactional
	public List<MessageEntity> getTimelineMessages(TimelineEntity timeline,
			Integer maxMessage) {

		TimelineEntity dbTimeline = attach(timeline);
		ConversationEntity dbConversation = dbTimeline.getConversation();

		return conversationDao.getConversationMessages(dbConversation,
				maxMessage);
	}

	@Transactional
	public List<MessageEntity> getTimelineMessages(TimelineEntity timeline,
			MessageEntity lastMessage, Integer maxMessage) {

		TimelineEntity dbTimeline = attach(timeline);
		ConversationEntity dbConversation = dbTimeline.getConversation();

		return conversationDao.getConversationMessages(dbConversation,
				lastMessage, maxMessage);
	}

	@Transactional
	public UserEntity getTimelineUser(TimelineEntity timeline) {
		TimelineEntity dbTimeline = attach(timeline);
		UserEntity dbUser = dbTimeline.getUser();
		dbUser.getId();
		return dbUser;
	}

	@SuppressWarnings("unchecked")
	@Transactional
	public List<TimelineEntity> getGroupTimelines(GroupEntity group) {

		GroupEntity dbGroup = groupDao.attach(group);

		Query q = em.createNamedQuery(
				ConversationEntity.NQ_NAME_FIND_CONVERSATION_TIMELINES)
				.setParameter(ConversationEntity.NQ_PARAM_CONVERSATION,
						dbGroup.getConversation());

		return q.getResultList();
	}

	// ////////////////////////
	// Security verifications
	// ////////////////////////

	public Boolean isTimelineAdmin(TimelineEntity timeline, UserEntity admin) {

		try {
			TimelineEntity dbTimeline = attach(timeline);
			UserEntity dbAdmin = userDao.attach(admin);
			UserEntity dbOwner = dbTimeline.getUser();
			if (dbOwner.getId().equals(dbAdmin.getId())) {
				return true;
			} else if (userDao.isUserAdmin(dbOwner, dbAdmin)) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
	}

	// /////////////////
	//
	// HELPERS
	//
	// /////////////////

	@Transactional
	protected TimelineEntity attach(final TimelineEntity timeline) {

		Assert.notNull(timeline);
		if (em.contains(timeline)) {
			return timeline;
		} else {
			TimelineEntity dbTimeline;
			if ((dbTimeline = em.find(TimelineEntity.class, timeline.getId())) == null) {
				throw new KhcNotFoundException(
						"Request to attach unknown timeline to JPA",
						KhcNotFoundInfo.Code.SYNCHRONIZATION_ERROR,
						TimelineEntity.class.getSimpleName(),
						new HashMap<String, String>() {
							private static final long serialVersionUID = 1L;
							{
								put("uuid", "" + timeline.getUUID());
							}
						});
			} else {
				return dbTimeline;
			}
		}
	}

}
