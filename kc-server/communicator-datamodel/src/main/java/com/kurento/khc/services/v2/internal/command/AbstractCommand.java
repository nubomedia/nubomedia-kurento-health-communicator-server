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

package com.kurento.khc.services.v2.internal.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.datamodel.pojo.Content;
import com.kurento.agenda.datamodel.pojo.KhcInvalidDataInfo.Code;
import com.kurento.agenda.datamodel.pojo.Timeline.State;
import com.kurento.agenda.datamodel.pojo.TimelineParty.PartyType;
import com.kurento.agenda.services.pojo.CallFwdRecv;
import com.kurento.agenda.services.pojo.CallReceive;
import com.kurento.agenda.services.pojo.GroupInfo;
import com.kurento.agenda.services.pojo.GroupUpdate;
import com.kurento.agenda.services.pojo.MessageReadResponse;
import com.kurento.agenda.services.pojo.TimelineCreate;
import com.kurento.agenda.services.pojo.TimelinePartyUpdate;
import com.kurento.agenda.services.pojo.TimelineReadResponse;
import com.kurento.agenda.services.pojo.UserId;
import com.kurento.agenda.services.pojo.UserReadAvatarResponse;
import com.kurento.agenda.services.pojo.UserReadContactResponse;
import com.kurento.agenda.services.pojo.UserReadNameResponse;
import com.kurento.agenda.services.pojo.UserReadResponse;
import com.kurento.agenda.services.pojo.topic.TopicBuilder;
import com.kurento.khc.KhcInternalServerException;
import com.kurento.khc.KhcInvalidDataException;
import com.kurento.khc.datamodel.AccountDao;
import com.kurento.khc.datamodel.CallDao;
import com.kurento.khc.datamodel.CallEntity;
import com.kurento.khc.datamodel.CallFwdDao;
import com.kurento.khc.datamodel.CallFwdEntity;
import com.kurento.khc.datamodel.CallFwdSecureDao;
import com.kurento.khc.datamodel.CallSecureDao;
import com.kurento.khc.datamodel.ChannelDao;
import com.kurento.khc.datamodel.ChannelEntity;
import com.kurento.khc.datamodel.ChannelSecureDao;
import com.kurento.khc.datamodel.CommandDao;
import com.kurento.khc.datamodel.CommandEntity;
import com.kurento.khc.datamodel.ContentDao;
import com.kurento.khc.datamodel.ConversationDao;
import com.kurento.khc.datamodel.ConversationEntity;
import com.kurento.khc.datamodel.GroupDao;
import com.kurento.khc.datamodel.GroupEntity;
import com.kurento.khc.datamodel.GroupSecureDao;
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
import com.kurento.khc.messaging.BrokerServer;
import com.kurento.khc.services.v2.AdministrativeService;
import com.kurento.khc.services.v2.CallFwdService;
import com.kurento.khc.services.v2.CallService;
import com.kurento.khc.services.v2.CommandService;
import com.kurento.khc.services.v2.ContentService;
import com.kurento.khc.services.v2.MessageService;
import com.kurento.khc.utils.KhcLogger;
import com.kurento.khc.utils.SecurityUtils;

public abstract class AbstractCommand {

	protected static Logger log = LoggerFactory
			.getLogger(AbstractCommand.class);

	@Autowired
	protected GroupSecureDao groupSecureDao;
	@Autowired
	protected UserSecureDao userSecureDao;
	@Autowired
	protected ChannelSecureDao notchSecureDao;
	@Autowired
	protected MessageSecureDao messageSecureDao;
	@Autowired
	protected TimelineSecureDao timelineSecureDao;
	@Autowired
	protected CallSecureDao callSecureDao;
	@Autowired
	protected CallFwdSecureDao callFwdSecureDao;

	@Autowired
	protected AccountDao accountDao;
	@Autowired
	protected GroupDao groupDao;
	@Autowired
	protected UserDao userDao;
	@Autowired
	protected CommandDao commandDao;
	@Autowired
	protected ChannelDao notchDao;
	@Autowired
	protected TimelineDao timelineDao;
	@Autowired
	protected ConversationDao conversationDao;
	@Autowired
	protected MessageDao messageDao;
	@Autowired
	protected ContentDao contentDao;
	@Autowired
	protected CallDao callDao;
	@Autowired
	protected CallFwdDao callFwdDao;
	@Autowired
	protected AdministrativeService administrativeService;
	@Autowired
	protected CommandService commandService;
	@Autowired
	protected MessageService messageService;
	@Autowired
	protected ContentService contentService;
	@Autowired
	protected CallService callService;
	@Autowired
	protected CallFwdService callFwdService;

	@Autowired
	protected BrokerServer brokerServer;

	@Autowired
	protected CommandServiceBackend commandBackend;

	@Autowired
	protected SecurityUtils securityUtils;

	@Autowired
	protected KhcLogger khcLog;

	@Autowired
	protected MessageSource messageSource;

	protected List<AbstractCommand> successors = new ArrayList<AbstractCommand>();

	protected static ObjectMapper jsonMapper = new KhcObjectMapper();

	public abstract void exec(Command command, Boolean asServer);

