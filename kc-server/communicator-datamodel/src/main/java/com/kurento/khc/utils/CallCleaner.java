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

package com.kurento.khc.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.kurento.khc.datamodel.CallDao;

@Component
public class CallCleaner {

	@Value("${kurento.call.terminated-call-ttl-minutes:#{null}}")
	private Long TERMINATED_CALL_TTL_MINUTES = 1L;

	@Autowired
	private CallDao callDao;

	// FIXME this scheduled delete all terminated callEntity each 60 seconds
	// @Scheduled(fixedDelay = 60000)
	public void cleanCalls() {
		callDao.cleanCalls(TERMINATED_CALL_TTL_MINUTES * 60 * 1000);
	}
}
