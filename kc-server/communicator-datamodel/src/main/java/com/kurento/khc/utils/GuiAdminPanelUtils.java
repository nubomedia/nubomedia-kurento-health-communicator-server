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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GuiAdminPanelUtils {

	private final static Logger log = LoggerFactory
			.getLogger(GuiAdminPanelUtils.class);

	@Value("${kurento.adminpanel.iconHeadWeb:nonsecures/favicon.png}")
	private String ICON_HEAD_WEB = "nonsecures/favicon.png";

	@Value("${kurento.adminpanel.titleHeadWeb:#{null}}")
	private String TITLE_HEAD_WEB = "Kurento Health Communicator";

	@Value("${kurento.adminpanel.logCustomer:#{null}}")
	private String CUSTOMER_LOGO = "";

	private String DEFAULT_ICON_HEAD_WEB = "nonsecures/favicon.png";
	private String DEFAULT_TITLE_HEAD_WEB = "Kurento Health Communicator";

	public String getIconHeadWeb() {
		if (ICON_HEAD_WEB == null || ICON_HEAD_WEB.isEmpty())
			ICON_HEAD_WEB = DEFAULT_ICON_HEAD_WEB;

		return ICON_HEAD_WEB;
	}

	public String getTitleHeadWeb() {
		if (TITLE_HEAD_WEB == null || TITLE_HEAD_WEB.isEmpty())
			TITLE_HEAD_WEB = DEFAULT_TITLE_HEAD_WEB;

		return TITLE_HEAD_WEB;
	}

}