	public abstract void exec(Command command, Content content, Boolean asServer);

	public ObjectNode getParams(CommandEntity command) {
		try {
			return jsonMapper.convertValue(
					jsonMapper.readTree(commandDao.getParams(command)),
					ObjectNode.class);
		} catch (Exception e) {
			throw new KhcInternalServerException(
					"Unable to build JSON params from data stored in database",
					e);
		}
	}

	public void initializeChannel(ChannelEntity channel) {
		// This command does not provide initialization sequence
		throw new KhcInternalServerException(
				"Wrong channel initalization sequence");

	}

	public void registerInitChannelSuccessor(AbstractCommand successor) {
		successors.add(successor);
	}

	public Integer getConsolidationPriority() {
		return 0;
	}

	public List<CommandEntity> consolide(List<CommandEntity> commands) {
		return commands;
	}

	public void verifyConstraints(Command command) {
		return;
	}

	// //////////////////////////////////
	// COMMON METHODS
	// //////////////////////////////////

	protected ChannelEntity getInvoker(Long channelId) {
		if ((brokerServer.getSubscription(channelId)) != null) {
			// We only verify the user have permission to use this channel
			return new ChannelEntity();
		}
		return notchSecureDao.findNotificationChannelByUUID(channelId);
	}

	protected void sendCommand(ChannelEntity invoker, ChannelEntity receiver,
			String method, String params, String notificationMsg) {
		if (invoker == null) {
			sendCommand(receiver, receiver, method, params, notificationMsg);
		} else {
			notchDao.sendCommand(invoker, receiver, method, params,
					notificationMsg);
		}
	}

	protected void sendCommand(ChannelEntity invoker,
			List<ChannelEntity> receivers, String method, String params,
			String notificationMsg) {
		for (ChannelEntity receiver : receivers) {
			sendCommand(invoker, receiver, method, params, notificationMsg);
			log.trace("Issue command SendMessage for channel {}",
					receiver.getId());
		}
	}

	protected String buildParams(Object param) {
		try {
			return jsonMapper.writeValueAsString(jsonMapper.convertValue(param,
					ObjectNode.class));
		} catch (Exception e) {
			throw new KhcInvalidDataException(
					"Unable to serialize command parameters",
					Code.COMMAND_INVALID_FORMAT, e);
		}
	}

	// //////////////////////////////////
	// GENERAL COMMANDS
	// //////////////////////////////////

	protected void sendFactoryReset(ChannelEntity invoker) {
		sendCommand(invoker, invoker, Command.METHOD_FACTORY_RESET,
				buildParams(new HashMap<String, String>()), null);
	}

	// //////////////////////////////////
	// USER COMMANDS
	// //////////////////////////////////

	protected void sendUpdateUser(ChannelEntity invoker, UserEntity user) {
		List<ChannelEntity> receivers = userDao.getNotificationChannels(user);
		UserReadResponse userParam = administrativeService.buildUserPojo(user)
				.buildUserReadResponse();
		String method = Command.METHOD_UPDATE_USER;
		String params = buildParams(userParam);
		sendCommand(invoker, receivers, method, params, null);
	}

	protected void sendUpdateUser(ChannelEntity invoker,
			ChannelEntity receiver, UserEntity user) {
		UserReadResponse userParam = administrativeService.buildUserPojo(user)
				.buildUserReadResponse();
		sendCommand(invoker, receiver, Command.METHOD_UPDATE_USER,
				buildParams(userParam), null);
	}

	protected void sendUpdateUserToAdmin(UserEntity user) {
		UserReadResponse userParam = administrativeService.buildUserPojo(user)
				.buildUserReadResponse();
		String method = Command.METHOD_UPDATE_USER;
		String params = buildParams(userParam);
		String[] topic = new String[] { TopicBuilder.toUser(user.getUUID())
				.getTopic() };

		brokerServer.sendCommand(topic, method, params);
	}

	protected void sendUpdateContact(ChannelEntity invoker, UserEntity user) {
		List<ChannelEntity> receivers = getChannelWatchersOfContact(user);
		UserReadContactResponse userParam = administrativeService
				.buildUserPojo(user).buildUserReadContactResponse();

		String method = Command.METHOD_UPDATE_CONTACT;
		String params = buildParams(userParam);

		sendCommand(invoker, receivers, method, params, null);
		// Do not propagate this command to administrators
	}

	protected void sendUpdateContact(ChannelEntity invoker,
			ChannelEntity receiver, UserEntity user) {
		UserReadContactResponse userParam = administrativeService
				.buildUserPojo(user).buildUserReadContactResponse();
		sendCommand(invoker, receiver, Command.METHOD_UPDATE_CONTACT,
				buildParams(userParam), null);
	}

	protected void sendUpdateContact(ChannelEntity invoker,
			List<ChannelEntity> receivers, UserEntity user) {
		UserReadContactResponse userParam = administrativeService
				.buildUserPojo(user).buildUserReadContactResponse();

		String method = Command.METHOD_UPDATE_CONTACT;
		String params = buildParams(userParam);

		sendCommand(invoker, receivers, method, params, null);
		// Do not propagate this command to administrators
	}

