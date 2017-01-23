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

package com.kurento.khc.services.v2.internal;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.kurento.agenda.datamodel.pojo.Channel;
import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.datamodel.pojo.Content;
import com.kurento.agenda.datamodel.pojo.Topic;
import com.kurento.khc.KhcException;
import com.kurento.khc.KhcInternalServerException;
import com.kurento.khc.KhcNotFoundException;
import com.kurento.khc.datamodel.AccountSecureDao;
import com.kurento.khc.datamodel.ChannelDao;
import com.kurento.khc.datamodel.ChannelEntity;
import com.kurento.khc.datamodel.ChannelSecureDao;
import com.kurento.khc.datamodel.CommandDao;
import com.kurento.khc.datamodel.CommandEntity;
import com.kurento.khc.datamodel.CommandTransactionDao;
import com.kurento.khc.datamodel.ContentDao;
import com.kurento.khc.datamodel.GroupDao;
import com.kurento.khc.datamodel.GroupSecureDao;
import com.kurento.khc.datamodel.MessageDao;
import com.kurento.khc.datamodel.TimelineDao;
import com.kurento.khc.datamodel.UserDao;
import com.kurento.khc.datamodel.UserEntity;
import com.kurento.khc.datamodel.UserSecureDao;
import com.kurento.khc.jackson.KhcObjectMapper;
import com.kurento.khc.messaging.BrokerServer;
import com.kurento.khc.messaging.Subscription;
import com.kurento.khc.qos.QosServer;
import com.kurento.khc.services.v2.CommandService;
import com.kurento.khc.services.v2.internal.command.AbstractCommand;
import com.kurento.khc.services.v2.internal.command.CommandServiceBackend;
import com.kurento.khc.utils.FileRepository;
import com.kurento.khc.utils.KhcLogger;

