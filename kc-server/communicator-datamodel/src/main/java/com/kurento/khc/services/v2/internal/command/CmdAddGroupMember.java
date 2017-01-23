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

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.datamodel.pojo.Content;
import com.kurento.agenda.datamodel.pojo.Group;
import com.kurento.agenda.datamodel.pojo.User;
import com.kurento.agenda.services.pojo.GroupInfo;
import com.kurento.agenda.services.pojo.UserReadAvatarResponse;
import com.kurento.khc.KhcException;
import com.kurento.khc.datamodel.ChannelEntity;
import com.kurento.khc.datamodel.GroupEntity;
import com.kurento.khc.datamodel.UserEntity;

@Component(Command.METHOD_ADD_GROUP_MEMBER)
public class CmdAddGroupMember extends AbstractCommand {

	@Autowired
	public void setAntecessor(CmdUpdateContact updateContact) {
		updateContact.registerInitChannelSuccessor(this);
	}

	@Override
	@Transactional
	public void exec(Command command, Boolean asServer) {
		// Get params
		User userParam = parseParam(command, Command.PARAM_USER,
				UserReadAvatarResponse.class).buildUserPojo();
		Group groupParam = parseParam(command, Command.PARAM_GROUP,
				GroupInfo.class).buildGroupPojo();
		ChannelEntity invoker = asServer ? null : getInvoker(command
				.getChannelId());
		UserEntity member = userDao.findUserByUUID(userParam.getId());
		GroupEntity group = groupDao.findGroupByUUID(groupParam.getId());

		// Execute command. User is added as member when added as admin
		if (asServer) {
			groupDao.addGroupMember(group, member);
		} else {
			groupSecureDao.addGroupMember(group, member);
		}
		propagateAddGroupMember(invoker, group, member);

	}

	@Override
	@Transactional
	public void exec(Command command, Content content, Boolean asServer) {
		exec(command, asServer);
	}

	@Override
	public void initializeChannel(ChannelEntity channel) {

		log.debug("Initialize channel: Update group members");

		// Update all user memberships that must be seen by this channel
		UserEntity user = notchDao.getUser(channel);

		for (GroupEntity group : userDao.getUserGroups(user)) {
			List<UserEntity> members;
			try {
				// If admin get all members
				members = groupDao.getGroupMembers(group);

				// Update contact for each seen group member
				for (UserEntity member : members) {
					// Do not update myself
					if (member.getId().equals(user.getId())) {
						continue;
					}
					sendGroupRelation(channel, Arrays.asList(channel),
							Command.METHOD_ADD_GROUP_MEMBER, group, member);
				}
			} catch (KhcException e) {
				log.warn("Unable to synchronize group:" + group.getUUID(), e);
			}
		}

		// Call all successors
		for (AbstractCommand cmd : successors) {
			cmd.initializeChannel(channel);
		}
	}
}