	protected void sendDeleteContact(ChannelEntity invoker, UserEntity user,
			UserEntity watched) {
		List<ChannelEntity> receivers = userDao.getNotificationChannels(user);
		UserReadNameResponse userParam = administrativeService.buildUserPojo(
				watched).buildUserReadNameResponse();
		String method = Command.METHOD_DELETE_CONTACT;
		String params = buildParams(userParam);

		sendCommand(invoker, receivers, method, params, null);
		// Do not propagate this command to administrators
	}

	protected void sendDeleteContactAvatar(ChannelEntity invoker,
			UserEntity user) {
		List<ChannelEntity> receivers = getChannelWatchersOfContact(user);
		UserId userParam = administrativeService.buildUserPojo(user)
				.buildUserId();

		String method = Command.METHOD_DELETE_CONTACT_AVATAR;
		String params = buildParams(userParam);
		String[] topic = new String[] { TopicBuilder.toUser(user.getUUID())
				.getTopic() };

		sendCommand(invoker, receivers, method, params, null);
		// This should be in a separate command, but due to the use it doesn't
		// matter
		brokerServer.sendCommand(topic, method, params);
	}

	// //////////////////////////////////
	// GROUP COMMANDS
	// /////////////////////////////////

	protected void sendUpdateGroup(ChannelEntity invoker, GroupEntity group) {
		String method = Command.METHOD_UPDATE_GROUP;
		for (UserEntity watcher : getUserWatchersOfGroup(group)) {
			List<ChannelEntity> receivers = userDao
					.getNotificationChannels(watcher);
			GroupUpdate groupParam = buildGroupUpdateParam(group, watcher);
			sendCommand(invoker, receivers, method, buildParams(groupParam),
					null);
		}
	}

	protected void sendUpdateGroup(ChannelEntity invoker,
			ChannelEntity receiver, GroupEntity group) {
		UserEntity watcher = notchDao.getUser(receiver);
		GroupUpdate groupParam = buildGroupUpdateParam(group, watcher);
		String method = Command.METHOD_UPDATE_GROUP;
		sendCommand(invoker, receiver, method, buildParams(groupParam), null);
		// Do not propagate channel assigned commands to administrators
	}

	protected void sendUpdateGroup(ChannelEntity invoker, UserEntity receiver,
			GroupEntity group) {
		GroupUpdate groupParam = buildGroupUpdateParam(group, receiver);
		List<ChannelEntity> watchers = userDao
				.getNotificationChannels(receiver);
		String method = Command.METHOD_UPDATE_GROUP;
		sendCommand(invoker, watchers, method, buildParams(groupParam), null);
	}

	protected void sendUpdateGroupToAdmin(GroupEntity group) {
		String method = Command.METHOD_UPDATE_GROUP;
		String[] topic = new String[] {
				TopicBuilder
				.toAccount(groupDao.getGroupAccount(group).getUUID())
				.toGroups().getTopic(),
				TopicBuilder.toGroup(group.getUUID()).getTopic() };
		brokerServer.sendCommand(topic, method,
				buildParams(buildGroupUpdateParamForAdmin(group)));
	}

	private GroupUpdate buildGroupUpdateParam(GroupEntity group,
			UserEntity watcher) {
		GroupUpdate groupParam = jsonMapper.convertValue(
				administrativeService.buildGroupPojo(group), GroupUpdate.class);
		groupParam.setCanLeave(group.isAutomanaged());
		groupParam.setIsAdmin(groupDao.isGroupAdmin(group, watcher));
		return groupParam;
	}

	private GroupUpdate buildGroupUpdateParamForAdmin(GroupEntity group) {
		GroupUpdate groupParam = jsonMapper.convertValue(
				administrativeService.buildGroupPojo(group), GroupUpdate.class);
		groupParam.setCanLeave(group.isAutomanaged());
		groupParam.setCanRead(true);
		groupParam.setCanSend(false);
		groupParam.setIsAdmin(true);
		return groupParam;
	}

	protected void propagateGroupUpdate(ChannelEntity invoker, GroupEntity group) {

		sendUpdateGroup(invoker, group);
		sendUpdateGroupToAdmin(group);

		// Propagate group name change to all timelines
		for (TimelineEntity timeline : timelineDao.getGroupTimelines(group)) {
			sendUpdateTimeline(invoker, timeline);
		}
	}

	protected void sendDeleteGroup(ChannelEntity invoker, GroupEntity group) {
		List<ChannelEntity> receivers = getChannelWatchersOfGroup(group);
		GroupInfo groupParam = administrativeService.buildGroupPojo(group)
				.buildGroupInfo();

		String method = Command.METHOD_DELETE_GROUP;
		String params = buildParams(groupParam);
		sendCommand(invoker, receivers, method, params, null);
	}

	protected void sendDeleteGroup(ChannelEntity invoker,
			List<ChannelEntity> receivers, GroupEntity group) {
		GroupInfo groupParam = administrativeService.buildGroupPojo(group)
				.buildGroupInfo();
		String method = Command.METHOD_DELETE_GROUP;
		String params = buildParams(groupParam);
		sendCommand(invoker, receivers, method, params, null);
	}

