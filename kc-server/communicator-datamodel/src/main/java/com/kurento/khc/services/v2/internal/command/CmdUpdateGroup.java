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
import com.kurento.agenda.datamodel.pojo.Group;
import com.kurento.khc.KhcException;
import com.kurento.khc.datamodel.ChannelEntity;
import com.kurento.khc.datamodel.ContentEntity;
import com.kurento.khc.datamodel.GroupEntity;
import com.kurento.khc.datamodel.UserEntity;

@Component(Command.METHOD_UPDATE_GROUP)
public class CmdUpdateGroup extends AbstractCommand {

	@Autowired
	public void setAntecessor(CmdFactoryReset factoryReset) {
		factoryReset.registerInitChannelSuccessor(this);
	}

	@Override
	@Transactional
	public void exec(Command command, Boolean asServer) {
		exec(command, null, asServer);
	}

	@Override
	@Transactional
	public void exec(Command command, Content content, Boolean asServer) {

		// Execute command
		GroupEntity group = administrativeService.buildGroupEntity(parseParam(
				command, Group.class));
		ChannelEntity invoker = asServer ? null : getInvoker(command
				.getChannelId());
		if (asServer) {
			group = groupDao.updateGroup(group);
		} else {
			group = groupSecureDao.updateGroup(group);
		}

		if (content != null) {
			ContentEntity newPic = contentService.buildContentEntity(content);
			contentDao.createContent(newPic);
			groupDao.setPicture(group, newPic);
		}

		propagateGroupUpdate(invoker, group);
	}

	@Override
	public void initializeChannel(ChannelEntity channel) {
		log.debug("Initialize channel => Update groups");
		UserEntity owner = notchDao.getUser(channel);
		List<GroupEntity> watchedGroups = userDao.getUserGroups(owner);

		for (GroupEntity group : watchedGroups) {
			// send update group command to initialized channel
			try {
				sendUpdateGroup(channel, channel, group);
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
