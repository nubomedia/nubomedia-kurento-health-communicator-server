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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.kurento.agenda.datamodel.pojo.KhcNotFoundInfo;
import com.kurento.khc.KhcNotFoundException;

@Component
public class CommandDao extends BaseDao {

	@Autowired
	private UserDao userDao;

	@Autowired
	private ChannelDao notchDao;

	@Autowired
	private Environment config;

	@Transactional
	public CommandEntity createCommand(CommandEntity command) {
		// Save new command
		// log.debug("Persist command entity");
		super.save(command);
		em.flush();

		return command;
	}

	@Transactional
	public void deleteCommand(CommandEntity command) {
		super.delete(command);
		em.flush();
	}

	@Transactional
	public String getParams(CommandEntity command) {
		CommandEntity dbCommand = attach(command);
		return dbCommand.getParams();
	}

	@Transactional(noRollbackFor = KhcNotFoundException.class)
	public CommandEntity findCommandByUUID(Long uuid) {
		return findSingle(CommandEntity.class, new String[] { "uuid" },
				new Object[] { uuid });
	}

	@Transactional
	public ChannelEntity getReceiver(CommandEntity command) {

		CommandEntity dbCommand = attach(command);
		ChannelEntity receiver = dbCommand.getReceiver();
		receiver.getId();
		return receiver;
	}

	// /////////////////
	//
	// HELPERS
	//
	// /////////////////
	protected CommandEntity attach(final CommandEntity command) {
		Assert.notNull(command);
		if (em.contains(command)) {
			return command;
		} else {
			CommandEntity dbCommand;
			if ((dbCommand = em.find(CommandEntity.class, command.getId())) == null) {
				throw new KhcNotFoundException(
						"Unable to attach unknown command to JPA",
						KhcNotFoundInfo.Code.SYNCHRONIZATION_ERROR,
						CommandEntity.class.getSimpleName(),
						new HashMap<String, String>() {
							private static final long serialVersionUID = 1L;
							{
								put("uuid", "" + command.getUUID());
							}
						});
			} else {
				return dbCommand;
			}
		}

	}
}
