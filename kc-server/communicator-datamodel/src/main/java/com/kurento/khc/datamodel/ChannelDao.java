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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.persistence.LockModeType;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.kurento.agenda.datamodel.pojo.KhcInvalidDataInfo.Code;
import com.kurento.agenda.datamodel.pojo.KhcNotFoundInfo;
import com.kurento.khc.KhcInvalidDataException;
import com.kurento.khc.KhcNotFoundException;
import com.kurento.khc.push.Notification;
import com.kurento.khc.push.NotificationServer;
import com.kurento.khc.utils.KhcLogger;

@Component
public class ChannelDao extends BaseDao {

	private static final Logger log = LoggerFactory.getLogger(ChannelDao.class);

	@Autowired
	private UserDao userDao;
	@Autowired
	private CommandDao commandDao;
	@Autowired
	private NotificationServer notificationServer;
	@Autowired
	private KhcLogger khcLog;

	@Transactional
	public ChannelEntity createNotificationChannel(ChannelEntity notch,
			UserEntity user) {

		verifyData(notch);

		// Find user entity (principal)
		UserEntity dbUser = userDao.attach(user);

		// Create Notch
		super.save(notch);

		// Assign notch to owner
		notch.setUser(dbUser);
		em.flush();

		return notch;

	}

	@Transactional
	public void deleteNotificationChannel(final ChannelEntity notch) {

		ChannelEntity dbNotch = attach(notch);

		// Unset user
		dbNotch.setUser(null);

		// Delete command queue
		Query q = em
				.createNamedQuery(CommandEntity.NQ_NAME_DELETE_COMMAND_QUEUE)
				.setParameter(CommandEntity.NQ_PARAM_RECEIVER, notch)
				.setMaxResults(100);
		do {
		} while (q.executeUpdate() > 0);

		// Delete channel calls
		//		q = em.createNamedQuery(CallEntity.NQ_NAME_DELETE_CHANNEL_CALLS)
		//				.setParameter(CallEntity.NQ_PARAM_CHANNEL, notch)
		//				.setMaxResults(100);

		// Update channel calls to null
		q = em.createNamedQuery(CallEntity.NQ_NAME_UPDATE_RECEIVER_AND_INVOKER)
				.setParameter(CallEntity.NQ_PARAM_CHANNEL, notch)
				.setParameter(CallEntity.NQ_NEW_PARAM_CHANNEL, null)
				.setMaxResults(100);
		do {
		} while (q.executeUpdate() > 0);

		// Update channel call forwards to null
		q = em.createNamedQuery(CallFwdEntity.NQ_NAME_UPDATE_INVOKER)
				.setParameter(CallFwdEntity.NQ_PARAM_CHANNEL, notch)
				.setParameter(CallFwdEntity.NQ_PARAM_NEW_CHANNEL_VALUE, null)
				.setMaxResults(100);
		do {
		} while (q.executeUpdate() > 0);

		q = em.createNamedQuery(CallFwdEntity.NQ_NAME_UPDATE_CALLER)
				.setParameter(CallFwdEntity.NQ_PARAM_CHANNEL, notch)
				.setParameter(CallFwdEntity.NQ_PARAM_NEW_CHANNEL_VALUE, null)
				.setMaxResults(100);
		do {
		} while (q.executeUpdate() > 0);

		q = em.createNamedQuery(CallFwdEntity.NQ_NAME_UPDATE_CALLEE)
				.setParameter(CallFwdEntity.NQ_PARAM_CHANNEL, notch)
				.setParameter(CallFwdEntity.NQ_PARAM_NEW_CHANNEL_VALUE, null)
				.setMaxResults(100);
		do {
		} while (q.executeUpdate() > 0);

		super.delete(dbNotch);
		em.flush();
	}

	@Transactional
	public ChannelEntity updateNotificationChannel(ChannelEntity notch) {

		em.detach(notch);
		ChannelEntity dbNotch = attach(notch);

		if (notch.getRegisterType() != null) {
			dbNotch.setRegisterType(notch.getRegisterType());
		}
		if (notch.getRegisterId() != null) {
			dbNotch.setRegisterId(notch.getRegisterId());
		}

		if (notch.isEnabled() != null) {
			dbNotch.setEnabled(notch.isEnabled());
		}
		return dbNotch;

	}

	@Transactional
	public CommandEntity sendCommand(ChannelEntity invoker,
			ChannelEntity receiver, String method, String params,
			String notificationMsg) {

		ChannelEntity dbReceiver = lock(attach(receiver));
		CommandEntity command = new CommandEntity();
		command.setMethod(method);
		CommandEntity dbCommand = commandDao.createCommand(command);
		dbCommand.setReceiver(dbReceiver);
		dbCommand.setParams(params);
		// Increment channel badge whe msg is sent
		if (notificationMsg != null && !notificationMsg.isEmpty()) {
			dbReceiver.incBadge();
		}

		log.trace("Notification triggered on channel {}", dbReceiver.getUUID());
		Notification notification = new Notification(dbReceiver,
				notificationMsg, dbReceiver.getBadge());
		notificationServer.sendNotification(notification);
		em.flush();

		return dbCommand;
	}

