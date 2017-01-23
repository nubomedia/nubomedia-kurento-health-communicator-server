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
public class ChannelSecureDao {

	@Autowired
	ChannelDao notificationChannelDao;

	@PreAuthorize("#user.id == principal.user.id")
	public ChannelEntity createNotificationChannel(ChannelEntity notch,
			UserEntity user) {
		return notificationChannelDao.createNotificationChannel(notch, user);
	}

	@PreAuthorize("@channelDao.isOwner(#notch, principal.user)")
	public void deleteNotificationChannel(ChannelEntity notch) {
		notificationChannelDao.deleteNotificationChannel(notch);
	}

	@PreAuthorize("@channelDao.isOwner(#notch, principal.user)")
	public void updateNotificationChannel(ChannelEntity notch) {
		notificationChannelDao.updateNotificationChannel(notch);
	}

	@PostAuthorize("@channelDao.isOwner(returnObject, principal.user)")
	public ChannelEntity findNotificationChannelByUUID(Long uuid) {
		return notificationChannelDao.findNotificationChannelByUUID(uuid);
	}
}
