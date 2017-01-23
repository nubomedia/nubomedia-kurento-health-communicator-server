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

package com.kurento.khc.datamodel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@Entity
@NamedQueries({
		@NamedQuery(name = CommandEntity.NQ_NAME_GET_PENDING_COMMANDS, query = ""
				+ "SELECT cmd FROM CommandEntity cmd WHERE cmd.receiver = :"
				+ CommandEntity.NQ_PARAM_RECEIVER
				+ " AND "
				+ " ( cmd.sequence > :"
				+ CommandEntity.NQ_PARAM_LAST_SEQUENCE
				+ " OR cmd.sequence = 0 )"),
		@NamedQuery(name = CommandEntity.NQ_NAME_DELETE_COMPLETED_COMMANDS, query = ""
				+ "DELETE FROM CommandEntity cmd WHERE cmd.receiver = :"
				+ CommandEntity.NQ_PARAM_RECEIVER
				+ " AND cmd.sequence > 0"
				+ " AND cmd.sequence <= :"
				+ CommandEntity.NQ_PARAM_LAST_SEQUENCE),
		@NamedQuery(name = CommandEntity.NQ_NAME_DELETE_COMMAND_QUEUE, query = ""
				+ "DELETE FROM CommandEntity cmd WHERE cmd.receiver = :"
				+ CommandEntity.NQ_PARAM_RECEIVER) })
public class CommandEntity extends BaseEntity {
	private static final long serialVersionUID = 1L;

	public static final String NQ_NAME_GET_PENDING_COMMANDS = "CommandEntity.getPendingCommands";
	public static final String NQ_NAME_DELETE_COMPLETED_COMMANDS = "CommandEntity.deleteCompletedCommands";
	public static final String NQ_NAME_DELETE_COMMAND_QUEUE = "CommandEntity.deleteCommandQueue";

	public static final String NQ_PARAM_LAST_SEQUENCE = "lastSequence";
	public static final String NQ_PARAM_RECEIVER = "receiver";

	// Attributes

	@Column(nullable = false)
	private Long sequence = 0L;

	@Column(nullable = false)
	private String method;

	@Lob
	@Column(length = 10000)
	private String params;

	@ManyToOne(fetch = FetchType.LAZY)
	private ChannelEntity receiver;

	// Getters & Setters

	public Long getSequence() {
		return sequence;
	}

	public void setSequence(Long sequence) {
		this.sequence = sequence;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	protected String getParams() {
		return params;
	}

	protected void setParams(String params) {
		this.params = params;
	}

	protected ChannelEntity getReceiver() {
		return receiver;
	}

	protected void setReceiver(ChannelEntity receiver) {
		this.receiver = receiver;
	}

}