	protected void sendDeleteGroupToAdmin(GroupEntity group) {
		GroupInfo groupParam = administrativeService.buildGroupPojo(group)
				.buildGroupInfo();

		String method = Command.METHOD_DELETE_GROUP;
		String params = buildParams(groupParam);
		String[] topic = new String[] {
				TopicBuilder
				.toAccount(groupDao.getGroupAccount(group).getUUID())
				.toGroups().getTopic(),
				TopicBuilder.toGroup(group.getUUID()).getTopic() };

		brokerServer.sendCommand(topic, method, params);
	}

	protected void sendGroupRelation(ChannelEntity invoker, String method,
			GroupEntity group, UserEntity member) {
		List<ChannelEntity> receivers = getChannelWatchersOfContactInGroup(
				member, group);
		sendGroupRelation(invoker, receivers, method, group, member);
	}

	protected void sendGroupRelation(ChannelEntity invoker,
			List<ChannelEntity> receivers, String method, GroupEntity group,
			UserEntity user) {

		String params = buildGroupRelationParam(group, user);
		sendCommand(invoker, receivers, method, params, null);
	}

	protected void sendGroupRelationToAdmin(String method, GroupEntity group,
			UserEntity user) {
		String[] topic = new String[] { TopicBuilder.toGroup(group.getUUID())
				.getTopic() };
		String params = buildGroupRelationParam(group, user);
		brokerServer.sendCommand(topic, method, params);
	}

	private String buildGroupRelationParam(GroupEntity group, UserEntity user) {
		UserReadAvatarResponse userParam = administrativeService.buildUserPojo(
				user).buildUserReadAvatarResponse();
		GroupInfo groupParam = administrativeService.buildGroupPojo(group)
				.buildGroupInfo();

		Map<String, Object> paramObject = new HashMap<String, Object>();
		paramObject.put(Command.PARAM_USER, userParam);
		paramObject.put(Command.PARAM_GROUP, groupParam);

		return buildParams(paramObject);
	}

	protected void sendDeleteGroupAvatar(ChannelEntity invoker,
			GroupEntity group) {
		List<ChannelEntity> receivers = getChannelWatchersOfGroup(group);
		GroupInfo groupParam = administrativeService.buildGroupPojo(group)
				.buildGroupInfo();

		String method = Command.METHOD_DELETE_GROUP_AVATAR;
		String params = buildParams(groupParam);
		String[] topic = new String[] { TopicBuilder.toGroup(group.getUUID())
				.getTopic() };

		sendCommand(invoker, receivers, method, params, null);
		// This should be in a separate command, but due to the use it doesn't
		// matter
		brokerServer.sendCommand(topic, method, params);
	}

	protected void propagateAddGroupAdmin(ChannelEntity invoker,
			GroupEntity group, UserEntity admin) {
		// Update group to promoted admin
		sendUpdateGroup(invoker, admin, group);
		sendUpdateGroupToAdmin(group);
		// Update new admin to all group members
		sendGroupRelation(invoker, Command.METHOD_ADD_GROUP_ADMIN, group, admin);
	}

	protected void propagateRemoveGroupAdmin(ChannelEntity invoker,
			GroupEntity group, UserEntity admin) {
		// Propagate updateGroup to removed admin
		sendUpdateGroup(invoker, admin, group);
		sendUpdateGroupToAdmin(group);
		// Propagate remove admin to all group members that can see it
		List<ChannelEntity> watchers = getChannelWatchersOfContactInGroup(
				admin, group);
		sendGroupRelation(invoker, watchers, Command.METHOD_REMOVE_GROUP_ADMIN,
				group, admin);
		sendGroupRelationToAdmin(Command.METHOD_REMOVE_GROUP_ADMIN, group,
				admin);
	}

	protected void propagateAddGroupMember(ChannelEntity invoker,
			GroupEntity group, UserEntity member) {
		List<ChannelEntity> receivers = userDao.getNotificationChannels(member);
		// 1.- Update group to added member & admins
		sendUpdateGroup(invoker, member, group);
		// Update current members to new member
		for (UserEntity contact : getUserWatchersOfContactInGroup(member, group)) {
			sendUpdateContact(invoker, receivers, contact);
			sendGroupRelation(invoker, receivers,
					Command.METHOD_ADD_GROUP_MEMBER, group, contact);
			if (groupDao.isGroupAdmin(group, contact)) {
				sendGroupRelation(invoker, receivers,
						Command.METHOD_ADD_GROUP_ADMIN, group, contact);
			}
		}
		sendUpdateGroupToAdmin(group);

		// 2.- Propagate commands to current members
		receivers = getChannelWatchersOfContactInGroup(member, group);
		sendUpdateContact(invoker, receivers, member);
		sendGroupRelation(invoker, receivers, Command.METHOD_ADD_GROUP_MEMBER,
				group, member);
		// 3.- Propagate change to admins
		sendGroupRelationToAdmin(Command.METHOD_ADD_GROUP_MEMBER, group, member);
	}

