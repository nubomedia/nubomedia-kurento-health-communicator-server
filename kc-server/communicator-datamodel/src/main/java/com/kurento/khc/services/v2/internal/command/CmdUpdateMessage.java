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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.datamodel.pojo.Content;
import com.kurento.agenda.datamodel.pojo.Message;
import com.kurento.agenda.datamodel.pojo.Timeline.State;
import com.kurento.agenda.services.pojo.MessageUpdate;
import com.kurento.khc.KhcException;
import com.kurento.khc.datamodel.ChannelEntity;
import com.kurento.khc.datamodel.MessageEntity;
import com.kurento.khc.datamodel.TimelineEntity;
import com.kurento.khc.datamodel.UserEntity;

@Component(Command.METHOD_UPDATE_MESSAGE)
public class CmdUpdateMessage extends AbstractCommand {

	@Autowired
	public void setAntecessor(CmdUpdateTimeline updateTimeline) {
		updateTimeline.registerInitChannelSuccessor(this);
	}

	@Override
	@Transactional
	public void exec(Command command, Boolean asServer) {
		// Get params
		MessageUpdate msgUpdate = parseParam(command, MessageUpdate.class);
		Message messageParam = msgUpdate.buildMessagePojo();
		ChannelEntity invoker = asServer ? null : getInvoker(command
				.getChannelId());

		MessageEntity message = messageDao.findMessageByUUID(messageParam
				.getId());
		if (messageParam.getApp() != null) //app is similar to cmd's params
			message.setPayload(buildParams(messageParam.getApp()));
		if (asServer) {
			messageDao.updateMessage(message);
		} else {
			messageSecureDao.updateMessage(message);
		}
		propagateUpdateMessage(invoker, message);
	}

	@Override
	@Transactional
	public void exec(Command command, Content content, Boolean asServer) {
		exec(command, asServer);
	}

	@Override
	public void initializeChannel(ChannelEntity channel) {

		log.debug("Initialize channel: Update messages");

		// Get channel owner
		UserEntity owner = notchDao.getUser(channel);

		// Get timelines
		List<TimelineEntity> timelines = userDao.getUserTimelines(owner);

		// Get messages for each enabled timeline
		for (TimelineEntity timeline : timelines) {
			try {
				if (timeline.getState().equals(State.ENABLED)) {
					/*
					 * getTimelineMessage returns messages in reverse order 1st
					 * is newest and last oldest. This sequence must be reversed
					 * to generate a correct sequence where 1st is oldest and
					 * last newest
					 */
					List<MessageEntity> watchedMessages = timelineDao
							.getTimelineMessages(timeline, 40);
					// Send command to update messages in reverse order
					// New messages first
					for (int i = watchedMessages.size() - 1; i >= 0; i--) {
						sendUpdateMessage(channel, channel, timeline,
								watchedMessages.get(i));
					}
				}
			} catch (KhcException e) {
				log.warn(
						"Unable to synchronize timeline:" + timeline.getUUID(),
						e);
				timelineDao.deleteTimeline(timeline);
			}
		}
	}

}
