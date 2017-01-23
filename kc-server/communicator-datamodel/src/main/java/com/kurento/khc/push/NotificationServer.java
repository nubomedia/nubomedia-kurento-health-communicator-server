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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.kurento.agenda.datamodel.pojo.Channel;
import com.kurento.khc.datamodel.ChannelDao;
import com.kurento.khc.datamodel.ChannelEntity;

@Component("khcNotificationServer")
public class NotificationServer {

	private static final int MAX_POOL_SIZE = 20;
	private static final int KEEP_ALIVE_TIME = 1;

	private static final Long QUICK_BACKOFF_TIME = 5000L;
	private static final Long SLOW_BACKOFF_TIME = 60000L;

	private static final String BCK_QUEUE_MSG = "Backoff";

	private static Logger log = LoggerFactory
			.getLogger(NotificationServer.class);

	private ThreadPoolExecutor notificationQueue;
	private ThreadPoolExecutor backoffNotificationQueue;
	private Queue<BackoffTask> quickBackoff = new ConcurrentLinkedQueue<BackoffTask>();
	private Queue<BackoffTask> slowBackoff = new ConcurrentLinkedQueue<BackoffTask>();

	@Autowired
	private Environment env;
	@Autowired
	private ChannelDao notificationChannelDao;

	@Autowired
	@Qualifier("khcGcmPush")
	private NotificationManager gcmPush;
	@Autowired
	@Qualifier("khcApnsPush")
	private NotificationManager apnsPush;
	@Autowired
	@Qualifier("khcWebSocketManager")
	private NotificationManager wsPush;
	@Autowired
	@Qualifier("khcDevnullPush")
	private NotificationManager devNull;

	@PostConstruct
	public void init() {
		notificationQueue = new ThreadPoolExecutor(MAX_POOL_SIZE,
				MAX_POOL_SIZE, KEEP_ALIVE_TIME, TimeUnit.MINUTES,
				new LinkedBlockingQueue<Runnable>());

		backoffNotificationQueue = new ThreadPoolExecutor(MAX_POOL_SIZE,
				MAX_POOL_SIZE, KEEP_ALIVE_TIME, TimeUnit.MINUTES,
				new LinkedBlockingQueue<Runnable>());
	}

	// PUBLIC API
	public void sendNotification(final Notification notification) {
		submitForExec(notification, notificationQueue, "");
	}

	private void sendBackoffNotification(Notification notification) {
		submitForExec(notification, backoffNotificationQueue, BCK_QUEUE_MSG);
	}

	private void submitForExec(final Notification notification,
			ThreadPoolExecutor executor, String msgForType) {
		if (log.isTraceEnabled())
			log.trace("{}PoolExecutor stats: {} active tasks | "
				+ "{} threads allocated in the pool | {} queue size",
				msgForType,
				executor.getActiveCount(),
				executor.getPoolSize(),
				executor.getQueue().size());

		executor.submit(new Runnable() {
			@Override
			public void run() {
				send(notification);
			}
		});
	}

	private void send(Notification notification) {
		Assert.notNull(notification);
		Assert.notNull(notification.getChannel());
		ChannelEntity channel = notification.getChannel();
		String channelType = channel.getRegisterType();
		log.debug("Notify {} channel: {} ", channelType,
				notification.getChannel().getUUID());

		// WS is used first if available
		if (wsPush.isEnabled(channel) && notification.isNew()) {
			if (log.isDebugEnabled())
				log.debug("Sending WS  notification to channel {}",
						notification.getChannel().getUUID());
			wsPush.sendNotification(notification);
		} else if (Channel.WEB_POLL.equals(channelType)) {
			// WEB_POLL is ignored
		} else if (Channel.DEV_NULL.equals(channelType)) {
			// This is for test purposes only
			devNull.sendNotification(notification);
		} else {
			if (notification.getMsg() != null
					&& !notification.getMsg().isEmpty()) {
				if (Channel.GCM.equals(channelType)) {
					if (log.isDebugEnabled())
						log.debug("Sending GCM notification to channel {}",
								notification.getChannel().getUUID());
					gcmPush.sendNotification(notification);
				} else if (Channel.APNS.equals(channelType)) {
					if (log.isDebugEnabled())
						log.debug("Sending APNS notification to channel {}",
								notification.getChannel().getUUID());
					apnsPush.sendNotification(notification);
				} else {
					log.warn(
							"PUSH Notification request for unknown channel type: {}",
							channelType);
					return;
				}
			} else {
				log.info(
						"Not sending PUSH Notification request without message: {}",
						channelType);
			}
		}
	}

	protected void processResult(PushResult pushResult) {
		Notification notification = pushResult.getNotification();
		notification.setNew(false);
		ChannelEntity channel = notification.getChannel();
		String channelType = channel.getRegisterType();
		Long channelId = channel.getUUID();
		switch (pushResult.getCause()) {
		case OK:
			log.trace("Successful notification of type {} sent on channel {}",
					channelType, channelId);
			break;
		case RETRY:
			log.debug("Inmediate retry of push notification for channel {}-{}",
					channelType, channelId);
			sendNotification(pushResult.getNotification());
			break;
		case RECOVERABLE:
			quickBackoff.add(new BackoffTask(notification));
			log.info(
					"Re-schedule recoverable notification of type {} sent on channel {}",
					channelType, channelId);
			break;
		case SERVICE_UNAVAILABLE:
			slowBackoff.add(new BackoffTask(notification));
			log.warn(
					"Re-schedule notification of type {} sent on channel {}, due to service unavailable",
					channelType, channelId);
			break;
		case INVALID_CHANNEL:
			log.warn("Failed notification of type {} sent on invalid channel {}",
					channelType, channelId);
			deactivateNotch(channel);
		case FAILED:
			log.warn("Failed notification of type {} sent on channel {}",
					channelType, channelId);
		}
	}

	private void deactivateNotch(ChannelEntity notch) {
		/*
		 * One of the reasons to fail is the channel has been deleted, so check
		 * first if notch is still there
		 */
		try {
			notificationChannelDao.findNotificationChannelByUUID(notch
					.getUUID());
			notch.setEnabled(false);
			notificationChannelDao.updateNotificationChannel(notch);
			log.info("Deactivate channel: {}", notch.getUUID());

		} catch (Exception e) {
			log.warn("Unable to deactivate notification channel. channelId="
					+ notch.getRegisterId(), e);
		}
	}

	// /////////////////////////
	// NOTIFICATION SCHEDULER
	// /////////////////////////

	@Scheduled(fixedDelay = 100)
	public void notificationBackoffService() {
		Long currentTime = System.currentTimeMillis();
		Long quickTimeout = currentTime - QUICK_BACKOFF_TIME;
		Long slowTimeout = currentTime - SLOW_BACKOFF_TIME;
		BackoffTask bt;
		// Remove tasks from quick queue
		while ((bt = quickBackoff.peek()) != null
				&& bt.getTimestamp() < quickTimeout) {
			bt = quickBackoff.poll();
			sendBackoffNotification(bt.getNotification());
		}
		// Remove tasks from slow queue
		while ((bt = slowBackoff.peek()) != null
				&& bt.getTimestamp() < slowTimeout) {
			bt = slowBackoff.poll();
			sendBackoffNotification(bt.getNotification());
		}
	}

	private class BackoffTask {

		private Long timestamp;
		private Notification notification;

		private BackoffTask(Notification notification) {
			this.notification = notification;
			this.timestamp = System.currentTimeMillis();
		}

		private Long getTimestamp() {
			return this.timestamp;
		}

		private Notification getNotification() {
			return this.notification;
		}
	}
}
