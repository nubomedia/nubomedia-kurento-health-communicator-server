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

package com.kurento.agenda;

import java.io.IOException;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.agenda.datamodel.pojo.Message;
import com.kurento.agenda.pojo.test.FakeKurentoPayload;
import com.kurento.agenda.services.pojo.MessageApp;
import com.kurento.agenda.services.pojo.MessageReadResponse;
import com.kurento.khc.jackson.KhcObjectMapper;

public class MessageAppTest {
	private final static Logger log = LoggerFactory
			.getLogger(MessageAppTest.class);

	private final static String jsonMessageReadResponseFakeKurento = "{\"id\":1807919593207688493,"
			+ "\"timestamp\":1416237624265,"
			+ "\"timeline\":{\"id\":1325457176439082733,\"type\":\"group\",\"name\":\"myGroup\"},"
			+ "\"from\":{\"id\":891479173783366180,\"name\":\"mad\",\"surname\":\"max\"},"
			+ "\"app\":{\"name\":\""
			+ FakeKurentoPayload.APP_NAME_KURENTO
			+ "\",\"payload\":{\"id\":1999000999,\"audio\":\"ac3\",\"video\":\"avi\"}},"
			+ "\"body\":\"Testing payloads...\"}";

	private static ObjectMapper mapper;

	@Rule
	public TestName testName = new TestName();

	@BeforeClass
	public static void setUpBeforeClass() {
		// mapper = new ObjectMapper();
		// mapper.setAnnotationIntrospector(new JaxbAnnotationIntrospector());
		// if using Jackson 2.X
		// mapper.registerModule(new JaxbAnnotationModule());

		mapper = new KhcObjectMapper();
	}

	@Before
	public void setUp() {
		log.info("\t-- Running {}", testName.getMethodName());
	}

	@After
	public void tearDown() {
		log.info("\t-- Finished {}\n", testName.getMethodName());
	}

	@Test
	public void testFakeKurentoSerialization() {
		Message msg = new Message();
		MessageApp app = new MessageApp();
		app.setName(FakeKurentoPayload.APP_NAME_KURENTO);
		FakeKurentoPayload kp = new FakeKurentoPayload();
		kp.setPublicKurentoId(1999000999L);
		kp.setAudioCodec("MP3");
		kp.setVideoCodec("MPEG");
		app.setPayload(kp);
		msg.setApp(mapper.convertValue(app, ObjectNode.class));
		String output = null;
		try {
			output = mapper.writeValueAsString(app);
		} catch (IOException e) {
			log.warn("Unable to write JSON", e);
		}
		Assert.assertTrue("Null or empty json output", output != null
				&& !output.isEmpty());
		Assert.assertTrue(
				"Output doesn't contain expected strings: " + output,
				output.contains(FakeKurentoPayload.APP_NAME_KURENTO)
						&& output.contains("\"MPEG\"")
						&& output.contains("MP3")
						&& output.contains("\"id\":1999000999"));
		log.info("App name {}, payload #{}, output:\n{}", app.getName(),
				((FakeKurentoPayload) app.getPayload()).getPublicKurentoId(),
				output);
	}

	@Test
	public void testFakeDeserialization() {
		MessageReadResponse msgRR = null;
		MessageApp app = null;
		FakeKurentoPayload kp = null;
		ObjectNode onode = null;

		log.info("Parsing json to MessageReadResponse:\n{}",
				jsonMessageReadResponseFakeKurento);
		try {
			msgRR = mapper.readValue(jsonMessageReadResponseFakeKurento,
					MessageReadResponse.class);
		} catch (Exception e) {
			log.warn("Unable to parse JSON", e);
		}
		Assert.assertNotNull(msgRR);
		Assert.assertNotNull(msgRR.getApp());

		log.info("Parsing object to MessageApp from msg update (body: {})",
				msgRR.getBody());
		try {
			onode = mapper.convertValue(msgRR.getApp(), ObjectNode.class);
			app = mapper.readValue(onode, MessageApp.class);
		} catch (Exception e) {
			log.warn("Unable to parse JSON", e);
		}
		Assert.assertNotNull(app);
		Assert.assertTrue("Not the same app name",
				FakeKurentoPayload.APP_NAME_KURENTO.equalsIgnoreCase(app
						.getName()));

		log.info(
				"Parsing object to FakeKurentoPayload from app's payload (name: {})",
				FakeKurentoPayload.APP_NAME_KURENTO);
		try {
			onode = mapper.convertValue(app.getPayload(), ObjectNode.class);
			kp = mapper.readValue(onode, FakeKurentoPayload.class);
		} catch (Exception e) {
			log.warn("Unable to parse JSON", e);
		}
		Assert.assertTrue("FKP's id " + kp.getPublicKurentoId()
				+ " <> expected id " + 1999000999L, kp.getPublicKurentoId()
				.compareTo(1999000999L) == 0);
		Assert.assertTrue("Not the same audio codec",
				"ac3".equalsIgnoreCase(kp.getAudioCodec()));
		Assert.assertTrue("Not the same video codec",
				"avi".equalsIgnoreCase(kp.getVideoCodec()));
		log.info("App name {}, payload #{}, audio - {}, video - {}",
				app.getName(), kp.getPublicKurentoId(), kp.getAudioCodec(),
				kp.getVideoCodec());
	}
}
