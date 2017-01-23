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

package com.kurento.khc.services.v2.internal;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kurento.khc.datamodel.ChannelSecureDao;
import com.kurento.khc.services.v2.LogService;
import com.kurento.khc.utils.FileRepository;

@Service
public class LogServiceImpl implements LogService {

	@Autowired
	private ChannelSecureDao notchSecureDao;

	@Autowired
	private FileRepository repo;

	private Long index = 0L;

	@Override
	public void log(Long channelId, String log) throws IOException {
		// Check requester has permission to log
		notchSecureDao.findNotificationChannelByUUID(channelId);

		// Generate file name
		String filename = channelId + "-" + System.currentTimeMillis() + "-"
				+ getIndex();
		repo.uploadMedia(filename, log);

	}

	synchronized private Long getIndex() {
		return ++index;
	}
}
