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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.xml.bind.DatatypeConverter;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.datamodel.pojo.Content;
import com.kurento.agenda.datamodel.pojo.KhcInvalidDataInfo.Code;
import com.kurento.khc.KhcInternalServerException;
import com.kurento.khc.KhcInvalidDataException;
import com.kurento.khc.datamodel.CommandTransactionDao;
import com.kurento.khc.datamodel.CommandTransactionEntity;
import com.kurento.khc.jackson.KhcObjectMapper;
import com.kurento.khc.services.v2.internal.command.AbstractCommand;

@Service("khcCommandServiceTransactionalImpl")
public class CommandServiceTransactionalImpl {

	private static final Logger log = LoggerFactory
			.getLogger(CommandServiceTransactionalImpl.class);

	@Autowired
	private Map<String, AbstractCommand> commandReference = new HashMap<String, AbstractCommand>();
	@Autowired
	private CommandTransactionDao transactionDao;

	private static MessageDigest md;
	private static ObjectMapper jsonMapper = new KhcObjectMapper();

	@PostConstruct
	public void init() {
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			log.error("Unable to activate digest algorithm: MD5", e);
		}
	}

	// Isolation level required to avoid concurrent data access to
	// channels
	@Transactional(isolation = Isolation.READ_UNCOMMITTED)
	public void executeCommand(Command command, Content content,
			Boolean asServer) {
		Assert.notNull(command);
		String method = command.getMethod();
		Assert.notNull(method);
		Long startTime = System.currentTimeMillis();
		AbstractCommand cmdExecutor = commandReference.get(method);
		String msg = "Rejected";

		try {
			if (cmdExecutor != null) {
				// Verify if this transaction has already been received
				CommandTransactionEntity transaction = new CommandTransactionEntity();
				transaction.setHash(DatatypeConverter.printHexBinary(md
						.digest(serialize(command))));
				if (!transactionDao.acceptTransactionKnownToServer(transaction)) {
					log.info("Request to execute transaction already executed");
					return;
				}

				cmdExecutor.exec(command, content, asServer);
				msg = "Successful";
			} else {
				throw new KhcInvalidDataException("Command not found: "
						+ command.getMethod(), Code.COMMAND_NOT_FOUND);
			}
		} finally {
			Object params = command.getParams();
			log.debug(
					"{} command {} on channel {}. Exec time: {} ms\n {}",
					new Object[] { msg, command.getMethod(),
							command.getChannelId(),
							System.currentTimeMillis() - startTime, params });
		}
	}

	// Isolation level required to avoid concurrent data access to
	// channels
	@Transactional(isolation = Isolation.READ_UNCOMMITTED)
	public void executeTransaction(List<Command> commands, Boolean asServer) {
		// Verify if this transaction has already been received
		CommandTransactionEntity transaction = new CommandTransactionEntity();
		transaction.setHash(DatatypeConverter.printHexBinary(md
				.digest(serialize(commands))));
		if (!transactionDao.acceptTransactionKnownToServer(transaction)) {
			return;
		}
		for (Command command : commands) {
			executeCommand(command, null, asServer);
		}
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
}