	protected void propagateRemoveGroupMember(ChannelEntity invoker,
			GroupEntity group, UserEntity user, List<UserEntity> watchers) {
		// 1.- Propagate deleteGroup to removed user
		sendDeleteGroup(invoker, userDao.getNotificationChannels(user), group);

		// 2.- Propagate removeGroupMember to current group members that could
		// see removed user & admins
		List<ChannelEntity> receivers = new ArrayList<ChannelEntity>();
		for (UserEntity watcher : watchers) {
			receivers.addAll(userDao.getNotificationChannels(watcher));
		}
		sendGroupRelation(invoker, receivers,
				Command.METHOD_REMOVE_GROUP_MEMBER, group, user);
		sendGroupRelationToAdmin(Command.METHOD_REMOVE_GROUP_MEMBER, group,
				user);

		// 3.- Propagate mutual deleteContact for those users that are not seen
		// anymore
		for (UserEntity member : groupDao.getGroupMembers(group)) {
			if (!userDao.canSeeContact(user, member)) {
				// Send deleteContact to member
				sendDeleteContact(invoker, member, user);
			}
			if (!userDao.canSeeContact(member, user)) {
				// Sende deleteContact to user
				sendDeleteContact(invoker, user, member);
			}
		}
	}

	// //////////////////////////////////
	// TIMELINE COMMANDS
	// /////////////////////////////////

	protected void sendUpdateTimeline(ChannelEntity invoker,
			TimelineEntity timeline) {
		UserEntity owner = timelineDao.getTimelineUser(timeline);
		List<ChannelEntity> receivers = userDao.getNotificationChannels(owner);
		TimelineReadResponse timelineParam = messageService.buildTimelinePojo(
				timeline).buildTimelineReadResponse();
		sendCommand(invoker, receivers, Command.METHOD_UPDATE_TIMELINE,
				buildParams(timelineParam), null);
	}

	protected void sendUpdateTimeline(ChannelEntity invoker,
			ChannelEntity receiver, TimelineEntity timeline) {
		TimelineReadResponse timelineParam = messageService.buildTimelinePojo(
				timeline).buildTimelineReadResponse();
		sendCommand(invoker, receiver, Command.METHOD_UPDATE_TIMELINE,
				buildParams(timelineParam), null);
	}

	protected void sendDeleteTimeline(ChannelEntity invoker,
			TimelineEntity timeline) {
		UserEntity owner = timelineDao.getTimelineUser(timeline);
		List<ChannelEntity> receivers = userDao.getNotificationChannels(owner);
		TimelineCreate timelineParam = messageService.buildTimelinePojo(
				timeline).buildTimelineCreate();
		sendCommand(invoker, receivers, Command.METHOD_DELETE_TIMELINE,
				buildParams(timelineParam), null);
	}

	protected void propagateTimelineActivation(ChannelEntity invoker,
			TimelineEntity timeline) {
		// Do not care about previous state and resync timeline
		List<MessageEntity> watchedMessages = timelineDao.getTimelineMessages(
				timeline, 40);
		propagateTimelineActivation(invoker, timeline, watchedMessages);
	}

	protected void propagateTimelineActivation(ChannelEntity invoker,
			TimelineEntity timeline, List<MessageEntity> watchedMessages) {
		// Do not care about previous state and resync timeline
		timeline.setState(State.ENABLED);
		timelineDao.updateTimeline(timeline);
		sendUpdateTimeline(invoker, timeline);

		UserEntity to = timelineDao.getTimelineUser(timeline);
		List<ChannelEntity> receivers = userDao.getNotificationChannels(to);

		/*
		 * getTimelineMessage returns messages in reverse order 1st is newest
		 * and last oldest. This sequence must be reversed to generate a correct
		 * sequence where 1st is oldest and last newest
		 */
		for (int i = watchedMessages.size() - 1; i >= 0; i--) {
			for (ChannelEntity receiver : receivers) {
				// This method does not send PUSH notification
				sendUpdateMessage(invoker, receiver, timeline,
						watchedMessages.get(i));
			}
		}
	}

	// //////////////////////////////////
	// MESSAGE COMMANDS
	// /////////////////////////////////

	protected void sendUpdateMessage(ChannelEntity invoker,
			TimelineEntity timeline, MessageEntity message) {

		MessageReadResponse messageParam = messageService.buildMessagePojo(
				message).buildMessageReadResponse();
		log.trace("Build message POJO");
		TimelinePartyUpdate timelineParam = messageService.buildTimelinePojo(
				timeline).buildTimelinePartyUpdate();
		log.trace("Build timeline POJO");
		messageParam.setTimeline(timelineParam);

		UserEntity owner = timelineDao.getTimelineUser(timeline);
		UserEntity sender = messageDao.getMessageSender(message);
		List<ChannelEntity> receivers = userDao.getNotificationChannels(owner);
		log.trace("Found receiver channels for timeline: {}", timeline.getId());

		String method = Command.METHOD_UPDATE_MESSAGE;
		String params = buildParams(messageParam);

		for (ChannelEntity receiver : receivers) {
			String msg;
			if (owner.getId().equals(sender.getId())) {
				// avoid PUSH notifications on sender channels
				msg = null;
			} else {
				// Unread messages are current badge plus this one.
				msg = messageSource.getMessage("notification.msg.template",
						new Object[] { receiver.getBadge() + 1 },
						Locale.forLanguageTag(receiver.getLocaleString()));
			}
			sendCommand(invoker, receiver, method, params, msg);
		}
	}

