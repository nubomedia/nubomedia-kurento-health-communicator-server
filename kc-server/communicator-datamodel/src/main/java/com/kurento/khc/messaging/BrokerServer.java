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

package com.kurento.khc.messaging;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.kurento.agenda.datamodel.pojo.Channel;
import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.khc.KhcInternalServerException;
import com.kurento.khc.datamodel.UserEntity;
import com.kurento.khc.jackson.KhcObjectMapper;
import com.kurento.khc.utils.SecurityUtils;

@Component
public class BrokerServer implements BeanFactoryAware {
	public static final String Q_SUBSC_PROP_NAME = "${kurento.command.queue-subscription-ttl-milliseconds:#{null}}";

	private static final Logger log = LoggerFactory.getLogger(BrokerServer.class);

	private static final int MAX_POOL_SIZE = 20;
	private static final int KEEP_ALIVE_TIME = 1;

	@Value("${kurento.command.queue-subscription-ttl-milliseconds:#{null}}")
	protected Long SUBSCRIPTION_TTL_MILLISECONDS = 3600000L;

	private ThreadPoolExecutor messageQueue;
	private Boolean enabled = true;

	private Map<Long, Subscription> subscriptionMap = new ConcurrentHashMap<Long, Subscription>();
	private Map<String, Long> idsMap = new ConcurrentHashMap<String, Long>();
	private ConcurrentLinkedQueue<SubscriptionTimer> subscriptionTimer = new ConcurrentLinkedQueue<SubscriptionTimer>();

	@Autowired
	@Qualifier("khcBrokerExchange")
	private AmqpTemplate brokerExchange;
	@Autowired
	private SecurityUtils securityUtils;

	private ObjectMapper jsonMapper = new KhcObjectMapper();
	private SecureRandom rnd = new SecureRandom();

	private BeanFactory beanFactory;

	@PostConstruct
	public void init() {

		messageQueue = new ThreadPoolExecutor(0, MAX_POOL_SIZE,
				KEEP_ALIVE_TIME, TimeUnit.MINUTES,
				new LinkedBlockingQueue<Runnable>());
	}

	@Override
	public void setBeanFactory(BeanFactory arg0) throws BeansException {
		beanFactory = arg0;
	}

	public void sendCommand(final String[] topic, final String method,
			final String params) {
		messageQueue.submit(new Runnable() {

			@Override
			public void run() {
				send(topic, method, params);
			}
		});
	}

	/**
	 * Creates or retrieves subscriptions based on the instance ID as uid. The subscription gets
	 * assigned an ID of a new and not persistent channel.
	 * 
	 * @param instanceId
	 *            the caller's instance ID
	 * @return the Channel that can be used to retrieve the subscription later on
	 */
	@PreAuthorize("isAuthenticated()")
	synchronized public Channel subscribe(String instanceId) {
		UserEntity owner = securityUtils.getPrincipal();
		Long channelId = getNextId();
		Subscription subscription = null;
		if (idsMap.containsKey(instanceId))
			subscription = subscriptionMap.get(idsMap.get(instanceId));
		if (subscription == null)
			try {
				subscription = (Subscription) beanFactory.getBean("subscription", channelId,
						instanceId, owner);
				log.info("New subscription " + subscription);
			} catch (Exception e) {
				String errMsg = "Unable to create subscription with instanceId " + instanceId
						+ " for user #" + owner.getUUID();
				log.warn(errMsg, e);
				throw new KhcInternalServerException(errMsg, e);
			}
		else
			subscription.setChannelId(channelId);
		subscriptionMap.put(channelId, subscription);
		idsMap.put(instanceId, channelId); // replace ids relation
		Channel notch = new Channel();
		notch.setId(channelId);
		notch.setInstanceId(instanceId);
		updateUsage(subscription);
		return notch;
	}

	@PostAuthorize("returnObject== null or returnObject.owner.id == principal.user.id")
	public Subscription getSubscription(final Long channelId) {
		if (channelId == null)
			return null;
		return subscriptionMap.get(channelId);
	}

	@PostAuthorize("returnObject == null or returnObject.owner.id == principal.user.id")
	synchronized public Subscription removeSubscription(final Long channelId) {
		if (channelId == null)
			return null;
		return subscriptionMap.remove(channelId);
	}

	private void send(String[] topics, String method, String params) {
		Assert.notNull(method);
		Assert.notNull(params);

		Command command = new Command();
		command.setMethod(method);
		command.setParams(params);

		if (enabled) {
			String strCmd;
			try {
				strCmd = jsonMapper.writeValueAsString(command);
			} catch (Exception e) {
				log.warn("Unable to send command: {}", command.toString());
				return;
			}

			try {
				for (String topic : topics) {
					brokerExchange.send(topic, new Message(strCmd.getBytes(),
							new MessageProperties()));
					log.debug("Send command to topic {}: {}", topic, strCmd);
				}
			} catch (Exception e) {
				log.error("Error sending message to AMQP broker", e);
				enabled = false;
			}
		}
	}

	protected void updateUsage(Subscription subscription) {
		SubscriptionTimer timer = new SubscriptionTimer(subscription);
		subscriptionTimer.add(timer);
	}

	@Scheduled(fixedDelay = 100)
	public void cleanSubscriptions() {
		log.trace("Subscription clean up");
		Long timeoutTimestamp = System.currentTimeMillis() - SUBSCRIPTION_TTL_MILLISECONDS;
		SubscriptionTimer timer;
		while ((timer = subscriptionTimer.peek()) != null
				&& timer.getTimestamp() < timeoutTimestamp) {
			Subscription subscription = subscriptionTimer.poll().getSubscription();
			if (subscription.getLastAccess() < timeoutTimestamp) {
				Long id = subscription.getChannelId();
				// Subscription hasn't been really accessed
				subscriptionMap.remove(id);
				idsMap.remove(id);
				log.info("Delete subscription {}", id);
			}
		}
	}

	private Long getNextId() {
		Long uuid;
		do {
			uuid = rnd.nextLong();
		} while (subscriptionMap.containsKey(uuid));
		return uuid;
	}

	private class SubscriptionTimer {
		private Subscription subscription;
		private Long timestamp;

		private SubscriptionTimer(Subscription subscription) {
			Long timestamp = System.currentTimeMillis();
			subscription.setLastAccess(timestamp);
			this.subscription = subscription;
			this.timestamp = timestamp;
		}

		private Long getTimestamp() {
			return timestamp;
		}

		private Subscription getSubscription() {
			return this.subscription;
		}
	}
}
