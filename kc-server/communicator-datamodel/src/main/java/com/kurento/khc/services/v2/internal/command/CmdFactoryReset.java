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

package com.kurento.khc.services.v2.internal.command;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.datamodel.pojo.Content;
import com.kurento.khc.datamodel.ChannelEntity;
import com.kurento.khc.datamodel.CommandEntity;

@Component(Command.METHOD_FACTORY_RESET)
public class CmdFactoryReset extends AbstractCommand {

	protected Integer consolidationPriority = 1;

	@Autowired
	public void setCommandReference(CommandServiceBackend commandReference) {
		// Register this command as channel initiator sequence
		this.commandBackend.registerInitSequence(this);
	}

	@Override
	@Transactional
	public void exec(Command command, Boolean asServer) {
		// No params required
		ChannelEntity invoker = asServer ? null : getInvoker(command
				.getChannelId());

		// Initialize channel (This propagates changes to owner)
		log.info("Channel initialization due to Factory Reset command : "
				+ invoker.getUUID());
		commandBackend.initializeChannel(invoker);
	}

	@Override
	@Transactional
	public void exec(Command command, Content content, Boolean asServer) {

		exec(command, asServer);
	}

	@Override
	public void initializeChannel(ChannelEntity channel) {
		sendFactoryReset(channel);

		// Call all successors
		for (AbstractCommand cmd : successors) {
			cmd.initializeChannel(channel);
		}
	}

	@Override
	public Integer getConsolidationPriority() {
		return 1;
	}

	@Override
	public List<CommandEntity> consolide(List<CommandEntity> commands) {
		List<CommandEntity> consolidedCmdList = new ArrayList<CommandEntity>();
		// When factory reset is found discard previous commands
		for (CommandEntity command : commands) {
			if (command.getMethod().equals(Command.METHOD_FACTORY_RESET)) {
				// Discard previous commands when factory reset is found
				consolidedCmdList.clear();
			}
			// Add to consolided list
			consolidedCmdList.add(command);
		}
		return consolidedCmdList;
	}
}
