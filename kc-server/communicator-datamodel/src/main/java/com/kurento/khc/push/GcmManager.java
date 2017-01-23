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

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.google.android.gcm.server.InvalidRequestException;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.kurento.agenda.datamodel.pojo.Channel;
import com.kurento.agenda.services.pojo.UserCreate;
import com.kurento.khc.datamodel.ChannelDao;
import com.kurento.khc.datamodel.ChannelEntity;

@Component("khcGcmPush")
public class GcmManager implements NotificationManager {

	private static final int GCM_MAX_RETRIES = 3;

	private static final String GCM_ERR_UNAVAILABLE = "Unavailable";
	private static final String GCM_ERR_NOT_REGISTERED = "NotRegistered";

	private static final String GCM_ERR_INVALID_REGISTRATION = "InvalidRegistration";

	private static Logger log = LoggerFactory.getLogger(GcmManager.class);

	// GCM channel
	private Boolean isGcmEnabled = false;
	private Sender gcmSender;
	@Value(value = "${kurento.gcm.key:#{null}}")
	private String GCM_AUTHORIZATION_KEY = "";

	@Autowired
	private ChannelDao notificationChannelDao;
	@Autowired
	private NotificationServer notificationServer;

	@PostConstruct
	public void init() {
		isGcmEnabled = false;
		if (!GCM_AUTHORIZATION_KEY.isEmpty()) {
			gcmSender = new Sender(GCM_AUTHORIZATION_KEY);
			isGcmEnabled = true;
		} else {
			log.error("GCM Authorization Key not available. GCM disabled");
			return;
		}
	}

	@Override
	public Boolean isEnabled(ChannelEntity channel) {
		String channelType = channel.getRegisterType();
		return Channel.GCM.equals(channelType)
				&& channel.isEnabled();
	}

	@Override
	public void sendNotification(Notification notification) {
		Assert.notNull(notification);
		ChannelEntity channel = notification.getChannel();
		String msg = notification.getMsg();
		if (channel.getRegisterId() != null
				&& !channel.getRegisterId().isEmpty()) {
			Message gcmMessage = null;
			String qosMsg = "";
			if (!notification.isQos()) {
				gcmMessage = new Message.Builder().addData("msg", msg).build();
			} else {
				gcmMessage = new Message.Builder().addData(
						UserCreate.QOS_GCM_DATA_KEY, msg).build();
				qosMsg = "for QoS ";
			}
			try {
				log.trace("Sending GCM notification {}to channel {}",
						qosMsg, channel.getUUID());
				Result gcmResult = gcmSender.send(gcmMessage,
						channel.getRegisterId(), GCM_MAX_RETRIES);
				notificationServer.processResult(verifyGcmResult(gcmResult,
						notification));
			} catch (InvalidRequestException e) {
				// Manage error
				int status = e.getHttpStatusCode();
				if (status == 401) {
					log.error("Discard notifications due to GCM authentication error: "
							+ status);
					isGcmEnabled = false; // Disable GCM
					notificationServer.processResult(new PushResult(
							notification, PushResult.Cause.FAILED));
				} else if (status == 503) {
					String errMsg = "Notification reschedule";
					if (notification.isQos()) {
						errMsg = "QoS Notification failed";
						notificationServer
								.processResult(new PushResult(notification,
										PushResult.Cause.FAILED));
					} else
						notificationServer
								.processResult(new PushResult(notification,
										PushResult.Cause.SERVICE_UNAVAILABLE));
					log.warn("{} due to GCM server error: {}", errMsg, status);
				} else {
					log.error("Discard notification due to GCM HTTP error:"
							+ status);
					notificationServer.processResult(new PushResult(
							notification, PushResult.Cause.FAILED));
				}

			} catch (IOException e) {
				log.error("IO error while connecting {}to GCM server", qosMsg, e);
				if (notification.isQos())
					// qos => no reschedule
					notificationServer
							.processResult(new PushResult(notification,
									PushResult.Cause.FAILED));
				else
					// Connection problem ==> Reschedule notification
					notificationServer.processResult(new PushResult(notification,
							PushResult.Cause.SERVICE_UNAVAILABLE));
			}
		}
	}

	private PushResult verifyGcmResult(Result result, Notification notification) {

		// Check if message is processed
		String msgId = result.getMessageId();
		PushResult pushResult;
		ChannelEntity notch = notification.getChannel();
		if (msgId != null) {
			// Message processed
			pushResult = new PushResult(notification, PushResult.Cause.OK);
			if (result.getCanonicalRegistrationId() != null) {
				try {
					// Replace registration ID with canonical (if provided)
					notch.setRegisterId(result.getCanonicalRegistrationId());
					notificationChannelDao.updateNotificationChannel(notch);
				} catch (Exception e) {
					log.error("Unable to update register ID for notification channel: "
							+ notch.getId());
				}
			}
		} else {
			// NULL ==> Error occurred
			String errCode = result.getErrorCodeName();
			if (GCM_ERR_UNAVAILABLE.equalsIgnoreCase(errCode)
					&& !notification.isQos()) { // qos => not recoverable
				pushResult = new PushResult(notification,
						PushResult.Cause.RECOVERABLE);
			} else if (GCM_ERR_NOT_REGISTERED.equalsIgnoreCase(errCode)) {
				log.warn("Channel not registered in GCM. channelId="
						+ notch.getRegisterId());
				pushResult = new PushResult(notification,
						PushResult.Cause.INVALID_CHANNEL);
			} else if (GCM_ERR_INVALID_REGISTRATION.equalsIgnoreCase(errCode)) {
				log.warn("Invalid channel registration in GCM. channelId="
						+ notch.getRegisterId());
				pushResult = new PushResult(notification,
						PushResult.Cause.INVALID_CHANNEL);
			} else {
				log.warn("Unable to send notification on channelId="
						+ notch.getRegisterId() + " due to error:" + errCode);
				pushResult = new PushResult(notification,
						PushResult.Cause.FAILED);
			}
		}
		return pushResult;
	}

	@Scheduled(fixedDelay = 60000)
	public void connectionWatchdog() {
		if (!isGcmEnabled) {
			init();
		}
	}

}
