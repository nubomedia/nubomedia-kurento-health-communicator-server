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
import java.util.HashMap;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.services.pojo.CallReceive;
import com.kurento.agenda.services.pojo.CallSend;
import com.kurento.khc.KhcNotFoundException;
import com.kurento.khc.datamodel.AccountEntity;
import com.kurento.khc.datamodel.ChannelDao;
import com.kurento.khc.datamodel.ChannelEntity;
import com.kurento.khc.datamodel.GroupEntity;
import com.kurento.khc.datamodel.UserEntity;
import com.kurento.khc.jackson.KhcObjectMapper;
import com.kurento.khc.services.v2.CommandService;
import com.kurento.khc.spring.KhcDatamodelConfig;
import com.kurento.khc.test.utils.KhcTestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = KhcDatamodelConfig.class)
@ActiveProfiles({ "embed_db", "embed_security" })
public class DeleteChannelCommandTest {

	private static final SecureRandom rnd = new SecureRandom();

	private AccountEntity acc;
	private GroupEntity group;
	private UserEntity sender, receiver;
	private ChannelEntity senderCh, receiverCh;

	@Autowired
	private KhcTestUtils utils;
	@Autowired
	private CommandService cmdService;
	@Autowired
	private ChannelDao channelDao;

	private ObjectMapper jsonMapper = new KhcObjectMapper();

	@Test
	public void testDeleteCallSenderChannel() throws IOException {
		testSetup();
		utils.login(sender);
		cmdService.deleteNotificationChannel(senderCh.getUUID());

		try {
			channelDao.findNotificationChannelByUUID(senderCh.getUUID());
			Assert.assertTrue(false);
		} catch (KhcNotFoundException e) {
		}
	}

	@Test
	public void testDeleteCallReceiverChannel() throws IOException {
		testSetup();
		utils.login(receiver);
		cmdService.deleteNotificationChannel(receiverCh.getUUID());

		try {
			channelDao.findNotificationChannelByUUID(receiverCh.getUUID());
			Assert.assertTrue(false);
		} catch (KhcNotFoundException e) {
		}
	}

	private void testSetup() throws IOException {

		String name = rnd.nextInt() + "";
		acc = utils.createAutomanagedAccount();
		sender = utils.createUser(name + "-sender", acc, false);
		receiver = utils.createUser(name + "receiver", acc, false);
		group = utils.createGroup(name, acc);
		utils.addGroupMember(group, sender);
		utils.addGroupMember(group, receiver);
		senderCh = utils.createChannel(sender);
		receiverCh = utils.createChannel(receiver);

		// Send several messages
		for (int i = 0; i < 50; i++) {
			utils.sendMessage("delete channelTest", sender, group);
		}
		// Inject commands
		injectCommands(receiver, receiverCh);
		injectCommands(sender, senderCh);
		// inject calls
		injectCall(sender, senderCh, receiver, receiverCh);

	}

	private void injectCommands(UserEntity user, ChannelEntity channel) {
		// Send factory reset commands to populate commands into channels
		Command command = new Command();
		command.setMethod(Command.METHOD_FACTORY_RESET);
		command.setChannelId(channel.getUUID());
		command.setParams(new HashMap<String, String>());
		utils.login(user);
		cmdService.executeCommand(command, null, false);
	}

	private void injectCall(UserEntity sender, ChannelEntity senderCh,
			UserEntity receiver, ChannelEntity receiverCh) {
		Command command = new Command();
		command.setChannelId(senderCh.getUUID());
		command.setMethod(Command.METHOD_CALL_DIAL);

		CallSend callParam = new CallSend();
		callParam.setTo(receiver.getUUID());
		callParam.setLocalId(rnd.nextLong());
		callParam.setSdp("SDP");

		command.setParams(callParam);
		// Send call
		utils.login(sender);
		cmdService.executeCommand(command, null, false);
		// Accept call in order to set receiver
		utils.login(receiver);
		for (Command cmd : cmdService.getPendingCommands(receiverCh.getUUID(),
				0L)) {
			if (cmd.getMethod().equals(Command.METHOD_CALL_DIAL)) {
				Command response = new Command();
				response.setChannelId(receiverCh.getUUID());
				response.setMethod(Command.METHOD_CALL_ACCEPT);
				CallReceive cl = jsonMapper.convertValue(cmd.getParams(),
						CallReceive.class);

				callParam = new CallSend();
				callParam.setId(cl.getId());
				callParam.setSdp("SDP");

				response.setParams(callParam);
				cmdService.executeCommand(response, null, false);
			}
		}
	}
}
