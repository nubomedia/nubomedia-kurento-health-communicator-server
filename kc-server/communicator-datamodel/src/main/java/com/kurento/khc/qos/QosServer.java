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

package com.kurento.khc.qos;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.kurento.khc.datamodel.ChannelDao;
import com.kurento.khc.datamodel.ChannelEntity;
import com.kurento.khc.push.Notification;
import com.kurento.khc.push.NotificationServer;

@Configuration
@EnableScheduling
public class QosServer {
	private static final Logger log = LoggerFactory.getLogger(QosServer.class);

	@Value("${kurento.qos.notification-delay-milliseconds:#{null}}")
	private Long QOS_DELAY_MILLISECONDS = 120000L;

	private ConcurrentMap<String, ChannelEntity> qosInstances = new ConcurrentHashMap<String, ChannelEntity>();

	@Autowired
	private NotificationServer notificationServer;

	@Autowired
	private ChannelDao notchDao;

	@PostConstruct
	private void init() {
		reloadInstances();
	}

	@Scheduled(fixedDelayString = "${kurento.qos.notification-delay-milliseconds:120000}")
	protected void sendNotifications() {
		log.trace("Sending QoS notifications...");
		for (ChannelEntity c : qosInstances.values()) {
			//requires a msg to be a valid notification
			Notification notification = new Notification(c,
					Long.toString(QOS_DELAY_MILLISECONDS), null);
			notification.setQos(true);
			notification.setNew(false); //never send using websocket
			notificationServer.sendNotification(notification);
			log.debug("Sent QoS notification ({}) on channel {}",
					notification.getChannel().getRegisterType(),
					notification.getChannel().getUUID());
		}
	}

	public void reviewQosInstance(ChannelEntity newNotch, boolean qos) {
		String instanceId = newNotch.getInstanceId();
		if (instanceId == null) {
			log.warn("Channel {} has no instance ID, unable to review Qos instances",
					newNotch.getUUID());
			return;
		}
		String action = "no action on";
		if (qos) {
			if (qosInstances.putIfAbsent(instanceId, newNotch) != null) {
				qosInstances.replace(instanceId, newNotch);
				action = "replaced";
			} else
				action = "added";
		} else {
			if (qosInstances.remove(instanceId) != null)
				action = "removed";
		}
		log.info("QoS: {} channel {}, instance {}", action,
				newNotch.getUUID(), instanceId);
	}

	public void removeAllInstances() {
		log.info("Removing existing instances ({} channels)",
				qosInstances.size());
		qosInstances.clear();
	}

	public void reloadInstances() {
		removeAllInstances();
		try {
			List<ChannelEntity> qosChannels = notchDao.findNotificationChannelsByQoS();
			if (qosChannels.isEmpty()) {
				log.info("No existing qos channels on init");
				return;
			}
			log.debug("Adding {} instances from channels of qos-enabled users",
					qosChannels.size());
			for (ChannelEntity c : qosChannels)
				reviewQosInstance(c, true);
			log.info("Added {} qos instances on app init", qosChannels.size());
		} catch (Exception e) {
			log.error("Populating the qos instances from existing qos channels", e);
		}
	}
}
