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

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.datamodel.pojo.Content;
import com.kurento.agenda.services.pojo.UserSearchLocalContact;
import com.kurento.khc.datamodel.ChannelEntity;
import com.kurento.khc.datamodel.UserEntity;

@Component(Command.METHOD_SEARCH_LOCAL_CONTACT)
public class CmdSearchLocalContact extends AbstractCommand {

	@Override
	public void exec(Command command, Boolean asServer) {

		// Get params
		UserSearchLocalContact localContactList = parseParam(command,
				UserSearchLocalContact.class);

		ChannelEntity invoker = asServer ? null : getInvoker(command
				.getChannelId());

		// Execute commands: Search local contacts
		UserEntity user = notchDao.getUser(invoker);
		List<ChannelEntity> receivers = userDao.getNotificationChannels(user);
		for (String phone : localContactList.getLocalPhones()) {
			try {
				UserEntity contact = userDao.findUserByPhone(phone,
						user.getPhoneRegion());
				// Verify is not myself and is not a local contact yet and
				// belongs to the same account
				if (!user.getId().equals(contact.getId())
						&& !userDao.hasLocalContact(user, contact)
						&& userDao
								.getUserAccount(user)
								.getId()
								.equals(userDao.getUserAccount(contact).getId())) {
					// Add local contact
					userDao.addLocalContact(user, contact);
					sendUpdateContact(invoker, receivers, contact);
				}
			} catch (Exception e) {
				// Try next number
				continue;
			}
		}
	}

	@Override
	public void exec(Command command, Content content, Boolean asServer) {
		exec(command, asServer);
	}

}
