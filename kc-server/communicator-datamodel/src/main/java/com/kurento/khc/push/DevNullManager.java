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

package com.kurento.khc.push;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.kurento.khc.datamodel.ChannelEntity;
import com.kurento.khc.push.PushResult.Cause;

@Component("khcDevnullPush")
public class DevNullManager implements NotificationManager {

	private static final Logger log = LoggerFactory
			.getLogger(DevNullManager.class);
	private static final SecureRandom rnd = new SecureRandom();

	private Map<Long, DevNullListener> listeners = new HashMap<Long, DevNullListener>();

	private CauseGenerator causeGenerator = null;

	@Autowired
	NotificationServer notificationServer;

	@Override
	public Boolean isEnabled(ChannelEntity channel) {
		return true;
	}

	@Override
	public void sendNotification(Notification notification) {
		Cause cause;
		if (causeGenerator == null)
			cause = randomCause();
		else
			cause = causeGenerator.generateCause();

		Long channelId = notification.getChannel().getUUID();
		if (listeners.containsKey(channelId)) {
			listeners.get(channelId).push(notification);
		}

		log.debug("Send fake notification on channel {}", notification
				.getChannel().getUUID());
		notificationServer.processResult(new PushResult(notification, cause));
	}

	public void addListener(Long channelId, DevNullListener listener) {
		listeners.put(channelId, listener);
	}

	public void addCauseGenerator(CauseGenerator causeGenerator) {
		this.causeGenerator = causeGenerator;
	}

	private static Cause randomCause() {
			int code = rnd.nextInt(100);
			if (code < 90) {
				return Cause.OK;
			} else if (code < 95) {
				return Cause.RECOVERABLE;
			} else if (code < 98) {
				return Cause.SERVICE_UNAVAILABLE;
			} else {
				return Cause.INVALID_CHANNEL;
			}
	}

	public interface DevNullListener {
		void push(Notification notification);
	}

	public interface CauseGenerator {
		Cause generateCause();
	}

}
