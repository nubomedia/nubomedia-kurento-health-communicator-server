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

import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.agenda.datamodel.pojo.Topic;
import com.kurento.agenda.services.pojo.topic.TopicBase;
import com.kurento.agenda.services.pojo.topic.TopicBuilder;
import com.kurento.agenda.services.pojo.topic.TopicCreate;
import com.kurento.khc.jackson.KhcObjectMapper;

public class TopicTest {
	private final static Logger log = LoggerFactory.getLogger(TopicTest.class);

	private final static String jsonInput = "{\"base\":\"" + TopicBase.GROUP.getValue()
			+ "\",\"id\":4018398642479907942,\"topic\":\"messages\"}";

	private static ObjectMapper mapper;

	@Rule
	public TestName testName = new TestName();

	@BeforeClass
	public static void setUpBeforeClass() {
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
	public void testSerialization() {
		Topic t = TopicBuilder.toGroup(99999L).toMessages();
		String output = null;
		try {
			output = mapper.writeValueAsString(t.buildTopicCreate());
		} catch (IOException e) {
			log.warn("Unable to write JSON", e);
		}
		Assert.assertNotNull(output);
		log.info("Topic type {}, ID #{}, output (as TopicCreate):\n{}", t.getBase(), t.getId(),
				output);
	}

	@Test
	public void testDeserialization() {
		TopicCreate tc = null;
		log.info("Parsing json {}", jsonInput);
		try {
			tc = mapper.readValue(jsonInput, TopicCreate.class);
		} catch (IOException e) {
			log.warn("Unable to parse JSON", e);
		}
		Assert.assertNotNull(tc);
		log.info("TopicCreate base {}, ID #{}, topic {}, Topic pojo:\n", tc.getBase(),
				tc.getId(), tc.getTopic(), tc.buildTopicPojo());
	}

	static class CustomObjectMapper extends ObjectMapper {

		public CustomObjectMapper() {
			super();
			configure(SerializationConfig.Feature.WRAP_ROOT_VALUE, false);
			configure(SerializationConfig.Feature.WRITE_NULL_MAP_VALUES, true);
			setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);

			AnnotationIntrospector primary = new JacksonAnnotationIntrospector();
			AnnotationIntrospector secondary = new JaxbAnnotationIntrospector();
			AnnotationIntrospector pair = new AnnotationIntrospector.Pair(primary, secondary);

			this.getDeserializationConfig().withAnnotationIntrospector(pair);
			this.getSerializationConfig().withAnnotationIntrospector(pair);

			this.setAnnotationIntrospector(pair);
		}

	}
}
