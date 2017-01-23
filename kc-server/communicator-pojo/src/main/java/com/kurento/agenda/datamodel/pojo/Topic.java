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

package com.kurento.agenda.datamodel.pojo;

import com.kurento.agenda.services.pojo.topic.TopicBase;
import com.kurento.agenda.services.pojo.topic.TopicCreate;
import com.kurento.agenda.services.pojo.topic.TopicCreateResponse;

public class Topic {
	private Long id;
	private TopicBase base;
	private String topic;

	private String key = null;

	public Topic(TopicBase base, Long id, String topic) {
		this.id = id;
		this.base = base;
		this.topic = topic;
	}

	public TopicBase getBase() {
		return base;
	}

	public Long getId() {
		return id;
	}

	synchronized public String getTopic() {
		if (key == null)
			buildTopicKey();
		return key;
	}

	public TopicCreate buildTopicCreate() {
		TopicCreate topicCreate = new TopicCreate();
		topicCreate.setBase(base);
		topicCreate.setId(id);
		topicCreate.setTopic(topic);
		return topicCreate;
	}

	public TopicCreateResponse buildTopicCreateResponse() {
		TopicCreateResponse response = new TopicCreateResponse();
		response.setTopic(this.getTopic());
		return response;
	}

	public static Topic deconstructTopicKey(String keyValue) {
		String[] tokens = keyValue.split(".", 3);
		if (tokens.length < 2)
			return null;
		TopicBase base = TopicBase.getFromValue(tokens[0]);
		Long id = Long.parseLong(tokens[1]);
		String topic = null;
		if (tokens.length == 3)
			topic = tokens[2];
		return new Topic(base, id, topic);
	}

	private void buildTopicKey() {
		key = base.getValue() + "." + id
				+ ((topic != null && !topic.isEmpty()) ? "." + topic : "");
	}
}