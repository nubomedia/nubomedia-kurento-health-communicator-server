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

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.kurento.khc.datamodel.UserEntity;

@Configuration
@EnableScheduling
public class BrokerConfiguration {
	private final String KHC_SUBSCRIPTION_EXCHANGE = "khc.subscription";

	@Value("${kurento.amqp.hostname:#{null}}")
	private String AMQP_HOSTNAME = "localhost";

	// Queue TTL & subscription Timeout must be the same
	@Value(BrokerServer.Q_SUBSC_PROP_NAME)
	private Long QUEUE_TTL_MILLISECONDS = 10000L;

	@Bean
	public ConnectionFactory connectionFactory() {
		CachingConnectionFactory connectionFactory = new CachingConnectionFactory(
				AMQP_HOSTNAME);
		connectionFactory.setPublisherConfirms(true);
		connectionFactory.setPublisherReturns(true);
		return connectionFactory;
	}

	@Bean
	public AmqpAdmin brokerAdmin() {
		return new RabbitAdmin(connectionFactory());
	}

	@Bean
	TopicExchange subscriptionExchange() {
		TopicExchange subscriptionExchange = new TopicExchange(
				KHC_SUBSCRIPTION_EXCHANGE, false, false);
		brokerAdmin().declareExchange(subscriptionExchange);
		return subscriptionExchange;
	}

	@Bean(name = "khcBrokerExchange")
	public AmqpTemplate brokerExchange() {
		RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory());
		rabbitTemplate.setExchange(KHC_SUBSCRIPTION_EXCHANGE);
		return rabbitTemplate;
	}

	@Bean(name = "khcBrokerQueue")
	@Scope("prototype")
	public AmqpTemplate brokerQueue(String queueName, String topic) {
		RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory());
		rabbitTemplate.setQueue(queueName);

		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("x-expires", QUEUE_TTL_MILLISECONDS);
		Queue queue = new Queue(queueName, true, false, true, arguments);
		brokerAdmin().declareQueue(queue);

		TopicExchange khcExchange = subscriptionExchange();
		brokerAdmin().declareBinding(
				BindingBuilder.bind(queue).to(khcExchange).with(topic));
		return rabbitTemplate;
	}

	@Bean
	@Scope("prototype")
	public Subscription subscription(Long channelId, String instanceId, UserEntity owner) {
		return new Subscription(channelId, instanceId, owner);
	}
}
