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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.services.pojo.UserEdit;
import com.kurento.khc.datamodel.AccountEntity;
import com.kurento.khc.datamodel.ChannelEntity;
import com.kurento.khc.datamodel.UserEntity;
import com.kurento.khc.jackson.KhcObjectMapper;
import com.kurento.khc.push.DevNullManager;
import com.kurento.khc.push.DevNullManager.CauseGenerator;
import com.kurento.khc.push.DevNullManager.DevNullListener;
import com.kurento.khc.push.Notification;
import com.kurento.khc.push.PushResult.Cause;
import com.kurento.khc.qos.QosServer;
import com.kurento.khc.services.v2.CommandService;
import com.kurento.khc.spring.KhcDatamodelConfig;
import com.kurento.khc.test.utils.KhcTestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = KhcDatamodelConfig.class)
@ActiveProfiles({ "embed_db", "embed_security" })
public class QosTest {
	private final Logger log = LoggerFactory.getLogger(QosTest.class);

	@Autowired
	private KhcTestUtils utils;
	@Autowired
	private CommandService cmdService;
	@Autowired
	private DevNullManager pushManager;
	@Autowired
	private QosServer qosServer;

	private AccountEntity acc;
	private UserEntity userQos, userNotQos;
	private List<ChannelEntity> qosChannels;
	private List<ChannelEntity> notQosChannels;

	private static final Random rnd = new SecureRandom();
	private ObjectMapper jsonMapper = new KhcObjectMapper();

	private ConcurrentMap<String, Notification> notifs = new ConcurrentHashMap<String, Notification>();

	@Value("${kurento.qos.notification-delay-milliseconds:#{null}}")
	private Long qosNotifDelay;

	@Test
	public void testQosServer() {
		try {
			testSetup();
		} catch (IOException e) {
			log.warn("Setup error", e);
			fail("Setup error: " + e.getMessage());
		}
		for (ChannelEntity c : qosChannels) {
			pushManager.addListener(c.getUUID(),
					createDevNullListener(c.getUUID()));
			qosServer.reviewQosInstance(c, true);
		}
		for (ChannelEntity c : notQosChannels) {
			pushManager.addListener(c.getUUID(),
					createDevNullListener(c.getUUID()));
			qosServer.reviewQosInstance(c, false);
		}
		try {
			log.debug("Sleeping 2 times the QosServer delay (2 * {}ms)", qosNotifDelay);
			Thread.sleep(2 * qosNotifDelay);
		} catch (InterruptedException e) {
			log.warn("Interrupted while waiting", e);
		}
		assertTrue("Notifications map size <> qos channels size: " + notifs.size()
				+ " <> " + qosChannels.size(), notifs.size() == qosChannels.size());
		for (Notification n : notifs.values())
			assertEquals(qosNotifDelay.longValue(), Long.parseLong(n.getMsg()));

		cmdService.executeCommand(createCmdChangeQos(qosChannels.get(0)), null, true);
		utils.login(userQos);
		for (ChannelEntity c : qosChannels) {
			cmdService.getPendingCommands(c.getUUID(), 0L);
		}
		notifs.clear();
		try {
			log.debug("Sleeping 2 times the QosServer delay (2 * {}ms)", qosNotifDelay);
			Thread.sleep(2 * qosNotifDelay);
		} catch (InterruptedException e) {
			log.warn("Interrupted while waiting", e);
		}
		assertTrue("Notifications map size is not empty (no qos channels): "
				+ notifs.size() + " <> 0", notifs.isEmpty());
	}

	private DevNullListener createDevNullListener(final Long notchId) {
		return new DevNullListener() {
			@Override
			public void push(Notification notification) {
				assertNotNull(notification);
				assertNotNull(notification.getChannel());
				assertNotNull(notification.getChannel().getInstanceId());
				log.debug("notchId {}: received notif {}", notchId,
						notification);
				notifs.putIfAbsent(notification.getChannel().getInstanceId(),
						notification);
			}
		};
	}

	private Command createCmdChangeQos(ChannelEntity channel) {
		UserEdit params = new UserEdit();
		params.setId(userQos.getUUID());
		params.setName(userQos.getName());
		params.setSurname(userQos.getSurname());
		params.setPassword(userQos.getPassword());
		params.setPhone(userQos.getPhone());
		params.setEmail(userQos.getEmail());
		params.setQos(false);

		Command updateUser = new Command();
		updateUser.setChannelId(channel.getUUID());
		updateUser.setMethod(Command.METHOD_UPDATE_USER);
		updateUser.setParams(jsonMapper.convertValue(params, ObjectNode.class));
		return updateUser;
	}

	private void testSetup() throws IOException {
		pushManager.addCauseGenerator(new CauseGenerator() {
			@Override
			public Cause generateCause() {
				return Cause.OK;
			}
		});

		String name = "" + rnd.nextInt();
		acc = utils.createAccount();
		userQos = utils.createUser(name + "-qos", acc, false, true);
		userNotQos = utils.createUser(name + "-not-qos", acc, false, false);

		qosChannels = new ArrayList<ChannelEntity>();
		for (int i = 0; i < 5; i++) {
			qosChannels.add(utils.createChannel(userQos));
		}

		notQosChannels = new ArrayList<ChannelEntity>();
		for (int i = 0; i < 5; i++) {
			notQosChannels.add(utils.createChannel(userNotQos));
		}
	}
}
