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
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.services.pojo.UserReadContactResponse;
import com.kurento.agenda.services.pojo.UserSearchLocalContact;
import com.kurento.khc.datamodel.AccountEntity;
import com.kurento.khc.datamodel.ChannelEntity;
import com.kurento.khc.datamodel.UserEntity;
import com.kurento.khc.jackson.KhcObjectMapper;
import com.kurento.khc.services.v2.CommandService;
import com.kurento.khc.spring.KhcDatamodelConfig;
import com.kurento.khc.test.utils.KhcTestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = KhcDatamodelConfig.class)
@ActiveProfiles({ "embed_db", "embed_security" })
public class SearchLocalContactCommandTest {

	private Random rnd = new SecureRandom();

	private AccountEntity acc, accExt;
	private UserEntity user;
	private ChannelEntity channel;

	private ObjectMapper jsonMapper = new KhcObjectMapper();

	@Autowired
	KhcTestUtils utils;

	@Autowired
	CommandService commandService;

	@Test
	public void searchLocalContactTest() throws IOException {

		testSetup();

		UserSearchLocalContact local = new UserSearchLocalContact();
		int i = 0;
		long lastSequence = 0L;
		// First round
		local.getLocalPhones().add("tururu"); // false
		local.getLocalPhones().add("914555000");// mine
		local.getLocalPhones().add("+330012231222"); // false
		local.getLocalPhones().add("tarara");
		local.getLocalPhones().add("91 45 55 50" + i++); // 1st
		local.getLocalPhones().add("+3491455-----5 50" + i++);// 2nd
		local.getLocalPhones().add("914--5-55-50" + i++);// 3rd
		local.getLocalPhones().add("+3491455550" + i++);// 4th
		i = 0;
		local.getLocalPhones().add("+3491455560" + i++);// foreign 1st
		local.getLocalPhones().add("+3491455560" + i++);// foreign 2nd

		Command command = new Command();
		command.setMethod(Command.METHOD_SEARCH_LOCAL_CONTACT);
		command.setChannelId(channel.getUUID());
		command.setParams(local);

		utils.login(user);
		commandService.executeCommand(command, null, false);
		List<Command> pendingCommands = commandService.getPendingCommands(
				channel.getUUID(), lastSequence);
		Assert.assertTrue(pendingCommands.size() == 4);
		Map<String, Boolean> phoneMap = new HashMap<String, Boolean>();
		for (i = 0; i < 4; i++) {
			phoneMap.put("" + i, true);
		}
		for (Command cmd : pendingCommands) {
			lastSequence = cmd.getSequenceNumber();
			UserReadContactResponse contact = jsonMapper.convertValue(
					cmd.getParams(), UserReadContactResponse.class);

			Assert.assertTrue(Command.METHOD_UPDATE_CONTACT.equals(cmd
					.getMethod()));
			String ix = ""
					+ contact.getPhone()
							.charAt(contact.getPhone().length() - 1);
			Assert.assertTrue(phoneMap.remove(ix));
		}

		Assert.assertTrue(phoneMap.size() == 0);

		// Second round
		local.getLocalPhones().add("003491455  550" + i++); // 5th
		local.getLocalPhones().add("91455      5 50" + i++); // 6th
		local.getLocalPhones().add("+349145 55 50" + i++); // 7th
		local.getLocalPhones().add("00349 1 4 5 5 5 50" + i++); // 8th
		command = new Command();
		command.setMethod(Command.METHOD_SEARCH_LOCAL_CONTACT);
		command.setChannelId(channel.getUUID());
		command.setParams(local); // Send full list again

		utils.login(user);
		commandService.executeCommand(command, null, false);
		pendingCommands = commandService.getPendingCommands(
				channel.getUUID(), lastSequence);
		Assert.assertTrue(pendingCommands.size() == 4);
		phoneMap = new HashMap<String, Boolean>();
		for (i = 4; i < 8; i++) { // Receive contacts only for new contacts
			phoneMap.put("" + i, true);
		}
		for (Command cmd : pendingCommands) {
			UserReadContactResponse contact = jsonMapper.convertValue(
					cmd.getParams(), UserReadContactResponse.class);

			Assert.assertTrue(Command.METHOD_UPDATE_CONTACT.equals(cmd
					.getMethod()));
			String ix = ""
					+ contact.getPhone()
							.charAt(contact.getPhone().length() - 1);
			Assert.assertTrue(phoneMap.remove(ix));
		}

		Assert.assertTrue(phoneMap.size() == 0);

	}

	private void testSetup() throws IOException {
		String name = String.valueOf(rnd.nextInt());
		acc = utils.createAutomanagedAccount();
		user = new UserEntity();
		user.setEmail(name + "@kurento.com");
		user.setPassword(utils.md5Hex(name));
		user.setPhone("914555000");
		user.setPhoneRegion("ES");
		user = utils.createUser(user, acc);
		channel = utils.createChannel(user);

		// Create several users
		UserEntity user;

		int i = 0;
		user = new UserEntity();
		user.setPhone("+34 91-45 55 - 5-0" + i++);
		user.setPhoneRegion("ES");
		user.setPassword("SECRETA");
		utils.createUser(user, acc);

		user = new UserEntity();
		user.setPhone("00 34-91-455-550-" + i++);
		user.setPhoneRegion("ES");
		user.setPassword("SECRETA");
		utils.createUser(user, acc);

		user = new UserEntity();
		user.setPhone("91455550" + i++);
		user.setPhoneRegion("ES");
		user.setPassword("SECRETA");
		utils.createUser(user, acc);

		user = new UserEntity();
		user.setPhone("+3491455550" + i++);
		user.setPhoneRegion("ES");
		user.setPassword("SECRETA");
		utils.createUser(user, acc);

		user = new UserEntity();
		user.setPhone("0034-91-45-555-0" + i++);
		user.setPhoneRegion("ES");
		user.setPassword("SECRETA");
		utils.createUser(user, acc);

		user = new UserEntity();
		user.setPhone("3491455550" + i++);
		user.setPhoneRegion("ES");
		user.setPassword("SECRETA");
		utils.createUser(user, acc);

		user = new UserEntity();
		user.setPhone("+34-9 1-4 5-5 5 5-0 " + i++);
		user.setPhoneRegion("ES");
		user.setPassword("SECRETA");
		utils.createUser(user, acc);

		user = new UserEntity();
		user.setPhone("9 1-45-5 55-0" + i++);
		user.setPhoneRegion("ES");
		user.setPassword("SECRETA");
		utils.createUser(user, acc);

		for (i = 10; i < 20; i++) {
			utils.createUser(name + i, acc, false);
		}

		// Create external account
		accExt = utils.createAutomanagedAccount();

		i = 0;
		user = new UserEntity();
		user.setPhone("+3491455560" + i++);
		user.setPhoneRegion("ES");
		user.setPassword("SECRETA");
		utils.createUser(user, accExt);

		user = new UserEntity();
		user.setPhone("+3491455560" + i++);
		user.setPhoneRegion("ES");
		user.setPassword("SECRETA");
		utils.createUser(user, accExt);

	}
}
