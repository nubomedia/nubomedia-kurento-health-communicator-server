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

import com.kurento.agenda.services.pojo.CommandReadResponse;

public class Command extends BasePojo {

	private static final long serialVersionUID = 1L;

	// METHOD CONSTANTS
	public static final String METHOD_FACTORY_RESET = "factoryReset";

	public static final String METHOD_CREATE_GROUP = "createGroup";
	public static final String METHOD_DELETE_GROUP = "deleteGroup";
	public static final String METHOD_UPDATE_GROUP = "updateGroup";
	public static final String METHOD_ADD_GROUP_MEMBER = "addGroupMember";
	public static final String METHOD_ADD_GROUP_ADMIN = "addGroupAdmin";
	public static final String METHOD_REMOVE_GROUP_MEMBER = "removeGroupMember";
	public static final String METHOD_REMOVE_GROUP_ADMIN = "removeGroupAdmin";
	public static final String METHOD_DELETE_GROUP_AVATAR = "deleteGroupAvatar";

	public static final String METHOD_UPDATE_USER = "updateUser";
	public static final String METHOD_UPDATE_CONTACT = "updateContact";
	public static final String METHOD_DELETE_CONTACT = "deleteContact";
	public static final String METHOD_DELETE_CONTACT_AVATAR = "deleteContactAvatar";
	public static final String METHOD_SEARCH_LOCAL_CONTACT = "searchLocalContact";

	public static final String METHOD_SEND_MESSAGE_TO_USER = "sendMessageToUser";
	public static final String METHOD_SEND_MESSAGE_TO_GROUP = "sendMessageToGroup";
	public static final String METHOD_UPDATE_MESSAGE = "updateMessage";

	public static final String METHOD_UPDATE_TIMELINE = "updateTimeline";
	public static final String METHOD_CREATE_TIMELINE = "createTimeline";
	public static final String METHOD_DELETE_TIMELINE = "deleteTimeline";

	public static final String METHOD_SEND_INVITATION = "sendInvitation";
	public static final String METHOD_ACCEPT_INVITATION = "acceptInvitation";
	public static final String METHOD_UPDATE_INVITATION = "updateInvitation";

	public static final String METHOD_CALL_DIAL = "callDial";
	public static final String METHOD_CALL_ACCEPT = "callAccept";
	public static final String METHOD_CALL_TERMINATE = "callTerminate";
	public static final String METHOD_CALL_MUTE = "callMute";

	public static final String METHOD_CALL_FWD_SETUP = "callFwdSetup";
	public static final String METHOD_CALL_FWD_ACK = "callFwdAck";
	public static final String METHOD_CALL_FWD_ESTABLISHED = "callFwdEstablished";
	public static final String METHOD_CALL_FWD_TERMINATED = "callFwdTerminated";

	// PARAMS CONSTANTS
	public static final String PARAM_ID = "id";
	public static final String PARAM_LOCAL_ID = "localId";
	public static final String PARAM_USERNAME = "username";
	public static final String PARAM_PASSWORD = "password";
	public static final String PARAM_NAME = "name";
	public static final String PARAM_SURNAME = "surname";
	public static final String PARAM_EMAIL = "email";
	public static final String PARAM_PHONE = "phone";

	public static final String PARAM_TIMESTAMP = "timestamp";
	public static final String PARAM_FROM = "from";
	public static final String PARAM_FROM_NAME = "fromName";
	public static final String PARAM_FROM_SURNAME = "fromSurname";
	public static final String PARAM_FROM_AVATAR = "fromPicture";
	public static final String PARAM_TO = "to";
	public static final String PARAM_ACK = "ack";
	public static final String PARAM_BODY = "body";
	public static final String PARAM_CONTENT_ID = "contentId";
	public static final String PARAM_CONTENT_TYPE = "contentType";
	public static final String PARAM_CONTENT_SIZE = "contentSize";
	public static final String PARAM_VIEWERS = "viewers";

	public static final String PARAM_OWNER = "owner";
	public static final String PARAM_PARTY_ID = "partyId";
	public static final String PARAM_PARTY_TYPE = "partyType";
	public static final String PARAM_PARTY_NAME = "partyName";

	public static final String PARAM_ACCOUNT = "account";
	public static final String PARAM_USER = "user";
	public static final String PARAM_GROUP = "group";
	public static final String PARAM_TIMELINE = "timeline";

	public static final String PARAM_CANSEND = "canSend";
	public static final String PARAM_CANREAD = "canRead";

	private Long sequenceNumber;

	private Long channelId;

	private String method;

	private Object params;

	// Getters & Setters

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
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

	public Long getSequenceNumber() {
		return sequenceNumber;
	}

	public void setSequenceNumber(Long sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public Long getChannelId() {
		return channelId;
	}

	public void setChannelId(Long channelId) {
		this.channelId = channelId;
	}

	// //////// FORMAT CONVERTERS //////////

	public CommandReadResponse buildCommandReadResponse() {
		CommandReadResponse commandRes = null;
		commandRes = new CommandReadResponse();
		commandRes.setChannelId(this.getChannelId());
		commandRes.setMethod(this.getMethod());
		commandRes.setSequenceNumber(this.getSequenceNumber());
		commandRes.setParams(this.getParams());

		return commandRes;
	}

	@Override
	public String toString () {
		String cmdStr = "METHOD: " + this.method + ", SEQUENCE: "
				+ this.sequenceNumber + ", CHANNEL: " + this.channelId;
		cmdStr += "\tparams: " + this.params.toString();
		return cmdStr;
	}
}