	// This method is used to initialize channels. Avoid sending PUSH
	// notifications
	protected void sendUpdateMessage(ChannelEntity invoker,
			ChannelEntity receiver, TimelineEntity timeline,
			MessageEntity message) {
		MessageReadResponse messageParam = messageService.buildMessagePojo(
				message).buildMessageReadResponse();
		TimelinePartyUpdate timelineParam = messageService.buildTimelinePojo(
				timeline).buildTimelinePartyUpdate();
		messageParam.setTimeline(timelineParam);

		sendCommand(invoker, receiver, Command.METHOD_UPDATE_MESSAGE,
				buildParams(messageParam), null);

	}

	protected void sendUpdateMessageToAdmin(MessageEntity message) {
		MessageReadResponse messageParam = messageService.buildMessagePojo(
				message).buildMessageReadResponse();

		String method = Command.METHOD_UPDATE_MESSAGE;
		String params = buildParams(messageParam);
		String[] topic;
		if (PartyType.GROUP.equals(message.getToType())) {
			topic = new String[] { TopicBuilder.toGroup(message.getToUUID())
					.toMessages().getTopic() };
		} else {
			topic = new String[] { TopicBuilder.toUser(message.getToUUID())
					.getTopic() };
		}
		brokerServer.sendCommand(topic, method, params);
	}

	protected void propagateUpdateMessage(ChannelEntity invoker,
			MessageEntity message) {

		ConversationEntity conversation = messageDao
				.getMessageConversation(message);
		List<TimelineEntity> timelines = messageDao
				.getMessageTimelines(message);
		List<MessageEntity> watchedMessages = null;
		log.trace("Found destination timelines for message");
		for (TimelineEntity timeline : timelines) {
			// If timeline is not active it must be activated first
			if (!timeline.getState().equals(State.ENABLED)) {
				if (watchedMessages == null) {
					watchedMessages = conversationDao.getConversationMessages(
							conversation, 40);
					// Remove first message. The one sent by command
					watchedMessages.remove(0);
				}
				propagateTimelineActivation(invoker, timeline, watchedMessages);
				log.trace("Propagate timeline activation");
			}
			// Update message just sent
			// This update might be duplicated if timeline is activated, but it
			// is required to force PUSH Notification
			sendUpdateMessage(invoker, timeline, message);
			log.trace("Message Sent to timeline: {}", timeline.getId());
		}
		// Send message update to admins
		sendUpdateMessageToAdmin(message);
	}

	// //////////////////////////////////
	// CALL COMMANDS
	// /////////////////////////////////

	protected void sendCallDialToAdmin(UserEntity to, CallEntity callEntity) {
		CallReceive callParams = callService.buildCallOfferPojo(callEntity).buildCallReceivePojo();
		String method = Command.METHOD_CALL_DIAL;
		String params = buildParams(callParams);
		String[] topic = new String[] { TopicBuilder.toUser(to.getUUID()).getTopic() };
		brokerServer.sendCommand(topic, method, params);
	}

	protected void propagateCallDial(ChannelEntity invoker, UserEntity to,
			CallEntity callEntity) {
		List<ChannelEntity> receivers = new ArrayList<ChannelEntity>();
		receivers.add(invoker);
		receivers.addAll(userDao.getNotificationChannels(to));
		CallReceive callParams = callService.buildCallOfferPojo(callEntity)
				.buildCallReceivePojo();
		String paramStr = buildParams(callParams);
		for (ChannelEntity receiver : receivers) {
			sendCommand(invoker, receiver, Command.METHOD_CALL_DIAL, paramStr,
					messageSource.getMessage("notification.calldial.template",
							null,
							Locale.forLanguageTag(receiver.getLocaleString())));
		}
		// Send message update to admins
		sendCallDialToAdmin(to, callEntity);
	}

	protected void propagateCallAccept(ChannelEntity invoker, CallEntity call) {

		ChannelEntity receiver = callDao.getToChannel(call);
		// Propagate call accept to involver channels
		List<ChannelEntity> acceptChannels = new ArrayList<ChannelEntity>();
		acceptChannels.add(callDao.getFromChannel(call));
		acceptChannels.add(receiver);
		CallReceive callParams = callService.buildCallAnswerPojo(call)
				.buildCallReceivePojo();
		for (ChannelEntity ac : acceptChannels) {
			sendCommand(invoker, ac, Command.METHOD_CALL_ACCEPT,
					buildParams(callParams), messageSource.getMessage(
							"notification.callaccept.template", null,
							Locale.forLanguageTag(ac.getLocaleString())));
		}

		// Propagate call termination to the rest of channels
		UserEntity to = notchDao.getUser(receiver);
		List<ChannelEntity> terminateChannels = userDao
				.getNotificationChannels(to);
		terminateChannels.remove(receiver);
		callParams = callService.buildCallTerminatePojo(call)
				.buildCallReceivePojo();
		for (ChannelEntity tc : terminateChannels) {
			sendCommand(invoker, tc, Command.METHOD_CALL_TERMINATE,
					buildParams(callParams), messageSource.getMessage(
							"notification.callterminate.template", null,
							Locale.forLanguageTag(tc.getLocaleString())));
		}
	}

