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

package com.kurento.agenda.services.pojo.topic;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.kurento.agenda.services.pojo.topic.TopicBase.TopicBaseAdapter;

@XmlJavaTypeAdapter(TopicBaseAdapter.class)
public enum TopicBase {
	ACCOUNT("account"), GROUP("group"), USER("user");

	private String value;

	private TopicBase(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public static TopicBase getFromValue(String val) {
		if (val == null)
			return null;
		for (TopicBase bvalue : TopicBase.values())
			if (bvalue.getValue().equals(val.trim()))
				return bvalue;
		return null;
	}

	// adapter to convert TopicBase from and to String value
	public static class TopicBaseAdapter extends XmlAdapter<String, TopicBase> {

		@Override
		public String marshal(TopicBase qualifier) {
			return qualifier.getValue();
		}

		@Override
		public TopicBase unmarshal(String val) {
			return TopicBase.getFromValue(val);
		}
	}
}