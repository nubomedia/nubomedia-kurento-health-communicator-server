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

import org.springframework.stereotype.Component;

import com.kurento.agenda.datamodel.pojo.Account;
import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.datamodel.pojo.Content;
import com.kurento.agenda.datamodel.pojo.Group;
import com.kurento.agenda.services.pojo.AccountId;
import com.kurento.agenda.services.pojo.GroupCreate;
import com.kurento.khc.datamodel.AccountEntity;
import com.kurento.khc.datamodel.ChannelEntity;
import com.kurento.khc.datamodel.ContentEntity;
import com.kurento.khc.datamodel.GroupEntity;
import com.kurento.khc.datamodel.UserEntity;

@Component(Command.METHOD_CREATE_GROUP)
public class CmdCreateGroup extends AbstractCommand {

	@Override
	public void exec(Command command, Boolean asServer) {
		exec(command, null, asServer);
	}

	@Override
	public void exec(Command command, Content content, Boolean asServer) {

		// Parse params
		Group groupParam = parseParam(command, Command.PARAM_GROUP,
				GroupCreate.class).buildGroupPojo();
		Account accountParam = parseParam(command, Command.PARAM_ACCOUNT,
				AccountId.class).buildAccountPojo();
		ChannelEntity invoker = asServer ? null : getInvoker(command
				.getChannelId());

		GroupEntity group = administrativeService.buildGroupEntity(groupParam);
		AccountEntity account = accountDao.findAccountByUUID(accountParam
				.getId());

		// Find creating user
		UserEntity owner = securityUtils.getPrincipal();

		// Create group
		if (asServer) {
			group = groupDao.createAutomanagedGroup(group, account, owner);
		} else {
			group = groupSecureDao
					.createAutomanagedGroup(group, account, owner);
		}

		// Store picture (if provided)
		if (content != null) {
			ContentEntity avatar = contentService.buildContentEntity(content);
			ContentEntity avatarEntity = contentDao.createContent(avatar);
			groupDao.setPicture(group, avatarEntity);
		}

		sendUpdateGroup(invoker, group);
		sendUpdateGroupToAdmin(group);
	}
}
