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

package com.kurento.khc.services.v2.internal;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.kurento.agenda.datamodel.pojo.Content;
import com.kurento.agenda.datamodel.pojo.KhcInvalidDataInfo.Code;
import com.kurento.agenda.datamodel.pojo.KhcNotFoundInfo;
import com.kurento.agenda.datamodel.pojo.Message;
import com.kurento.agenda.datamodel.pojo.Timeline;
import com.kurento.agenda.datamodel.pojo.TimelineParty;
import com.kurento.agenda.datamodel.pojo.TimelineParty.PartyType;
import com.kurento.agenda.services.pojo.MessageApp;
import com.kurento.khc.KhcInvalidDataException;
import com.kurento.khc.KhcNotFoundException;
import com.kurento.khc.datamodel.ContentDao;
import com.kurento.khc.datamodel.ContentEntity;
import com.kurento.khc.datamodel.GroupDao;
import com.kurento.khc.datamodel.GroupEntity;
import com.kurento.khc.datamodel.MessageDao;
import com.kurento.khc.datamodel.MessageEntity;
import com.kurento.khc.datamodel.MessageSecureDao;
import com.kurento.khc.datamodel.TimelineDao;
import com.kurento.khc.datamodel.TimelineEntity;
import com.kurento.khc.datamodel.TimelineSecureDao;
import com.kurento.khc.datamodel.UserDao;
import com.kurento.khc.datamodel.UserEntity;
import com.kurento.khc.datamodel.UserSecureDao;
import com.kurento.khc.jackson.KhcObjectMapper;
import com.kurento.khc.services.v2.AdministrativeService;
import com.kurento.khc.services.v2.ContentService;
import com.kurento.khc.services.v2.MessageService;
import com.kurento.khc.utils.FileRepository;
import com.kurento.khc.utils.SecurityUtils;

@Service("khcMessageServicev2")
public class MessageServiceImpl implements MessageService {

	@Autowired
	private AdministrativeService administrativeService;
	@Autowired
	private ContentService contentService;

	@Autowired
	private UserSecureDao userSecureDao;
	@Autowired
	private MessageSecureDao messageSecureDao;
	@Autowired
	private TimelineSecureDao timelineSecureDao;

	@Autowired
	private GroupDao groupDao;
	@Autowired
	private UserDao userDao;
	@Autowired
	private ContentDao contentDao;
	@Autowired
	private MessageDao messageDao;
	@Autowired
	private TimelineDao timelineDao;

	@Autowired
	private FileRepository repository;
	@Autowired
	private SecurityUtils securityUtils;

	private ObjectMapper jsonMapper = new KhcObjectMapper();

	@Override
	@Transactional
	public Message getMessage(Long messageId) {

		MessageEntity messageEntity = messageSecureDao
				.findMessageByUUID(messageId);

		return buildMessagePojo(messageEntity);
	}

	@Override
	@Transactional
	public List<Message> getMessageListFromTimeline(Long timelineId,
			Integer maxMessage) {
		return getMessageListFromTimeline(timelineId, null, maxMessage);
	}

	@Override
	@Transactional
	public List<Message> getMessageListFromTimeline(Long timelineId,
			Long lastMessage, Integer maxMessage) {

		// Get timeline
		TimelineEntity timelineEntity = timelineSecureDao
				.findTimelineByUUID(timelineId);

		List<MessageEntity> messageEntities;
		// Get timeline messages
		if (lastMessage == null) {
			messageEntities = timelineDao.getTimelineMessages(timelineEntity,
					maxMessage);
		} else {
			MessageEntity lastMessageEntity = messageDao
					.findMessageByUUID(lastMessage);
			messageEntities = timelineDao.getTimelineMessages(timelineEntity,
					lastMessageEntity, maxMessage);
		}

		// Build Message pojos
		List<Message> messages = new ArrayList<Message>();
		for (MessageEntity messageEntity : messageEntities) {
			Message message = buildMessagePojo(messageEntity);
			messages.add(message);
		}

		return messages;
	}

	@Override
	@Transactional
	public Content getMessageContent(Long timelineId, final Long messageId) {
		TimelineEntity timeline = timelineDao.findTimelineByUUID(timelineId);
		MessageEntity message = timelineSecureDao.findTimelineMessageByUUID(
				timeline, messageId);
		ContentEntity content = messageDao.getMessageContent(message);
		if (content != null) {
			return contentService.buildContentPojo(content);
		} else {
			throw new KhcNotFoundException("Message content not found",
					KhcNotFoundInfo.Code.SYNCHRONIZATION_IGNORE,
					MessageEntity.class.getSimpleName(),
					new HashMap<String, String>() {
						private static final long serialVersionUID = 1L;
						{
							put("uuid", "" + messageId);
						}
					});
		}
	}

	@Override
	@Transactional
	public Content getMessageAvatar(Long timelineId, final Long messageId)
			throws FileNotFoundException {
		TimelineEntity timeline = timelineDao.findTimelineByUUID(timelineId);
		MessageEntity message = timelineSecureDao.findTimelineMessageByUUID(
				timeline, messageId);
		UserEntity from = messageDao.getMessageSender(message);
		return contentService.buildContentPojo(userDao.getPicture(from));

	}

