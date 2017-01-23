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

package com.kurento.khc.jsonrpc;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class JsonRpcError implements Serializable {

	private static final long serialVersionUID = 1L;

	@XmlEnum(Integer.class)
	public enum Code {
		@XmlEnumValue("-32700")
		PARSE_ERROR, //
		@XmlEnumValue("-32600")
		INVALID_REQUEST, //
		@XmlEnumValue("-32601")
		METHOD_NOT_FOUND, //
		@XmlEnumValue("-32602")
		INVALID_PARAMS, //
		@XmlEnumValue("-32603")
		INTERNAL_ERROR,

		@XmlEnumValue("1")
		MISSING_CHANNEL_ID; //
	}

	@XmlElement(required = true)
	private Code code;
	@XmlElement(required = true)
	private String message;
	@XmlElement
	private Object data;

	public Code getCode() {
		return code;
	}

	public void setCode(Code code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

}