	protected void propagateRingingCallTermination(ChannelEntity invoker,
			CallEntity call) {
		List<ChannelEntity> terminateChannels = new ArrayList<ChannelEntity>();
		terminateChannels.add(callDao.getFromChannel(call));
		terminateChannels.addAll(userDao.getNotificationChannels(callDao
				.getToUser(call)));
		CallReceive callParams = callService.buildCallTerminatePojo(call)
				.buildCallReceivePojo();
		for (ChannelEntity tc : terminateChannels) {
			sendCommand(invoker, tc, Command.METHOD_CALL_TERMINATE,
					buildParams(callParams), messageSource.getMessage(
							"notification.callterminate.template", null,
							Locale.forLanguageTag(tc.getLocaleString())));
		}
	}

	protected void propagateConfirmedCallTermination(ChannelEntity invoker,
			CallEntity call) {
		List<ChannelEntity> terminateChannels = new ArrayList<ChannelEntity>();
		terminateChannels.add(callDao.getFromChannel(call));
		terminateChannels.add(callDao.getToChannel(call));
		CallReceive callParams = callService.buildCallTerminatePojo(call)
				.buildCallReceivePojo();
		for (ChannelEntity tc : terminateChannels) {
			sendCommand(invoker, tc, Command.METHOD_CALL_TERMINATE,
					buildParams(callParams), messageSource.getMessage(
							"notification.callterminate.template", null,
							Locale.forLanguageTag(tc.getLocaleString())));
		}

	}

	protected void propagateMuteCall(ChannelEntity invoker, CallEntity call, UserEntity muteTo) {
		ChannelEntity toChannel = null;
		UserEntity muteFrom = null;
		if (muteTo.getId().equals(callDao.getFromUser(call).getId())) {
			toChannel = callDao.getFromChannel(call);
			muteFrom = callDao.getToUser(call);
		}
		if (muteTo.getId().equals(callDao.getToUser(call).getId())) {
			toChannel = callDao.getToChannel(call);
			muteFrom = callDao.getFromUser(call);
		}
		CallReceive callParams = callService.buildCallTerminatePojo(call)
				.buildCallReceivePojo();
		callParams.setTo(muteTo.getUUID());
		callParams.setFrom(administrativeService.buildUserPojo(muteFrom)
				.buildUserReadAvatarResponse());

		String notifMsg = messageSource.getMessage(
				"notification.callmute.template", null,
				Locale.forLanguageTag(toChannel.getLocaleString()));
		sendCommand(invoker, toChannel, Command.METHOD_CALL_MUTE,
				buildParams(callParams), notifMsg);
	}

	protected void propagateCallFwdSetup(ChannelEntity invoker, CallFwdEntity fwdEntity) {
		List<ChannelEntity> receivers = new ArrayList<ChannelEntity>();
		receivers.add(invoker);
		receivers.add(callFwdDao.getFromChannel(fwdEntity));
		receivers.add(callFwdDao.getToChannel(fwdEntity));
		CallFwdRecv callFwdParams = callFwdService.buildCallFwdSetupPojo(fwdEntity)
				.buildCallFwdRecvPojo();
		String paramStr = buildParams(callFwdParams);
		for (ChannelEntity receiver : receivers) {
			sendCommand(invoker, receiver, Command.METHOD_CALL_FWD_SETUP,
					paramStr, null);
		}
	}

	protected void propagateCallFwdAck(ChannelEntity invoker,
			CallFwdEntity fwdEntity) {
		List<ChannelEntity> receivers = new ArrayList<ChannelEntity>();
		receivers.add(invoker);
		receivers.add(callFwdDao.getFromChannel(fwdEntity));
		// send notifications to fwd dispatcher's channels
		List<ChannelEntity> dispatcherChannels = userDao
				.getNotificationChannels(callFwdDao.getDispatchUser(fwdEntity));
		receivers.addAll(dispatcherChannels);

		CallFwdRecv callFwdParams = callFwdService.buildCallFwdAckPojo(
				fwdEntity).buildCallFwdRecvPojo();
		String paramStr = buildParams(callFwdParams);
		for (ChannelEntity receiver : receivers) {
			sendCommand(invoker, receiver, Command.METHOD_CALL_FWD_ACK,
					paramStr, null);
		}
	}

	protected void propagateCallFwdEstablished(ChannelEntity invoker,
			CallFwdEntity fwdEntity) {
		List<ChannelEntity> receivers = new ArrayList<ChannelEntity>();
		receivers.add(invoker);
		receivers.add(callFwdDao.getFromChannel(fwdEntity));
		//send notifications to fwd dispatcher's channels
		List<ChannelEntity> dispatcherChannels = userDao
				.getNotificationChannels(callFwdDao.getDispatchUser(fwdEntity));
		receivers.addAll(dispatcherChannels);

		CallFwdRecv callFwdParams = callFwdService.buildCallFwdEstablishedPojo(fwdEntity)
				.buildCallFwdRecvPojo();
		String paramStr = buildParams(callFwdParams);
		for (ChannelEntity receiver : receivers) {
			sendCommand(invoker, receiver, Command.METHOD_CALL_FWD_ESTABLISHED,
					paramStr, null);
		}
	}


