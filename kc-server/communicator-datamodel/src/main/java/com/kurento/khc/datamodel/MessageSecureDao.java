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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class MessageSecureDao {

	@Autowired
	private MessageDao messageDao;

	@PreAuthorize(value = "@messageDao.canSendMessageToGroup(#from, #to, principal.user)")
	public MessageEntity sendMessage(MessageEntity message,
			UserEntity from, GroupEntity to, ContentEntity content) {
		return messageDao.sendMessage(message, from, to, content);
	}

	@PreAuthorize(value = "#from.id == principal.user.id"
			+ " and @userDao.canSeeContact( #to, #from)")
	public MessageEntity sendMessage(MessageEntity message,
			UserEntity from, UserEntity to, ContentEntity content) {
		return messageDao.sendMessage(message, from, to, content);
	}

	@PreAuthorize(value = "@messageDao.canUpdateMessage(#message, principal.user)")
	public MessageEntity updateMessage(MessageEntity message) {
		return messageDao.updateMessage(message);
	}

	@PostAuthorize(value = "@messageDao.canReadMessage(#message, principal.user)")
	public MessageEntity findMessageByUUID(Long messageId) {
		return messageDao.findMessageByUUID(messageId);
	}
}
