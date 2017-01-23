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

package com.kurento.khc.utils;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.kurento.khc.jackson.KhcObjectMapper;

@Component("khcPerformanceLog")
public class KhcLogger {

	private final static String MESSAGE = "message";
	private final static String LOG_TYPE = "log_type";
	private final static String TIMESTAMP = "timestamp";
	private final static String CHANNEL_ID = "channel_id";
	private final static String FROM = "from";
	private final static String TO = "to";
	private final static String ID = "id";
	private final static String LOCAL_ID = "local_id";
	private final static String ATTACHMENT_SIZE = "attachment_size";

	protected static Logger log = LoggerFactory.getLogger(KhcLogger.class);

	private static ObjectMapper jsonMapper = new KhcObjectMapper();

	public KhcLog getLog(String logType) {
		return new KhcLog(logType);
	}

	private void print(Map<String, Object> cdr) {
		log.debug(jsonMapper.convertValue(cdr, ObjectNode.class).toString());

	}

	public class KhcLog {

		private Map<String, Object> cdr = new HashMap<String, Object>();
		private Map<String, Object> msg = new HashMap<String, Object>();

		public KhcLog(String logType) {
			cdr.put(LOG_TYPE, logType);
			cdr.put(TIMESTAMP, System.currentTimeMillis());
			cdr.put(MESSAGE, msg);
		}

		public void send() {
			print(cdr);
		}

		public KhcLog withMessageId(Long id) {
			msg.put(ID, id);
			return this;
		}

		public KhcLog withMessageLocalId(Long localId) {
			msg.put(LOCAL_ID, localId);
			return this;
		}

		public KhcLog withSize(Long size) {
			msg.put(ATTACHMENT_SIZE, size);
			return this;
		}

		public KhcLog withChannelId(Long channelId) {
			cdr.put(CHANNEL_ID, channelId);
			return this;
		}

		public KhcLog from(Long fromId) {
			msg.put(FROM, fromId);
			return this;
		}

		public KhcLog to(Long toId) {
			msg.put(TO, toId);
			return this;
		}
	}
}