	protected void propagateCallFwdTerminated(ChannelEntity invoker,
			CallFwdEntity fwdEntity) {
		List<ChannelEntity> receivers = new ArrayList<ChannelEntity>();
		receivers.add(callFwdDao.getFromChannel(fwdEntity));
		receivers.add(callFwdDao.getToChannel(fwdEntity));
		//send notifications to fwd dispatcher's channels
		List<ChannelEntity> dispatcherChannels = userDao
				.getNotificationChannels(callFwdDao.getDispatchUser(fwdEntity));
		receivers.addAll(dispatcherChannels);

		CallFwdRecv callFwdParams = callFwdService.buildCallFwdTerminatedPojo(fwdEntity)
				.buildCallFwdRecvPojo();
		String paramStr = buildParams(callFwdParams);
		for (ChannelEntity receiver : receivers) {
			sendCommand(invoker, receiver, Command.METHOD_CALL_FWD_TERMINATED,
					paramStr, null);
		}
	}

	// /////////////////
	//
	// HELPERS
	//
	// /////////////////

	/*
	 * Regard params as a single string and try to build a POJO of given classs
	 */
	protected <T> T parseParam(Command command, Class<T> clazz) {
		ObjectNode params = jsonMapper.convertValue(command.getParams(),
				ObjectNode.class);
		try {
			return jsonMapper.readValue(params, clazz);
		} catch (Exception e) {
			throw new KhcInvalidDataException("Unable to parse command params",
					Code.COMMAND_INVALID_FORMAT, e);
		}
	}

	/*
	 * intended for multiparam commands
	 */
	protected <T> T parseParam(Command command, String paramName, Class<T> clazz) {
		ObjectNode params = jsonMapper.valueToTree(command.getParams());
		try {
			return jsonMapper.readValue(params.get(paramName), clazz);
		} catch (Exception e) {
			throw new KhcInvalidDataException("Unable to parse command params"
					+ paramName, Code.COMMAND_INVALID_FORMAT, e);
		}
	}

	// //////////////////////////////////////
	// Propagation helpers
	// //////////////////////////////////////

	/*
	 * Find all watchers in all groups for a given contact. Who can see any
	 * user's role
	 */
	protected List<ChannelEntity> getChannelWatchersOfContact(UserEntity user) {
		List<ChannelEntity> watchers = new ArrayList<ChannelEntity>();
		for (GroupEntity group : userDao.getUserGroups(user)) {
			watchers.addAll(getChannelWatchersOfContactInGroup(user, group));
		}
		return watchers;
	}

	/*
	 * Find watchers in one groups for a given contact = What members of a group
	 * can see any user's role, except requester user
	 */
	protected List<ChannelEntity> getChannelWatchersOfContactInGroup(
			UserEntity user, GroupEntity group) {
		List<ChannelEntity> watchers = new ArrayList<ChannelEntity>();
		for (UserEntity member : getUserWatchersOfContactInGroup(user, group)) {
			watchers.addAll(userDao.getNotificationChannels(member));
		}
		return watchers;
	}

	protected List<UserEntity> getUserWatchersOfContactInGroup(UserEntity user,
			GroupEntity group) {
		List<UserEntity> watchers = new ArrayList<UserEntity>();
		for (UserEntity member : groupDao.getGroupMembers(group)) {
			if (!member.getId().equals(user.getId())) {
				watchers.add(member);
			}
		}
		return watchers;

	}

	/* Find who must be noticed in administrative changes in group members */
	protected List<UserEntity> getUserWatchersOfGroup(GroupEntity group) {
		List<UserEntity> watchers = new ArrayList<UserEntity>();
		watchers.addAll(groupDao.getGroupMembers(group));
		return watchers;
	}

	protected List<ChannelEntity> getChannelWatchersOfGroup(GroupEntity group) {
		List<ChannelEntity> watchers = new ArrayList<ChannelEntity>();
		// List of members can see
		for (UserEntity user : getUserWatchersOfGroup(group)) {
			watchers.addAll(userDao.getNotificationChannels(user));
		}
		return watchers;
	}

	/*
	 * Find the list of contacts that can see a given user in all the groups
	 * where is member
	 */
	protected List<UserEntity> getWatchedContactsOfUser(UserEntity user) {

		Map<Long, UserEntity> memberTable = new HashMap<Long, UserEntity>();
		for (GroupEntity watchedGroup : userDao.getUserGroups(user)) {
			for (UserEntity member : groupDao.getGroupMembers(watchedGroup)) {
				if (!member.getId().equals(user.getId())) {
					memberTable.put(member.getId(), member);
				}
			}
		}
		return new ArrayList<UserEntity>(memberTable.values());
	}
}
