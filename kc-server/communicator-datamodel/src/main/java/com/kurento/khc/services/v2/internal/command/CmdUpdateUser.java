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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.datamodel.pojo.Content;
import com.kurento.agenda.datamodel.pojo.KhcInvalidDataInfo.Code;
import com.kurento.agenda.datamodel.pojo.User;
import com.kurento.agenda.services.pojo.UserEdit;
import com.kurento.khc.KhcInvalidDataException;
import com.kurento.khc.KhcNotFoundException;
import com.kurento.khc.datamodel.ChannelEntity;
import com.kurento.khc.datamodel.ContentEntity;
import com.kurento.khc.datamodel.UserEntity;
import com.kurento.khc.qos.QosServer;

@Component(Command.METHOD_UPDATE_USER)
public class CmdUpdateUser extends AbstractCommand {

	@Autowired
	private QosServer qosServer;

	@Override
	@Transactional
	public void exec(Command command, Boolean asServer) {
		exec(command, null, asServer);
	}

	@Override
	@Transactional
	public void exec(Command command, Content content, Boolean asServer) {

		// Get params
		User userParam = parseParam(command, UserEdit.class).buildUserPojo();

		UserEntity user = administrativeService.buildUserEntity(userParam);
		ChannelEntity invoker = asServer ? null : getInvoker(command
				.getChannelId());

		if (asServer) {
			user = userDao.updateUser(user);
		} else {
			user = userSecureDao.updateUser(user);
		}

		// Update avatar
		if (content != null) {
			ContentEntity newPic = contentService.buildContentEntity(content);
			contentDao.createContent(newPic);
			userDao.setPicture(user, newPic);
		}
		// Propagate command to watchers
		Boolean publicInfo = userParam.getPhone() != null
				|| userParam.getName() != null
				|| userParam.getSurname() != null || content != null;
		Boolean privateInfo = publicInfo || userParam.getEmail() != null
				|| userParam.getQos() != null;

		// Notifiy all user's channels if private information has been modified
		if (privateInfo) {
			sendUpdateUser(invoker, user);
		}

		// Notify to all "cansee" users if public information has been modified
		if (publicInfo) {
			sendUpdateContact(invoker, user);
		}

		// Notify administrators
		sendUpdateUserToAdmin(user);
	}

	@Override
	// This method has to be here bec
	public void verifyConstraints(Command command) {
		// Verify if error has to do with user constraints
		UserEdit user = jsonMapper.convertValue(command.getParams(),
				UserEdit.class);
		// 1.- email
		if (user.getEmail() != null) {
			try {
				userDao.findUserByEmail(user.getEmail());
				throw new KhcInvalidDataException("Email already registered",
						Code.EMAIL_ALREADY_USED);
			} catch (KhcNotFoundException e1) {
				// Do nothing
			}
		}
		// 2.- phone
		if (user.getPhone() != null) {
			try {
				userDao.findUserByPhone(user.getPhone(), user.getPhoneRegion());
				throw new KhcInvalidDataException("Phone already registered",
						Code.PHONE_ALREADY_USED);
			} catch (KhcNotFoundException e1) {
				// Do nothing
			}
		}
	}

}
