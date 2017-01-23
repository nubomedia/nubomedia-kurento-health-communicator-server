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

import javax.persistence.LockModeType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.kurento.agenda.datamodel.pojo.KhcInvalidDataInfo.Code;
import com.kurento.khc.KhcInternalServerException;
import com.kurento.khc.KhcInvalidDataException;
import com.kurento.khc.KhcNotFoundException;
import com.kurento.khc.datamodel.CallFwdEntity.State;

@Component
public class CallFwdDao extends BaseDao {

	@Autowired
	private CallDao callDao;
	@Autowired
	private ChannelDao channelDao;
	@Autowired
	private UserDao userDao;

	@Transactional
	public CallFwdEntity createCallFwd(CallFwdEntity fwd,
			ChannelEntity invoker, UserEntity from, UserEntity to) {

		ChannelEntity dbInvoker = channelDao.attach(invoker);
		UserEntity dbDispatcher = channelDao.getUser(dbInvoker);

		UserEntity dbFrom = userDao.attach(from);
		UserEntity dbTo = userDao.attach(to);
		log.debug(
				"Dispatcher:\n{}\nDispatcher chan (invoker): {}\nFwd from:\n{}\nFwd to:\n{}",
				dbDispatcher, dbInvoker, dbFrom, dbTo);

		ChannelEntity caller = null;
		ChannelEntity callee = null;

		for (CallEntity call : callDao.getConfirmedCalls(dbDispatcher)) {
			try {
				UserEntity callFromUser = callDao.getFromUser(call);
				UserEntity callToUser = callDao.getToUser(call);

				ChannelEntity callFromNotch = callDao.getFromChannel(call);
				ChannelEntity callToNotch = callDao.getToChannel(call);

				if (callFromNotch == null)
					throw new KhcInternalServerException(
							"Null 'from' channel (invoker) in call\n" + call);
				if (callToNotch == null)
					throw new KhcInternalServerException(
							"Null 'to' channel (recv) in call\n" + call);

				log.trace("Confirmed call of invoker:\n{}"
						+ "\ncallFromUser:\n{}\ncallFromNotch: {}"
						+ "\ncallToUser:\n{}\ncallToNotch: {}", call,
						callFromUser, callFromNotch, callToUser, callToNotch);

				// only calls on current channel
				if (!callFromNotch.getUUID().equals(dbInvoker.getUUID())
						&& !callToNotch.getUUID().equals(dbInvoker.getUUID()))
					continue;

				if (callToUser.getUUID().equals(dbFrom.getUUID()))
					caller = callToNotch;
				else if (callToUser.getUUID().equals(dbTo.getUUID()))
					callee = callToNotch;

				if (callFromUser.getUUID().equals(dbFrom.getUUID()))
					caller = callFromNotch;
				else if (callFromUser.getUUID().equals(dbTo.getUUID()))
					callee = callFromNotch;
			} catch (Throwable e) {
				log.warn(
						"Error in active confirmed call of dispatcher {} (#{}):\n\t{}",
						dbDispatcher.getEmail(), dbDispatcher.getId(),
						e.getMessage());
				log.trace("Error details", e);
			}
		}

		if (caller == null)
			throw new KhcInvalidDataException(
					"No valid ongoing call between the "
							+ "dispatcher and the caller", Code.INVALID_DATA);
		if (callee == null)
			throw new KhcInvalidDataException("No valid ongoing call between "
					+ "the dispatcher and the callee", Code.INVALID_DATA);

		fwd.setState(State.SETUP);

		fwd.setDispatch(dbDispatcher);
		fwd.setInvoker(dbInvoker);
		fwd.setFrom(dbFrom);
		fwd.setCaller(caller);
		fwd.setTo(dbTo);
		fwd.setCallee(callee);

		super.save(fwd);

		log.debug("(setup) Saved call forward #{} (uuid={}) between "
				+ "- caller channel:\n{}\n\t- and callee channel:\n{}", fwd.id,
				fwd.uuid, caller, callee);
		return fwd;
	}

	@Transactional
	public void deleteCallFwd(CallFwdEntity fwd) {
		CallFwdEntity dbCallFwd = lock(fwd);
		super.delete(dbCallFwd);
	}

	@Transactional
	public void cleanCall(Long timeToLive) {

	}

	@Transactional
	public void ackCallFwd(CallFwdEntity fwd, ChannelEntity callee) {
		CallFwdEntity dbCallFwd = attach(fwd);
		if (!dbCallFwd.getCallee().getUUID().equals(callee.getUUID()))
			throw new KhcInvalidDataException(
					"Callee channel not registered for this call forward",
					Code.INVALID_DATA);
		if (!State.SETUP.equals(dbCallFwd.getState())) {
			throw new KhcInvalidDataException(
					"Unable to acknowledge call forward",
					Code.CALL_FWD_ALREADY_ACK);
		}
		dbCallFwd.setState(State.ACK);
		dbCallFwd.setTimeStampAck(System.currentTimeMillis());

		log.debug("Call fwd #{}: {}", dbCallFwd.getUUID(), dbCallFwd.getState());
	}

