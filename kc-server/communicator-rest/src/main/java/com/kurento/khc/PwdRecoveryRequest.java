package com.kurento.khc;

import com.kurento.khc.datamodel.UserEntity;

public class PwdRecoveryRequest {

	private UserEntity user;
	private Long timeStamp;

	public PwdRecoveryRequest(UserEntity user) {
		this.user = user;
		this.timeStamp = System.currentTimeMillis();
	}

	public UserEntity getUser() {
		return user;
	}

	public Long getTimeStamp() {
		return timeStamp;
	}
}
