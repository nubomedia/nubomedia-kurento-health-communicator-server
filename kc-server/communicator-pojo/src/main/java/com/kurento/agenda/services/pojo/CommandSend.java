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

package com.kurento.agenda.services.pojo;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.kurento.agenda.datamodel.pojo.Command;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class CommandSend implements Serializable {

	private static final long serialVersionUID = 1L;

	// Attributes

	@XmlElement(required = true)
	private Long sequenceNumber;

	@XmlElement(required = true)
	private Long channelId;

	@XmlElement(required = true)
	private String method;

	@XmlElement
	private Object params;

	// Getters & Setters

	public Long getSequenceNumber() {
		return sequenceNumber;
	}

	public void setSequenceNumber(Long sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public Object getParams() {
		return params;
	}

	public void setParams(Object params) {
		this.params = params;
	}

	public Long getChannelId() {
		return this.channelId;
	}

	public void setChannelId(Long channelId) {
		this.channelId = channelId;
	}

	// ////////// FORMAT CONVERTERS //////////

	public Command buildCommandPojo() {
		Command command = null;
		command = new Command();
		command.setSequenceNumber(this.getSequenceNumber());
		command.setChannelId(this.getChannelId());
		command.setMethod(this.getMethod());
		command.setParams(this.getParams());

		return command;
	}

}
