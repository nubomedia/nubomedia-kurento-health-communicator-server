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

import java.util.List;

import com.kurento.agenda.datamodel.pojo.Channel;
import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.datamodel.pojo.Content;
import com.kurento.agenda.datamodel.pojo.Topic;
import com.kurento.khc.datamodel.ChannelEntity;
import com.kurento.khc.datamodel.CommandEntity;

public interface CommandService {

	// /////////////////////////////////////////////
	// Command management services
	// /////////////////////////////////////////////

	/**
	 * Executes one KHC command
	 * 
	 * @param command
	 *            Command POJO to be executed
	 * @param content
	 *            Optional content POJO with all information about media
	 *            location within repository
	 * @param asServer
	 *            Command the sytem to execute this command without security
	 *            checking
	 */
	void executeCommand(Command command, Content content, Boolean asServer);

	/**
	 * Executes many KHC commands as a single transaction
	 * 
	 * @param commands
	 *            List of command POJOs to be executed together
	 * @param asServer
	 *            Command the sytem to execute this command without security
	 *            checking
	 */
	void executeTransaction(List<Command> command, Boolean asServer);

	/**
	 * Return a chronologically ordered list commands to be executed in device
	 * instance identified by channelId
	 * 
	 * @param channelId
	 *            Public channel ID. Identifies a single client instance
	 * @param lastSequence
	 *            Sequence number of last executed command
	 * @return
	 */
	List<Command> getPendingCommands(Long channelId, Long lastSequence);

	// /////////////////////////////////////////////
	// Notification channel services
	// /////////////////////////////////////////////

	/**
	 * Creates a notification channel for the given register ID
	 * 
	 * @param notch
	 *            Notification channel POJO to be created
	 * @return Notification channel POJO of entity inserted into database
	 */
	Channel createNotificationChannel(Channel notch);

	/**
	 * Creates a non-persistent Channel linked with a new or already existing Subscription. This
	 * method will try to obtain an exiting subscription using the provided
	 * <strong>instanceId</strong>. If it fails, it creates a new subscription.
	 * 
	 * @param instanceId
	 *            String that identifies the subscription
	 * @return Notification channel POJO of Channel entity linked to the Subscription
	 */
	Channel createSubscription(String instanceId);

	/**
	 * Deletes a subscription using the (non-persistent) channel's ID.
	 * 
	 * @param notchId
	 */
	void deleteSubscription(Long notchId);

	void addSubscriptionTopic(Channel channel,
			Topic topic);

	void removeSubscriptionTopic(Channel channel,
			Topic topic);

	void addSubscriptionTopic(Long notchId, Topic topic);

	void removeSubscriptionTopic(Long notchId, Topic topic);

	/**
	 * Deletes a notification channel
	 * 
	 * @param notchId
	 *            Public ID of channel to be deleted
	 */
	void deleteNotificationChannel(Long notchId);

	/**
	 * Allows to update the registerID of an existing channel. If registerID is
	 * empty or null the channel becomes unregistered and notifications are not
	 * sent. Notice that channel still exists and commands are added even in
	 * unregistered state
	 * 
	 * @param notch
	 *            POJO of the notification channel to be updated
	 */
	void updateNotificationChannel(Channel notch);

	// /////////////////////////////////////////////
	// Format converters
	// /////////////////////////////////////////////

	Command buildCommandPojo(CommandEntity commandEntity);

	Channel buildNotificationChannelPojo(ChannelEntity notchEntity);
}