	@SuppressWarnings("unchecked")
	@Transactional
	public List<CommandEntity> getPendingCommands(ChannelEntity receiver,
			Long lastSequence) {

		ChannelEntity dbReceiver = lock(attach(receiver));
		log.trace("Query pending commands for channel {} - {}",
				receiver.getUUID(), lastSequence);
		Query q = em
				.createNamedQuery(CommandEntity.NQ_NAME_GET_PENDING_COMMANDS)
				.setParameter(CommandEntity.NQ_PARAM_RECEIVER, dbReceiver)
				.setParameter(CommandEntity.NQ_PARAM_LAST_SEQUENCE,
						lastSequence);
		List<CommandEntity> pendingCommands = q.getResultList();
		cleanCommandQueue(dbReceiver, lastSequence);
		dbReceiver.setLastSequenceExec(lastSequence);
		dbReceiver.setBadge(0);

		return pendingCommands;
	}

	@Transactional
	protected void cleanCommandQueue(ChannelEntity receiver, Long lastSequence) {
		ChannelEntity dbReceiver = attach(receiver);
		Query q = em
				.createNamedQuery(
						CommandEntity.NQ_NAME_DELETE_COMPLETED_COMMANDS)
						.setParameter(CommandEntity.NQ_PARAM_RECEIVER, dbReceiver)
						.setParameter(CommandEntity.NQ_PARAM_LAST_SEQUENCE,
								lastSequence);
		q.executeUpdate();

	}

	@Transactional(noRollbackFor = KhcNotFoundException.class)
	public ChannelEntity findNotificationChannelByUUID(Long uuid) {
		try {
			return super.findSingle(ChannelEntity.class,
					new String[] { "uuid", }, new Object[] { uuid });
		} catch (KhcNotFoundException e) {
			throw new KhcNotFoundException(e.getMessage(),
					KhcNotFoundInfo.Code.CHANNEL_NOT_FOUND, e.getEntity(),
					e.getFilter());
		}
	}

	@Transactional(noRollbackFor = KhcNotFoundException.class)
	public ChannelEntity findNotificationChannelByInstanceId(String instanceId) {
		return findSingle(ChannelEntity.class, new String[] { "instanceId", },
				new Object[] { instanceId });

	}

	@Transactional(noRollbackFor = KhcNotFoundException.class)
	public ChannelEntity findNotificationChannelByRegisterId(String registerId,
			String registerType) {
		return findSingle(ChannelEntity.class, new String[] { "registerId",
		"registerType" }, new Object[] { registerId, registerType });
	}

	@Transactional(noRollbackFor = KhcNotFoundException.class)
	public List<ChannelEntity> findNotificationChannelsByQoS() {
		List<UserEntity> qosUsers = userDao.findAll(UserEntity.class, -1,
				new String[]{"qos"}, new Object[]{true});
		if (qosUsers != null && !qosUsers.isEmpty()) {
			if (log.isTraceEnabled())
				log.trace("Users with QoS enabled: {}", qosUsers.size());
			return findAll(ChannelEntity.class, -1, new String[] {"user"},
					new Object[] {qosUsers});
		} else
			return Collections.emptyList();
	}

	@Transactional
	public UserEntity getUser(ChannelEntity notch) {
		ChannelEntity dbNotch = attach(notch);
		UserEntity dbUser = dbNotch.getUser();
		dbUser.getId(); // Force object creation
		return dbUser;
	}

	// ////////////////////////
	// Security verifications
	// ////////////////////////

	@Transactional
	public Boolean isOwner(ChannelEntity channel, UserEntity owner) {

		try {
			ChannelEntity dbChannel = attach(channel);
			UserEntity dbOwner = userDao.attach(owner);

			if (dbChannel.getUser().getId().equals(dbOwner.getId())) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
	}

	// /////////
	//
	// HELPERS
	//
	// /////////

	@Transactional
	protected ChannelEntity attach(final ChannelEntity channel) {
		Assert.notNull(channel);
		if (em.contains(channel)) {
			return channel;
		} else {
			ChannelEntity dbChannel;
			if ((dbChannel = em.find(ChannelEntity.class, channel.getId())) == null) {
				throw new KhcNotFoundException(
						"Unable to attach unknown channel to JPA",
						KhcNotFoundInfo.Code.CHANNEL_NOT_FOUND,
						ChannelEntity.class.getSimpleName(),
						new HashMap<String, String>() {
							private static final long serialVersionUID = 1L;
							{
								put("uuid", "" + channel.getUUID());
							}
						});
			} else {
				return dbChannel;
			}
		}
	}

	protected ChannelEntity lock(ChannelEntity channel) {
		ChannelEntity dbChannel = attach(channel);
		em.refresh(dbChannel, LockModeType.PESSIMISTIC_WRITE);
		log.trace("Lock channel {}", channel.getUUID());
		return dbChannel;
	}

	protected void verifyData(ChannelEntity channel) {
		// Verify data
		if (channel.getInstanceId() == null) {
			throw new KhcInvalidDataException(
					"Instance ID is mandatory on channel creation",
					Code.INVALID_DATA);
		}
		if (channel.getRegisterId() == null) {
			throw new KhcInvalidDataException(
					"Register ID is mandatory on channel creation",
					Code.INVALID_DATA);
		}
		if (channel.getRegisterType() == null) {
			throw new KhcInvalidDataException(
					"Register TYPE is mandatory on channel creation",
					Code.INVALID_DATA);
		}
		if (channel.getLocaleString() == null) {
			throw new KhcInvalidDataException(
					"Locale string is mandatory on channel creation",
					Code.INVALID_DATA);
		}
	}
}