@Service("khcCommandServicev2")
public class CommandServiceImpl implements CommandService,
		CommandServiceBackend {

	private static final Logger log = LoggerFactory
			.getLogger(CommandServiceImpl.class);

	// @Autowired
	// private AdministrativeService administrativeService;
	// @Autowired
	// private MessageService messageService;
	//
	@Autowired
	private CommandServiceTransactionalImpl cmdServiceTrns;
	@Autowired
	private AccountSecureDao accountSecureDao;
	@Autowired
	private GroupSecureDao groupSecureDao;
	@Autowired
	private UserSecureDao userSecureDao;
	// @Autowired
	// private MessageSecureDao messageSecureDao;
	@Autowired
	private ChannelSecureDao notchSecureDao;

	@Autowired
	private GroupDao groupDao;
	@Autowired
	private UserDao userDao;
	@Autowired
	private ChannelDao notchDao;
	@Autowired
	private CommandDao commandDao;
	@Autowired
	private CommandTransactionDao transactionDao;
	@Autowired
	private ContentDao contentDao;
	@Autowired
	private MessageDao messageDao;
	@Autowired
	private TimelineDao timelineDao;

	@Autowired
	private BrokerServer brokerServer;

	@Autowired
	private FileRepository repository;

	@Autowired
	private KhcLogger khcLog;

	@Autowired
	private Map<String, AbstractCommand> commandReference = new HashMap<String, AbstractCommand>();

	@Autowired
	private QosServer qosServer;

	private List<AbstractCommand> initSequence = new ArrayList<AbstractCommand>();

	private List<AbstractCommand> consolidationSequence = new ArrayList<AbstractCommand>();

	private static MessageDigest md;
	private static ObjectMapper jsonMapper = new KhcObjectMapper();

	// /////////////////////////////////////////////
	// Command management services
	// /////////////////////////////////////////////
	@PostConstruct
	public void init() {
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			log.error("Unable to activate digest algorithm: MD5", e);
		}

		// Create consolidation based in priority announced by commands. Zero
		// means no consolidation is required
		consolidationSequence = new ArrayList<AbstractCommand>();
		for (AbstractCommand command : commandReference.values()) {
			if (command.getConsolidationPriority() != 0) {
				consolidationSequence.add(command);
			}
		}
		Collections.sort(consolidationSequence,
				new ConsolidationPriorityComparer());

	}

	// This method can not be transactional in order to avoid rollback
	// transactions for constraints verifications
	@Override
	public void executeCommand(Command command, Content content,
			Boolean asServer) {

		try {
			cmdServiceTrns.executeCommand(command, content, asServer);
		} catch (RuntimeException e) {
			if (!(e instanceof KhcException)) {
				// Verify constraints if java exception is raised
				commandReference.get(command.getMethod()).verifyConstraints(
						command);
			}
			// If nothing happens continue with original exception
			throw e;
		}
	}

	// This method can not be transactional in order to avoid rollback
	// transactions for constraints verifications
	@Override
	public void executeTransaction(List<Command> commands, Boolean asServer) {

		try {
			cmdServiceTrns.executeTransaction(commands, asServer);
		} catch (RuntimeException e) {
			// Vrify constraints for each command in transaction
			for (Command command : commands) {
				if (!(e instanceof KhcException)) {
					// Verify constraints if java exception is raised
					commandReference.get(command.getMethod())
							.verifyConstraints(command);
				}
			}
			// If nothing happens continue with original exception
			throw e;
		}
	}

	// Isolation level required to avoid concurrent data access to
	// channels
	@Override
	@Transactional(isolation = Isolation.READ_UNCOMMITTED)
	public List<Command> getPendingCommands(Long channelId, Long lastSequence) {
		List<Command> pendingCmdList = null;
		String subscMsg = "";
		try {
			Subscription subscription;
			if ((subscription = brokerServer.getSubscription(channelId)) != null) {
				// Commands on subscription channels are sent with no consolidation
				pendingCmdList = subscription.getPendingCommands();
				subscMsg = "subscription ";
				return pendingCmdList;
			}

			// Get list of pending commands
			khcLog.getLog("MsgsRequest").withChannelId(channelId).send();
			ChannelEntity receiver = notchSecureDao.findNotificationChannelByUUID(channelId);

			List<CommandEntity> pendingCmdEntityList = notchDao.getPendingCommands(receiver,
					lastSequence);

			// Commands with no sequence must be consolidated before delivery
			List<CommandEntity> cmdToConsolide = new ArrayList<CommandEntity>();
			for (CommandEntity command : pendingCmdEntityList) {
				if (command.getSequence() == 0L) {
					cmdToConsolide.add(command);
				}
			}

			// Perform consolidation
			for (AbstractCommand cmdExecutor : consolidationSequence) {
				cmdToConsolide = cmdExecutor.consolide(cmdToConsolide);
			}

			// Add sequence number to consolidated commands
			for (CommandEntity command : cmdToConsolide) {
				// This will also update database
				command.setSequence(receiver.getNextSequence());
			}

			// Delete commands not consolidated: those with no sequence yet
			// Delete commands obsoleted: sequence <= lastSequence
			for (CommandEntity command : new ArrayList<CommandEntity>(pendingCmdEntityList)) {
				if (command.getSequence() == 0L || command.getSequence() <= lastSequence) {
					pendingCmdEntityList.remove(command);
					commandDao.deleteCommand(command);
				}
			}

			// Generate command POJOs
			pendingCmdList = new ArrayList<Command>();
			for (CommandEntity cmdEntity : pendingCmdEntityList) {
				pendingCmdList.add(buildCommandPojo(cmdEntity));
			}

			//update QoS
			UserEntity userEntity = notchDao.getUser(receiver);
			qosServer.reviewQosInstance(receiver, userEntity.isQos());

			return pendingCmdList;
		} finally {
			if (pendingCmdList != null && log.isDebugEnabled()) {
				int cmdSize = pendingCmdList.size();
				if (cmdSize > 0)
					log.debug("Return {} pending commands on {}channel {}", cmdSize, subscMsg,
							channelId);
				else if (log.isTraceEnabled())
					log.trace("Return {} pending commands on {}channel {}", cmdSize, subscMsg,
							channelId);
				if (log.isTraceEnabled())
					for (Command cmd : pendingCmdList)
						log.trace(">>>> COMMAND >>>>> " + cmd.toString());
			}
		}
	}

	// /////////////////////////////////////////////
	// Notification channel services
	// /////////////////////////////////////////////

	@Override
	@Transactional
	public Channel createNotificationChannel(Channel notch) {

		// Clean up previous channels with from same instance or with same regId
		// Override any channel from this APP instance
		ChannelEntity notchEntity;
		try {
			do {
				notchEntity = notchDao
						.findNotificationChannelByInstanceId(notch
								.getInstanceId());
				//update QoS
				qosServer.reviewQosInstance(notchEntity, false);
				notchDao.deleteNotificationChannel(notchEntity);
			} while (true);
		} catch (KhcNotFoundException e) {
			log.debug("Channel clean up completed for APP instance {}",
					notch.getInstanceId());
		}

		// Override any channel with the same register ID
		try {
			do {
				notchEntity = notchDao.findNotificationChannelByRegisterId(
						notch.getRegisterId(), notch.getRegisterType());
				//update QoS
				qosServer.reviewQosInstance(notchEntity, false);
				notchDao.deleteNotificationChannel(notchEntity);
			} while (true);
		} catch (KhcNotFoundException e) {
			log.debug("Channel clean up completed for register key {}",
					notch.getRegisterId());
		}

		UserEntity userEntity = userDao.findUserByUUID(notch.getUserId());

		notchEntity = buildChannelEntity(notch);
		notchEntity = notchSecureDao.createNotificationChannel(notchEntity,
				userEntity);

		log.info("Channel initialization after creation : "
				+ notchEntity.getUUID());
		initializeChannel(notchEntity);

		// Return create notch
		return buildNotificationChannelPojo(notchEntity);

	}

	@Override
	public void deleteSubscription(Long notchId) {
		brokerServer.removeSubscription(notchId);
	}

	@Override
	public Channel createSubscription(String instanceId) {
		return brokerServer.subscribe(instanceId);
	}

	@Override
	public void addSubscriptionTopic(Channel notch, Topic topic) {
		Assert.notNull(notch);
		addSubscriptionTopic(notch.getId(), topic);
	}

	@Override
	public void removeSubscriptionTopic(Channel notch, Topic topic) {
		Assert.notNull(notch);
		removeSubscriptionTopic(notch.getId(), topic);
	}

	@Override
	public void addSubscriptionTopic(Long notchId, Topic topic) {
		Assert.notNull(notchId);
		Assert.notNull(topic);
		checkTopicAccess(topic);
		Subscription subscription;
		if ((subscription = brokerServer.getSubscription(notchId)) != null)
			subscription.addTopic(topic);
	}

	@Override
	public void removeSubscriptionTopic(Long notchId, Topic topic) {
		Assert.notNull(notchId);
		Assert.notNull(topic);
		checkTopicAccess(topic);
		Subscription subscription;
		if ((subscription = brokerServer.getSubscription(notchId)) != null)
			subscription.removeTopic(topic);
	}

	private void checkTopicAccess(Topic topic) {
		// Verify if user is able to subscribe
		switch (topic.getBase()) {
		case ACCOUNT:
			accountSecureDao.findAccountByUUID(topic.getId());
			break;
		case GROUP:
			groupSecureDao.findGroupByUUID(topic.getId());
			break;
		case USER:
			userSecureDao.findUserByUUID(topic.getId());
			break;
		}
	}

	@Override
	@Transactional
	public void deleteNotificationChannel(Long notchId) {
		ChannelEntity notch = notchSecureDao
				.findNotificationChannelByUUID(notchId);
		notchSecureDao.deleteNotificationChannel(notch);
	}

	@Override
	@Transactional
	public void updateNotificationChannel(Channel notch) {
		ChannelEntity notchEntity = buildChannelEntity(notch);
		notchSecureDao.updateNotificationChannel(notchEntity);
	}

	// /////////////////////////////////////////////
	// Command Backend interface
	// /////////////////////////////////////////////

	@Override
	public void registerCommand(AbstractCommand command, String method) {
		commandReference.put(method, command);
	}

	@Override
	public void registerInitSequence(AbstractCommand initCommand) {
		initSequence.add(initCommand);
	}

	@Override
	@Transactional
	public void initializeChannel(ChannelEntity channel) {
		channel.setEnabled(true);
		channel = notchDao.updateNotificationChannel(channel);
		for (AbstractCommand command : initSequence) {
			command.initializeChannel(channel);
		}
	}

	// /////////////////////////////////////////////
	// Format converters
	// /////////////////////////////////////////////

	@Override
	@Transactional
	public Command buildCommandPojo(CommandEntity commandEntity) {

		ChannelEntity notch = commandDao.getReceiver(commandEntity);

		Command command = new Command();
		command.setId(commandEntity.getUUID());
		command.setChannelId(notch.getUUID());
		command.setSequenceNumber(commandEntity.getSequence());
		command.setMethod(commandEntity.getMethod());
		if (commandReference.containsKey(command.getMethod())) {
			command.setParams(commandReference.get(command.getMethod())
					.getParams(commandEntity));
		} else {
			command.setParams(new HashMap<String, String>());
		}
		return command;
	}

	@Override
	@Transactional
	public Channel buildNotificationChannelPojo(ChannelEntity notchEntity) {

		UserEntity userEntity = notchDao.getUser(notchEntity);

		Channel notch = new Channel();
		notch.setId(notchEntity.getUUID());
		notch.setRegisterId(notchEntity.getRegisterId());
		notch.setRegisterType(notchEntity.getRegisterType());
		notch.setInstanceId(notchEntity.getInstanceId());
		notch.setUserId(userEntity.getUUID());
		notch.setLocale(notchEntity.getLocaleString());

		return notch;
	}

	public ChannelEntity buildChannelEntity(Channel notch) {

		Assert.notNull(notch);

		ChannelEntity notchEntity;
		if (notch.getId() != null) {
			notchEntity = notchDao.findNotificationChannelByUUID(notch.getId());
		} else {
			notchEntity = new ChannelEntity();
			notchEntity.setInstanceId(notch.getInstanceId() != null ? notch
					.getInstanceId() : notchEntity.getInstanceId());
			notchEntity.setRegisterId(notch.getRegisterId() != null ? notch
					.getRegisterId() : notchEntity.getRegisterId());
			notchEntity.setRegisterType(notch.getRegisterType() != null ? notch
					.getRegisterType() : notchEntity.getRegisterType());
			notchEntity.setLocaleString(notch.getLocale());
		}

		return notchEntity;

	}

	public byte[] serialize(Object command) {
		try {
			return jsonMapper.writeValueAsBytes(command);
		} catch (Exception e) {
			throw new KhcInternalServerException(
					"Unable to build JSON params from data stored in database",
					e);
		}
	}

	private class ConsolidationPriorityComparer implements
			Comparator<AbstractCommand> {

		@Override
		public int compare(AbstractCommand o1, AbstractCommand o2) {
			if (o1.getConsolidationPriority() < o2.getConsolidationPriority()) {
				return -1;
			} else if (o1.getConsolidationPriority() > o2
					.getConsolidationPriority()) {
				return 1;
			} else {
				return 0;
			}
		}

	}
}
