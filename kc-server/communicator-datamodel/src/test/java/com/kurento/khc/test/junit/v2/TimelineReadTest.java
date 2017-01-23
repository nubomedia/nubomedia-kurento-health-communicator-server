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

package com.kurento.khc.test.junit.v2;

import java.io.IOException;
import java.security.SecureRandom;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.datamodel.pojo.Message;
import com.kurento.agenda.datamodel.pojo.Timeline;
import com.kurento.agenda.services.pojo.MessageSend;
import com.kurento.khc.datamodel.AccountEntity;
import com.kurento.khc.datamodel.ChannelEntity;
import com.kurento.khc.datamodel.GroupEntity;
import com.kurento.khc.datamodel.UserEntity;
import com.kurento.khc.services.v2.CommandService;
import com.kurento.khc.services.v2.MessageService;
import com.kurento.khc.spring.KhcDatamodelConfig;
import com.kurento.khc.test.utils.KhcTestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = KhcDatamodelConfig.class)
@ActiveProfiles({ "embed_db", "embed_security" })
public class TimelineReadTest {

	private final static SecureRandom rnd = new SecureRandom();
	private final static String BODY = "timeline test";

	private AccountEntity acc, accExt;
	private GroupEntity group;
	private UserEntity user, grpAdmin, accAdmin, root, rootExt;
	private ChannelEntity channel;

	@Autowired
	private KhcTestUtils utils;
	@Autowired
	private MessageService msgService;
	@Autowired
	private CommandService cmdService;

	@Test
	public void testReadTimelineByAllReceivers() throws IOException {
		testSetup();

		sendMessages();

		Long groupId = group.getUUID();
		utils.login(user);
		Timeline timeline = msgService.getMesssageTimelineWithGroup(groupId);
		int i = 10;
		for (Message msg : msgService.getMessageListFromTimeline(
				timeline.getId(), 10)) {
			Assert.assertTrue(msg.getBody().equals(BODY + --i));
		}

		utils.login(grpAdmin);
		timeline = msgService.getMesssageTimelineWithGroup(groupId);
		i = 10;
		for (Message msg : msgService.getMessageListFromTimeline(
				timeline.getId(), 10)) {
			Assert.assertTrue(msg.getBody().equals(BODY + --i));
		}
	}

	@Test
	public void testReadTimelineWhenAdminIsMember() throws IOException {
		testSetup();
		utils.addGroupMember(group, accAdmin);
		utils.addGroupMember(group, root);
		sendMessages();

		Long groupId = group.getUUID();

		utils.login(accAdmin);
		Timeline timeline = msgService.getMesssageTimelineWithGroup(groupId);
		int i = 10;
		for (Message msg : msgService.getMessageListFromTimeline(
				timeline.getId(), 10)) {
			Assert.assertTrue(msg.getBody().equals(BODY + --i));
		}
	}

	void sendMessages() {
		utils.login(user);
		for (int i = 0; i < 10; i++) {
			// Send messages in order to create timelines
			MessageSend params = new MessageSend();
			params.setLocalId(rnd.nextLong());
			params.setFrom(user.getUUID());
			params.setTo(group.getUUID());
			params.setBody(BODY + i);

			Command sendMessage = new Command();
			sendMessage.setChannelId(channel.getUUID());
			sendMessage.setMethod(Command.METHOD_SEND_MESSAGE_TO_GROUP);
			sendMessage.setParams(params);

			cmdService.executeCommand(sendMessage, null, false);
		}
	}

	private void testSetup() throws IOException {
		String name = "" + rnd.nextInt();
		acc = utils.createAccount();
		group = utils.createGroup(name, acc);
		user = utils.createUser(name + "-user", acc, false);
		utils.addGroupMember(group, user);
		channel = utils.createChannel(user);
		grpAdmin = utils.createUser(name + "-grpAdmin", acc, false);
		utils.addGroupAdmin(group, grpAdmin);
		accAdmin = utils.createUser(name + "-accAdmin", acc, false);
		utils.addAccountAdmin(acc, accAdmin);
		root = utils.createRootUser(name + "root", acc);

		accExt = utils.createAccount();
		rootExt = utils.createRootUser(name + "-rootExt", accExt);

	}

}
