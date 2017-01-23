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

import java.util.HashMap;
import java.util.List;

import javax.persistence.LockModeType;
import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.kurento.agenda.datamodel.pojo.KhcInvalidDataInfo.Code;
import com.kurento.khc.KhcInvalidDataException;
import com.kurento.khc.KhcNotFoundException;
import com.kurento.khc.datamodel.CallEntity.State;

@Component
public class CallDao extends BaseDao {

	@Autowired
	private ChannelDao channelDao;
	@Autowired
	private UserDao userDao;

	@Transactional
	public CallEntity createCall(CallEntity call, ChannelEntity invoker,
			UserEntity to) {
		ChannelEntity dbInvoker = channelDao.attach(invoker);
		UserEntity dbFrom = channelDao.getUser(dbInvoker);
		UserEntity dbTo = userDao.attach(to);

		call.setState(CallEntity.State.RINGING);
		super.save(call);
		call.setInvoker(dbInvoker);
		call.setFrom(dbFrom);
		call.setTo(dbTo);
		return call;
	}

	@Transactional
	public void deleteCall(CallEntity call) {
		CallEntity dbCall = lock(call);
		super.delete(dbCall);
	}

	@Transactional
	public void cleanCall(Long timeToLive) {

	}

	@Transactional
	public void acceptCall(CallEntity call, ChannelEntity receiver) {
		CallEntity dbCall = attach(call);
		if (!State.RINGING.equals(dbCall.getState())) {
			throw new KhcInvalidDataException("Unable to accept call",
					Code.CALL_ALREADY_ACCEPTED);
		}
		ChannelEntity dbReceiver = channelDao.attach(receiver);
		dbCall.setState(State.CONFIRMED);
		dbCall.setReceiver(dbReceiver);
		dbCall.setTimeStampAccepted(System.currentTimeMillis());
	}

	@Transactional
	public void terminateCall(CallEntity call) {
		CallEntity dbCall = attach(call);
		dbCall.setState(State.TERMINATED);

		if(dbCall.getTimeStampAccepted() != null){
			Long duration = System.currentTimeMillis() - dbCall.getTimeStampAccepted();
			dbCall.setDuration(duration);
		}

	}

	@Transactional
	public void muteCall(CallEntity call) {
		CallEntity dbCall = attach(call);
		if (!State.CONFIRMED.equals(dbCall.getState()))
			throw new KhcInvalidDataException("Unable to accept call mute",
					Code.CALL_NOT_ACCEPTED);
	}

	@Transactional
	public void cleanCalls(Long timeToLive) {
		em.createNamedQuery(CallEntity.NQ_NAME_DELETE_OLD_CALLS)
				.setParameter(CallEntity.NQ_PARAM_STATE, State.TERMINATED)
				.setParameter(CallEntity.NQ_PARAM_TIMESTAMP,
						System.currentTimeMillis() - timeToLive)
				.executeUpdate();
	}

	@Transactional
	public UserEntity getFromUser(CallEntity call) {
		CallEntity dbCall = attach(call);
		return dbCall.getFrom();
	}

	@Transactional
	public ChannelEntity getFromChannel(CallEntity call) {
		CallEntity dbCall = attach(call);
		return dbCall.getInvoker();
	}

	@Transactional
	public UserEntity getToUser(CallEntity call) {
		CallEntity dbCall = attach(call);
		return dbCall.getTo();
	}

	@Transactional
	public ChannelEntity getToChannel(CallEntity call) {
		CallEntity dbCall = attach(call);
		return dbCall.getReceiver();
	}

	@Transactional
	public CallEntity findCallByUUID(Long uuid) {
		return lock(findSingle(CallEntity.class, new String[] { "uuid" },
				new Object[] { uuid }));
	}

	@Transactional
	public CallEntity findCallByLocalId(Long localId) {
		return lock(findSingle(CallEntity.class, new String[] { "localId" },
				new Object[] { localId }));
	}

	@SuppressWarnings("unchecked")
	@Transactional
	public List<CallEntity> getConfirmedCalls(UserEntity user) {
		UserEntity dbUser = userDao.attach(user);
		Query q = em.createNamedQuery(CallEntity.NQ_NAME_GET_CALLS)
				.setParameter(CallEntity.NQ_PARAM_STATE, State.CONFIRMED)
				.setParameter(CallEntity.NQ_PARAM_USER, dbUser);
		return (List<CallEntity>) q.getResultList();
	}

	@SuppressWarnings("unchecked")
	@Transactional
	public List<CallEntity> getRingingCalls(UserEntity user) {
		UserEntity dbUser = userDao.attach(user);
		Query q = em.createNamedQuery(CallEntity.NQ_NAME_GET_CALLS)
				.setParameter(CallEntity.NQ_PARAM_STATE, State.RINGING)
				.setParameter(CallEntity.NQ_PARAM_USER, dbUser);
		return (List<CallEntity>) q.getResultList();
	}

	// /////////////////////////
	// Security verifications
	// /////////////////////////

	@Transactional
	public Boolean isCallOwner(CallEntity call, UserEntity owner) {
		try {
			CallEntity dbCall = attach(call);
			UserEntity dbOwner = userDao.attach(owner);
			if (dbCall.getFrom().getId().equals(dbOwner.getId())) {
				return true;
			} else if (dbCall.getTo().getId().equals(dbOwner.getId())) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
	}

	@Transactional
	public Boolean isCallReceiver(CallEntity call, UserEntity receiver) {
		try {
			CallEntity dbCall = attach(call);
			UserEntity dbReceiver = userDao.attach(receiver);
			if (dbCall.getTo().getId().equals(dbReceiver.getId())) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
	}

	@Transactional
	public Boolean isCallForward(CallEntity call) {
		try {
			CallEntity dbCall = attach(call);
			return dbCall.getCallFwd() != null && dbCall.getCallFwd();
		} catch (Exception e) {
			return false;
		}
	}

	// ////////////////
	//
	// HELPERS
	//
	// ////////////////

	@Transactional
	protected CallEntity attach(final CallEntity call) {
		Assert.notNull(call);
		if (em.contains(call)) {
			return call;
		} else {
			CallEntity dbCall;
			if ((dbCall = em.find(CallEntity.class, call.getId())) == null) {
				throw new KhcNotFoundException(
						"Unable to attach unknown call to JPA",
						com.kurento.agenda.datamodel.pojo.KhcNotFoundInfo.Code.SYNCHRONIZATION_IGNORE,
						CallEntity.class.getSimpleName(),
						new HashMap<String, String>() {
							private static final long serialVersionUID = 1L;
							{
								put("uuid", "" + call.getUUID());
							}
						});
			} else {
				return dbCall;
			}
		}
	}

	protected CallEntity lock(CallEntity call) {
		CallEntity dbCall = attach(call);
		em.refresh(dbCall, LockModeType.PESSIMISTIC_WRITE);
		log.trace("Lock channel {}", call.getUUID());
		return dbCall;
	}

}