	@Transactional
	public void establishCallFwd(CallFwdEntity fwd, ChannelEntity callee) {
		CallFwdEntity dbCallFwd = attach(fwd);
		if (!dbCallFwd.getCallee().getUUID().equals(callee.getUUID()))
			throw new KhcInvalidDataException(
					"Callee channel not registered for this call forward",
					Code.INVALID_DATA);
		if (!State.ACK.equals(dbCallFwd.getState())) {
			throw new KhcInvalidDataException(
					"Unable to establish call forward",
					Code.CALL_FWD_ALREADY_ESTD);
		}
		dbCallFwd.setState(State.ESTABLISHED);
		dbCallFwd.setTimeStampAccepted(System.currentTimeMillis());

		log.debug("Call fwd #{}: {}", dbCallFwd.getUUID(), dbCallFwd.getState());
	}

	@Transactional
	public void terminateCallFwd(CallFwdEntity fwd) {
		CallFwdEntity dbCallFwd = attach(fwd);
		dbCallFwd.setFailed(fwd.getFailed());
		dbCallFwd.setState(State.TERMINATED);

		log.debug("Call fwd #{}: {} (failed: {})", dbCallFwd.getUUID(),
				dbCallFwd.getState(), dbCallFwd.getFailed());
	}

	@Transactional
	public void cleanCallForwards(Long timeToLive) {
		em.createNamedQuery(CallFwdEntity.NQ_NAME_DELETE_OLD_CALL_FWDS)
				.setParameter(CallFwdEntity.NQ_PARAM_STATE, State.TERMINATED)
				.setParameter(CallFwdEntity.NQ_PARAM_TIMESTAMP,
						System.currentTimeMillis() - timeToLive)
				.executeUpdate();
	}

	@Transactional
	public UserEntity getDispatchUser(CallFwdEntity fwd) {
		CallFwdEntity dbCallFwd = attach(fwd);
		return dbCallFwd.getDispatch();
	}

	@Transactional
	public ChannelEntity getDispatchChannel(CallFwdEntity fwd) {
		CallFwdEntity dbCallFwd = attach(fwd);
		return dbCallFwd.getInvoker();
	}

	@Transactional
	public UserEntity getFromUser(CallFwdEntity fwd) {
		CallFwdEntity dbCallFwd = attach(fwd);
		return dbCallFwd.getFrom();
	}

	@Transactional
	public ChannelEntity getFromChannel(CallFwdEntity fwd) {
		CallFwdEntity dbCallFwd = attach(fwd);
		return dbCallFwd.getCaller();
	}

	@Transactional
	public UserEntity getToUser(CallFwdEntity fwd) {
		CallFwdEntity dbCallFwd = attach(fwd);
		return dbCallFwd.getTo();
	}

	@Transactional
	public ChannelEntity getToChannel(CallFwdEntity fwd) {
		CallFwdEntity dbCallFwd = attach(fwd);
		return dbCallFwd.getCallee();
	}

	@Transactional
	public CallFwdEntity findCallByUUID(Long uuid) {
		return lock(findSingle(CallFwdEntity.class, new String[] { "uuid" },
				new Object[] { uuid }));
	}

	@Transactional
	public CallFwdEntity findCallByLocalId(Long localId) {
		return lock(findSingle(CallFwdEntity.class, new String[] { "localId" },
				new Object[] { localId }));
	}

	// /////////////////////////
	// Security verifications
	// /////////////////////////

	@Transactional
	public Boolean isMember(CallFwdEntity fwd, UserEntity member) {
		try {
			CallFwdEntity dbCallFwd = attach(fwd);
			Long memberId = userDao.attach(member).getId();
			if (dbCallFwd.getDispatch().getId().equals(memberId)
					|| dbCallFwd.getFrom().getId().equals(memberId)
					|| dbCallFwd.getTo().getId().equals(memberId))
				return true;
			else
				return false;
		} catch (Exception e) {
			return false;
		}
	}

	@Transactional
	public Boolean isCallee(CallFwdEntity fwd, UserEntity callee) {
		try {
			CallFwdEntity dbCallFwd = attach(fwd);
			Long calleeId = userDao.attach(callee).getId();
			if (dbCallFwd.getTo().getId().equals(calleeId))
				return true;
			else
				return false;
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
	protected CallFwdEntity attach(final CallFwdEntity fwd) {
		Assert.notNull(fwd);
		if (em.contains(fwd)) {
			return fwd;
		} else {
			CallFwdEntity dbFwd;
			if ((dbFwd = em.find(CallFwdEntity.class, fwd.getId())) == null) {
				throw new KhcNotFoundException(
						"Unable to attach unknown call forward to JPA",
						com.kurento.agenda.datamodel.pojo.KhcNotFoundInfo.Code.SYNCHRONIZATION_IGNORE,
						CallFwdEntity.class.getSimpleName(),
						new HashMap<String, String>() {
							private static final long serialVersionUID = 1L;
							{
								put("uuid", "" + fwd.getUUID());
							}
						});
			} else {
				return dbFwd;
			}
		}
	}

	protected CallFwdEntity lock(CallFwdEntity fwd) {
		CallFwdEntity dbFwd = attach(fwd);
		em.refresh(dbFwd, LockModeType.PESSIMISTIC_WRITE);
		log.trace("Lock channel {}", fwd.getUUID());
		return dbFwd;
	}

}
