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

package com.kurento.khc.services.v2;

import java.io.FileNotFoundException;
import java.util.List;

import com.kurento.agenda.datamodel.pojo.Content;
import com.kurento.agenda.datamodel.pojo.Message;
import com.kurento.agenda.datamodel.pojo.Timeline;
import com.kurento.khc.datamodel.MessageEntity;
import com.kurento.khc.datamodel.TimelineEntity;

public interface MessageService {

	/**
	 * Retrieves the message identified by given messageId. An exception is
	 * thrown if no message is found with given ID
	 * 
	 * @param messageId
	 *            Public ID of requested message
	 * @return Message POJO which ID is the requested one
	 */
	Message getMessage(Long messageId);

	/**
	 * Retrieves a list of messages from timeline identified by timelineId. List
	 * size is limited to maxMessage and the first message is the next one
	 * received after message identified by lastMessage
	 * 
	 * @param timelineId
	 *            ID of timeline from where message is requested. Security check
	 *            is performed on this element
	 * @param lastMessage
	 *            Last message read. First message returned will be next one
	 *            received after that one
	 * @param maxMessage
	 *            Maximum size of returned list
	 * @return List of messages from timeline recevied after lastMessage
	 */
	List<Message> getMessageListFromTimeline(Long timelineId, Long lastMessage,
			Integer maxMessage);

	/**
	 * Retrieves a list of messages from the begining of timeline. List size is
	 * limited to maxMessage
	 * 
	 * @param timelineId
	 *            ID of timeline from where message is requested. Security check
	 *            is performed on this element
	 * @param maxMessage
	 *            Maximum size of returned list
	 * @return List of messages from timeline recevied after lastMessage
	 */
	List<Message> getMessageListFromTimeline(Long timelineId, Integer maxMessage);

	/**
	 * Retrieves the content entity associated to message attachment
	 * 
	 * @param timelineId
	 *            Public ID of timeline from where message is retrieved.
	 *            Security checkings are performed on this element
	 * @param messageId
	 *            Public ID of message from where content is requested
	 * @return Content entity if non null
	 */
	Content getMessageContent(Long timelineId, Long messageId);

	/**
	 * Retrieves the content entity associated to sender's profile picture
	 * 
	 * @param timelineId
	 *            Public ID of timeline from where message is retrieved.
	 *            Security checkings are performed on this element
	 * @param messageId
	 *            Public ID of message from where avatar is requested
	 * @return Content entity of sender's avatarif non null
	 * @throws FileNotFoundException
	 */
	Content getMessageAvatar(Long timelineId, Long messageId)
			throws FileNotFoundException;

	/**
	 * Returns the list of timelines owned by user
	 * 
	 * @param userId
	 *            Public ID of user which timelines are requested
	 * @return List of timelines
	 */
	List<Timeline> getMessageTimelines(Long userId);

	/**
	 * Creates a new timeline entity to hold messages interchanged between a
	 * user and a party. Parties can be of type user, to record one-to-one
	 * communications, or group, to record one-to-many scenarios. Special care
	 * have to be taken as no verification is performed wheter a timeline
	 * already exists for the same user and party. So the same pair of user and
	 * party might appear several times. This method does not check the
	 * existence of the party. If a timeline is created with a non-existing
	 * party it won't never hold messages
	 * 
	 * @param timelineReq
	 *            POJO with timeline details
	 * @return Timeline POJO including public ID
	 */
	Timeline CreateMessageTimeline(Timeline timelineReq);

	/**
	 * Retrieves a timeline with the given public ID
	 * 
	 * @param timelineId
	 *            Public timeline ID
	 * @return Timeline POJO
	 */
	Timeline getMessageTimeline(Long timelineId);

	/**
	 * Retrieves he timeline principal might have with given group
	 * 
	 * @param groupId
	 *            Public ID of group which timeline is requested
	 * @return TImeline POJO
	 */
	Timeline getMesssageTimelineWithGroup(Long groupId);

	// ////////////////////////////////////
	// Format converters
	// ////////////////////////////////////

	Message buildMessagePojo(MessageEntity messageEntity);

	MessageEntity buildMessageEntity(Message message);

	Timeline buildTimelinePojo(TimelineEntity timelineEntity);

}