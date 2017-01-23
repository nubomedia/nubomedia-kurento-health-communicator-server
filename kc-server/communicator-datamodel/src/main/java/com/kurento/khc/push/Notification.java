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

package com.kurento.khc.push;

import com.kurento.khc.datamodel.ChannelEntity;

public class Notification {

	private ChannelEntity channel;
	private String msg;
	private Integer badge;
	private Boolean isNew = true;
	private boolean qos = false;

	public Notification(ChannelEntity channel, String msg, Integer badge) {
		this.channel = channel;
		this.msg = msg;
		this.badge = badge;
	}

	public ChannelEntity getChannel() {
		return channel;
	}

	public String getMsg() {
		return msg;
	}

	public Integer getBadge() {
		return badge;
	}

	public Boolean isNew() {
		return isNew;
	}

	public void setNew(Boolean isNew) {
		this.isNew = isNew;
	}

	public boolean isQos() {
		return qos;
	}

	public void setQos(boolean qos) {
		this.qos = qos;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		if (channel != null)
			builder.append("channel=").append(channel).append(", ");
		builder.append("msg=").append(msg).append(", ");
		builder.append("qos=").append(qos).append(", ");
		builder.append("isNew=").append(isNew).append(", ");
		if (badge != null)
			builder.append("badge=").append(badge);
		builder.append("]");
		return builder.toString();
	}

}
