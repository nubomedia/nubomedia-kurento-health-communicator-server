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

import com.kurento.agenda.datamodel.pojo.Topic;

public class TopicBuilder {

	public static class GroupTopic extends Topic {
		protected GroupTopic(Long groupId) {
			super(TopicBase.GROUP, groupId, "");
		}
	
		public Topic toMessages() {
			return new Topic(this.getBase(), this.getId(), "messages");
		}
	}

	public static class AccountTopic extends Topic {
	
		protected AccountTopic(Long accountId) {
			super(TopicBase.ACCOUNT, accountId, "");
		}
	
		public Topic toGroups() {
			return new Topic(this.getBase(), this.getId(), "groups");
		}
	
		public Topic toUsers() {
			return new Topic(this.getBase(), this.getId(), "users");
		}
	}

	private TopicBuilder() {
	}

	public static TopicBuilder.AccountTopic toAccount(Long accountId) {
		return new TopicBuilder.AccountTopic(accountId);
	}

	public static TopicBuilder.GroupTopic toGroup(Long groupId) {
		return new TopicBuilder.GroupTopic(groupId);
	}

	public static Topic toUser(Long userId) {
		return new Topic(TopicBase.USER, userId, "");
	}
}