	@Override
	@Transactional
	public List<Timeline> getMessageTimelines(Long userId) {
		UserEntity userEntity = userSecureDao.findUserByUUID(userId);
		List<Timeline> timelines = new ArrayList<Timeline>();
		for (TimelineEntity timelineEntity : userDao
				.getUserTimelines(userEntity)) {
			timelines.add(buildTimelinePojo(timelineEntity));
		}
		return timelines;
	}

	@Override
	@Transactional
	public Timeline CreateMessageTimeline(Timeline timelineReq) {
		// Find timeline owner
		UserEntity ownerEntity = userDao.findUserByUUID(timelineReq
				.getOwnerId());
		TimelineParty party = timelineReq.getParty();
		TimelineEntity timelineEntity = new TimelineEntity();
		timelineEntity.setLocalId(timelineReq.getId());
		if (party.equals(PartyType.USER)) {
			UserEntity partyEntity = userDao.findUserByUUID(party.getId());
			timelineEntity = timelineDao.createTimeline(timelineEntity,
					ownerEntity,
					partyEntity);
		} else if (party.equals(PartyType.GROUP)) {
			GroupEntity partyEntity = groupDao.findGroupByUUID(party.getId());
			timelineEntity = timelineDao.createTimeline(timelineEntity,
					ownerEntity,
					partyEntity);
		} else {
			throw new KhcInvalidDataException(
					"Unkown party type while creating timeline: " + party,
					Code.INVALID_DATA);
		}

		return buildTimelinePojo(timelineEntity);
	}

	@Override
	@Transactional
	public Timeline getMessageTimeline(Long timelineId) {

		// Get timeline
		TimelineEntity timelineEntity = timelineSecureDao
				.findTimelineByUUID(timelineId);

		// Build timeline pojo
		return buildTimelinePojo(timelineEntity);
	}

	@Override
	@Transactional
	public Timeline getMesssageTimelineWithGroup(Long groupId) {
		UserEntity owner = securityUtils.getPrincipal();
		TimelineEntity timeline = timelineSecureDao.findTimelineByOwner(owner,
				groupId, PartyType.GROUP);
		return buildTimelinePojo(timeline);
	}

	// ////////////////////////////////////
	// Format converters
	// ////////////////////////////////////

	@Override
	public MessageEntity buildMessageEntity(Message message) {
		Assert.notNull(message);
		// Create message entity
		MessageEntity messageEntity = new MessageEntity();
		messageEntity.setLocalId(message.getLocalId());
		messageEntity.setTimestamp(System.currentTimeMillis());
		messageEntity.setBody(message.getBody());
		if (message.getApp() != null)
			messageEntity.setPayload(buildJson(message.getApp()));
		return messageEntity;
	}

	@Override
	public Message buildMessagePojo(MessageEntity messageEntity) {
		Assert.notNull(messageEntity);

		Message message = new Message();
		message.setId(messageEntity.getUUID());
		message.setLocalId(messageEntity.getLocalId());
		message.setTimestamp(messageEntity.getTimestamp());
		UserEntity fromEntity = messageDao.getMessageSender(messageEntity);
		if (fromEntity != null) {
			message.setFrom(administrativeService.buildUserPojo(fromEntity));
		}
		message.setTo(messageEntity.getToUUID());
		message.setBody(messageEntity.getBody());
		if (messageEntity.getPayload() != null)
			message.setApp(parseJson(messageEntity.getPayload(), MessageApp.class));
		ContentEntity contentEntity = messageDao
				.getMessageContent(messageEntity);
		if (contentEntity != null) {
			message.setContent(contentService.buildContentPojo(contentEntity));
		}

		return message;
	}

	@Override
	public Timeline buildTimelinePojo(TimelineEntity timelineEntity) {
		Assert.notNull(timelineEntity);

		UserEntity owner = timelineDao.getTimelineUser(timelineEntity);

		TimelineParty party = new TimelineParty();
		party.setId(timelineEntity.getPartyUUID());
		party.setType(timelineEntity.getPartyType());
		party.setName(getTimelineName(party));

		Timeline timeline = new Timeline();
		timeline.setId(timelineEntity.getUUID());
		timeline.setLocalId(timelineEntity.getLocalId());
		timeline.setOwnerId(owner.getUUID());
		timeline.setParty(party);
		timeline.setState(timelineEntity.getState());
		return timeline;
	}

	private String getTimelineName(TimelineParty party) {
		if (PartyType.GROUP.equals(party.getType())) {
			GroupEntity group = groupDao.findGroupByUUID(party.getId());
			return group.getName() != null ? group.getName() : "";
		} else if (PartyType.USER.equals(party.getType())) {
			UserEntity user = userDao.findUserByUUID(party.getId());
			String name = user.getName() != null ? user.getName() + " " : "";
			name += user.getSurname() != null ? user.getSurname() : "";
			return name;
		} else {
			return "";
		}
	}

	protected <T> T parseJson(String json, Class<T> clazz) {
		if (json == null)
			return null;
		try {
			return jsonMapper.readValue(json, clazz);
		} catch (Exception e) {
			throw new KhcInvalidDataException("Unable to parse json into " + clazz.getName(),
					Code.COMMAND_INVALID_FORMAT, e);
		}
	}

	protected String buildJson(Object param) {
		try {
			return jsonMapper.writeValueAsString(jsonMapper.convertValue(param,
					ObjectNode.class));
		} catch (Exception e) {
			throw new KhcInvalidDataException(
					"Unable to serialize object",
					Code.COMMAND_INVALID_FORMAT, e);
		}
	}
}