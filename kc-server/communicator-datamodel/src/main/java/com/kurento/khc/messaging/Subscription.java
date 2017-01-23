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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.datamodel.pojo.Topic;
import com.kurento.khc.datamodel.UserEntity;
import com.kurento.khc.jackson.KhcObjectMapper;

public class Subscription implements BeanFactoryAware {

	private static final Logger log = LoggerFactory
			.getLogger(Subscription.class);

	private Long channelId;
	private String instanceId;
	private UserEntity owner;
	private Long sequence = 0L;
	private Long lastAccess;
	private Map<String, AmqpTemplate> messageTemplates = new HashMap<String, AmqpTemplate>();

	private ObjectMapper jsonMapper = new KhcObjectMapper();
	private BeanFactory beanFactory;

	@Autowired
	BrokerServer brokerServer;

	protected Subscription(Long channelId, String instanceId, UserEntity owner) {
		this.channelId = channelId;
		this.instanceId = instanceId;
		this.owner = owner;
		this.lastAccess = System.currentTimeMillis();
	}

	@Override
	public void setBeanFactory(BeanFactory arg0) throws BeansException {
		beanFactory = arg0;
	}

	public Long getChannelId() {
		return channelId;
	}

	public void setChannelId(Long channelId) {
		this.channelId = channelId;
	}

	public String getInstanceId() {
		return instanceId;
	}

	public UserEntity getOwner() {
		return owner;
	}

	synchronized public Long getNextSequence() {
		return sequence++;
	}

	public Long getLastAccess() {
		return lastAccess;
	}

	public void setLastAccess(Long lastAccess) {
		this.lastAccess = lastAccess;
	}

	/**
	 * Adds a new AMQP Template for this subscription only if the given topic (key) isn't already
	 * registered.
	 * 
	 * @param topic
	 */
	public void addTopic(Topic topic) {
		String topicKey = topic.getTopic();
		if (!messageTemplates.containsKey(topicKey)) {
			String qName = instanceId + "." + topicKey;
			log.debug("Subscr " + this.toString() + " + topic: " + topicKey + " | queue: " + qName);
			messageTemplates.put(topicKey,
					(AmqpTemplate) beanFactory.getBean("khcBrokerQueue", qName, topicKey));
		}
	}

	public void removeTopic(Topic topic) {
		messageTemplates.remove(topic.getTopic());
	}

	public List<Command> getPendingCommands() {
		updateUsage();
		List<Command> commands = new ArrayList<Command>();
		Message message;

		for (AmqpTemplate messageTemplate : messageTemplates.values()) {
			while ((message = messageTemplate.receive()) != null) {
				try {
					Long sequence = getNextSequence();
					Command command = jsonMapper.convertValue(
							jsonMapper.readTree(message.getBody()),
							Command.class);
					command.setParams(jsonMapper.convertValue(
							jsonMapper.readTree(command.getParams().toString()),
							ObjectNode.class));
					command.setSequenceNumber(sequence);
					commands.add(command);
				} catch (Exception e) {
					// If an exception is hit then a sequence is missing and
					// receiver will know something went wrong
					log.warn("Unable to parse subscription command");
				}
			}
		}
		return commands;
	}

	protected void updateUsage() {
		brokerServer.updateUsage(this);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		if (channelId != null)
			builder.append("channelId=").append(channelId).append(", ");
		if (instanceId != null)
			builder.append("instanceId=").append(instanceId).append(", ");
		if (owner != null)
			builder.append("owner=").append(owner.getUUID()).append(", ");
		if (sequence != null)
			builder.append("sequence=").append(sequence).append(", ");
		if (messageTemplates != null) {
			StringBuilder tmpl = new StringBuilder("[");
			Iterator<String> k = messageTemplates.keySet().iterator();
			while (k.hasNext()) {
				tmpl.append(k.next());
				if (k.hasNext())
					tmpl.append(", ");
			}
			tmpl.append("]");
			builder.append("topics=").append(tmpl.toString());
		}
		builder.append("]");
		return builder.toString();
	}

}
