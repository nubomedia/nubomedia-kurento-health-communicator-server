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

package com.kurento.agenda.datamodel.pojo;

import javax.xml.bind.annotation.XmlEnum;

public class KhcInvalidDataInfo {

	@XmlEnum
	public enum Code {
		OK, //
		INVALID_DATA, //
		NO_CREDENTIALS, //
		INVALID_SECURITY_TOKEN, //
		SECURITY_TOKEN_REQUEST_FROM_UNKNOWN_USER, //
		ACCOUNT_ALREADY_EXISTS, //
		EMAIL_ALREADY_USED, //
		PHONE_ALREADY_USED, //
		PHONE_FORMAT, //
		MESSAGE_UNKNOWN_PARTY_TYPE, //
		CALL_ALREADY_ACCEPTED, //
		CALL_NOT_ACCEPTED, //
		CALL_FWD_ALREADY_ACK, //
		CALL_FWD_ALREADY_ESTD, //
		COMMAND_NOT_FOUND, //
		COMMAND_INVALID_FORMAT, //
		CONFIGURATION_ERROR, //
		UNKNOWN_ERROR
	}

	private Code code;

	public Code getCode() {
		return code;
	}

	public void setCode(Code code) {
		this.code = code;
	}
}
