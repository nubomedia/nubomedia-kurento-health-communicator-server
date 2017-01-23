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

package com.kurento.khc.datamodel;

/*
 * Logical entity that holds a list of messages representing conversations
 * within groups or between two users.
 */
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@Entity
@NamedQueries({
		@NamedQuery(name = ConversationEntity.NQ_NAME_FIND_CONVERSATION_TIMELINES, query = ""
				+ "SELECT tl from TimelineEntity tl WHERE"
				+ " tl.conversation =:"
				+ ConversationEntity.NQ_PARAM_CONVERSATION),
		@NamedQuery(name = ConversationEntity.NQ_NAME_FIND_CONVERSATION_GROUP, query = ""
				+ "SELECT grp from GroupEntity grp WHERE grp.conversation = :"
				+ ConversationEntity.NQ_PARAM_CONVERSATION) })
public class ConversationEntity extends BaseEntity {
	private static final long serialVersionUID = 1L;

	public static final String NQ_NAME_FIND_CONVERSATION_TIMELINES = "ConversationEntity.FindConversationTimelines";
	public static final String NQ_NAME_FIND_CONVERSATION_GROUP = "ConversationEntity.FindConversationGroup";
	public static final String NQ_PARAM_CONVERSATION = "conversation";

	// DO NOT LINK SPLICITLY THE LIST OF MESSAGES !!!
	// List below can cause memory overflow when large ammount of messages are
	// associated to this conversation
	// @ManyToMany(fetch = FetchType.LAZY, mappedBy = "timelines")
	// private List<MessageEntity> messages = new ArrayList<MessageEntity>();

}
