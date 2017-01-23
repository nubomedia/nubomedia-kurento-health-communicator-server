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

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.datamodel.pojo.Content;
import com.kurento.agenda.datamodel.pojo.Message;
import com.kurento.agenda.datamodel.pojo.TimelineParty.PartyType;
import com.kurento.agenda.services.pojo.MessageSend;
import com.kurento.khc.datamodel.ChannelEntity;
import com.kurento.khc.datamodel.ContentEntity;
import com.kurento.khc.datamodel.MessageEntity;
import com.kurento.khc.datamodel.UserEntity;

@Component(Command.METHOD_SEND_MESSAGE_TO_USER)
public class CmdSendMessageToUser extends AbstractCommand {

	@Override
	@Transactional
	public void exec(Command command, Boolean asServer) {
		exec(command, null, asServer);
	}

	@Override
	@Transactional
	public void exec(Command command, Content content, Boolean asServer) {

		// Get params
		MessageSend msgSend = parseParam(command, MessageSend.class);
		Message messageParam = msgSend.buildMessagePojo();

		ChannelEntity invoker = asServer ? null : getInvoker(command
				.getChannelId());

		UserEntity from = userDao
				.findUserByUUID(messageParam.getFrom().getId());
		UserEntity to = userDao.findUserByUUID(messageParam.getTo());

		ContentEntity contentEntity = null;
		if (content != null) {
			contentEntity = contentService.buildContentEntity(content);
			contentEntity = contentDao.createContent(contentEntity);
		}

		MessageEntity message = messageService.buildMessageEntity(messageParam);
		message.setToUUID(messageParam.getTo());
		message.setToType(PartyType.GROUP);
		if (asServer) {
			messageDao.sendMessage(message, from, to, contentEntity);
		} else {
			messageSecureDao.sendMessage(message, from, to,
					contentEntity);
		}
		// Add sender as local contact of receiver if it is not already
		if (!userDao.hasLocalContact(to, from)) {
			userDao.addLocalContact(to, from);
			List<ChannelEntity> receivers = userDao.getNotificationChannels(to);
			sendUpdateContact(invoker, receivers, from);
		}
		// Propagate message
		propagateUpdateMessage(invoker, message);

		khcLog.getLog("FinishedMsgReceiving")
				.withChannelId(command.getChannelId())
				.withMessageLocalId(messageParam.getLocalId())
				.withMessageId(message.getUUID())
				.from(messageParam.getFrom().getId()).to(messageParam.getTo())
				.send();
	}

}
