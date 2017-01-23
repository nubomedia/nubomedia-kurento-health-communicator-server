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

package com.kurento.khc.test.junit.v2;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.services.pojo.CallFwdRecv;
import com.kurento.agenda.services.pojo.CallFwdSend;
import com.kurento.agenda.services.pojo.CallReceive;
import com.kurento.agenda.services.pojo.CallSend;
import com.kurento.khc.datamodel.AccountEntity;
import com.kurento.khc.datamodel.ChannelEntity;
import com.kurento.khc.datamodel.GroupEntity;
import com.kurento.khc.datamodel.UserEntity;
import com.kurento.khc.jackson.KhcObjectMapper;
import com.kurento.khc.services.v2.CommandService;
import com.kurento.khc.spring.KhcDatamodelConfig;
import com.kurento.khc.test.utils.KhcTestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = KhcDatamodelConfig.class)
@ActiveProfiles({ "embed_db", "embed_security" })
public class CallFwdTest {

	private static final Random rnd = new SecureRandom();

	@Autowired
	private KhcTestUtils utils;
	@Autowired
	private CommandService cmdService;

	private AccountEntity acc;
	private UserEntity from, to, operator;
	private ChannelEntity fromChan, toChan, opChan;
	private List<ChannelEntity> fromChannels, toChannels, opChannels;

	private ObjectMapper jsonMapper = new KhcObjectMapper();

	private Long lastSequenceOp = 0L;
	private Long lastSequenceFrom = 0L;
	private Long lastSequenceTo = 0L;

	@Test
	public void testFwdSetupNotFailed() throws IOException {
		testSetup();

		// Call between 'to' and 'operator'
		Command command = buildCallDial(toChan, operator);
		utils.login(to);
		cmdService.executeCommand(command, null, false);
		Iterator<Command> pc = cmdService.getPendingCommands(toChan.getUUID(),
				lastSequenceTo).iterator();
		command = pc.next();
		assertCallDial(command, to, operator);
		lastSequenceTo = command.getSequenceNumber();

		// op responds with accept
		utils.login(operator);
		pc = cmdService.getPendingCommands(opChan.getUUID(),
				lastSequenceOp).iterator();
		command = pc.next();
		assertCallDial(command, to, operator);
		lastSequenceOp = command.getSequenceNumber();
		command = buildCallAccept(opChan, command);
		cmdService.executeCommand(command, null, false);
		pc = cmdService.getPendingCommands(opChan.getUUID(),
				lastSequenceOp).iterator();
		command = pc.next();
		assertCallAccept(command);
		lastSequenceOp = command.getSequenceNumber();

		// 'to' receives accept:
		// - cmd-1:dial,
		// - cmd-2:accept,
		utils.login(to);
		pc = cmdService.getPendingCommands(toChan.getUUID(), lastSequenceTo).iterator();
		command = pc.next();
		assertCallAccept(command);
		lastSequenceTo = command.getSequenceNumber();

		//...
		//Hypothetical call wait (atm, using data channels)
		//...

		// Call between 'operator' and 'from'
		command = buildCallDial(opChan, from);
		utils.login(operator);
		cmdService.executeCommand(command, null, false);
		pc = cmdService.getPendingCommands(opChan.getUUID(),
				lastSequenceOp).iterator();
		command = pc.next();
		assertCallDial(command, operator, from);
		lastSequenceOp = command.getSequenceNumber();

		// 'from' responds with accept
		utils.login(from);
		pc = cmdService.getPendingCommands(fromChan.getUUID(),
				lastSequenceFrom).iterator();
		command = pc.next();
		assertCallDial(command, operator, from);
		lastSequenceFrom = command.getSequenceNumber();
		command = buildCallAccept(fromChan, command);
		cmdService.executeCommand(command, null, false);
		pc = cmdService.getPendingCommands(fromChan.getUUID(),
				lastSequenceFrom).iterator();
		command = pc.next();
		assertCallAccept(command);
		lastSequenceFrom = command.getSequenceNumber();

		// 'operator' receives accept
		utils.login(operator);
		pc = cmdService.getPendingCommands(opChan.getUUID(), lastSequenceOp).iterator();
		command = pc.next();
		assertCallAccept(command);
		lastSequenceOp = command.getSequenceNumber();

		// --------------------CALL FWD SETUP---

		// 'operator' sends call fwd setup
		utils.login(operator);
		command = buildCFSetup();
		cmdService.executeCommand(command, null, false);
		// Invoker (op) receives command
		pc = cmdService.getPendingCommands(opChan.getUUID(), lastSequenceOp).iterator();
		command = pc.next();
		assertCallFwdSetup(command);
		lastSequenceOp = command.getSequenceNumber();
		Assert.assertTrue(!pc.hasNext());

		// 'to' receives cmd on offered chan only
		utils.login(to);
		Command cfwd = null;
		for (ChannelEntity channel : toChannels) {
			Long cid = channel.getUUID();
			if (cid.equals(toChan.getUUID())) {
				pc = cmdService.getPendingCommands(cid, lastSequenceTo).iterator();
				cfwd = pc.next();
				assertCallFwdSetup(cfwd);
				lastSequenceTo = cfwd.getSequenceNumber();
				Assert.assertTrue(!pc.hasNext());
			} else {
				pc = cmdService.getPendingCommands(cid, 0L).iterator();
				Assert.assertTrue(!pc.hasNext());
			}
		}
		Assert.assertNotNull(cfwd);

		// 'from' receives cmd on offered chan only
		utils.login(from);
		for (ChannelEntity channel : fromChannels) {
			Long cid = channel.getUUID();
			if (cid.equals(fromChan.getUUID())) {
				pc = cmdService.getPendingCommands(cid, lastSequenceFrom).iterator();
				command = pc.next();
				assertCallFwdSetup(command);
				lastSequenceFrom = command.getSequenceNumber();
				Assert.assertTrue(!pc.hasNext());
			} else {
				pc = cmdService.getPendingCommands(cid, 0L).iterator();
				Assert.assertTrue(!pc.hasNext());
			}
		}

		// --------------------ACKNOWLEDGE CALL FWD---

		// 'to' sends call fwd ack
		utils.login(to);
		command = buildCFAcked(cfwd);
		cmdService.executeCommand(command, null, false);
		// Invoker (to) receives command
		pc = cmdService.getPendingCommands(toChan.getUUID(), lastSequenceTo)
				.iterator();
		command = pc.next();
		assertCallFwdAcked(command);
		lastSequenceTo = command.getSequenceNumber();
		Assert.assertTrue(!pc.hasNext());

		// 'op' receives command
		utils.login(operator);
		pc = cmdService.getPendingCommands(opChan.getUUID(), lastSequenceOp)
				.iterator();
		command = pc.next();
		assertCallFwdAcked(command);
		lastSequenceOp = command.getSequenceNumber();
		Assert.assertTrue(!pc.hasNext());

		// 'from' receives command
		utils.login(from);
		pc = cmdService
				.getPendingCommands(fromChan.getUUID(), lastSequenceFrom)
				.iterator();
		command = pc.next();
		assertCallFwdAcked(command);
		lastSequenceFrom = command.getSequenceNumber();
		Assert.assertTrue(!pc.hasNext());

		// --------------------ESTABLISH CALL---

		// Call between 'from' and 'to'
		command = buildCallDial(fromChan, to);
		utils.login(from);
		cmdService.executeCommand(command, null, false);
		pc = cmdService.getPendingCommands(fromChan.getUUID(),
				lastSequenceFrom).iterator();
		command = pc.next();
		assertCallDial(command, from, to);
		lastSequenceFrom = command.getSequenceNumber();

		// 'to' responds with accept
		utils.login(to);
		pc = cmdService.getPendingCommands(toChan.getUUID(),
				lastSequenceTo).iterator();
		command = pc.next();
		assertCallDial(command, from, to);
		lastSequenceTo = command.getSequenceNumber();
		command = buildCallAccept(toChan, command);
		cmdService.executeCommand(command, null, false);
		pc = cmdService.getPendingCommands(toChan.getUUID(),
				lastSequenceTo).iterator();
		Command callAccept = pc.next();
		assertCallAccept(callAccept);
		lastSequenceTo = callAccept.getSequenceNumber();

		// 'from' receives accept
		utils.login(from);
		pc = cmdService.getPendingCommands(fromChan.getUUID(), lastSequenceFrom).iterator();
		command = pc.next();
		assertCallAccept(command);
		lastSequenceFrom = command.getSequenceNumber();

		// --------------------MARK CALL FWD AS ESTABLISHED---

		// 'to' sends call fwd established
		utils.login(to);
		command = buildCFEstablished(cfwd);
		cmdService.executeCommand(command, null, false);
		// Invoker (to) receives command
		pc = cmdService.getPendingCommands(toChan.getUUID(), lastSequenceTo).iterator();
		command = pc.next();
		assertCallFwdEstablished(command);
		lastSequenceTo = command.getSequenceNumber();
		Assert.assertTrue(!pc.hasNext());

		// 'op' receives command
		utils.login(operator);
		pc = cmdService.getPendingCommands(opChan.getUUID(), lastSequenceOp).iterator();
		command = pc.next();
		assertCallFwdEstablished(command);
		lastSequenceOp = command.getSequenceNumber();
		Assert.assertTrue(!pc.hasNext());

		// 'from' receives command
		utils.login(from);
		pc = cmdService.getPendingCommands(fromChan.getUUID(), lastSequenceFrom).iterator();
		command = pc.next();
		assertCallFwdEstablished(command);
		lastSequenceFrom = command.getSequenceNumber();
		Assert.assertTrue(!pc.hasNext());

		// --------------------TERMINATE CALL---

		// 'from' terminates call
		utils.login(from);
		command = buildCallTerminate(fromChan, callAccept);
		cmdService.executeCommand(command, null, false);
		pc = cmdService.getPendingCommands(fromChan.getUUID(), lastSequenceFrom).iterator();
		command = pc.next();
		assertCallTerminate(command);
		lastSequenceFrom = command.getSequenceNumber();

		// 'to' receives terminate
		utils.login(to);
		pc = cmdService.getPendingCommands(toChan.getUUID(), lastSequenceTo).iterator();
		command = pc.next();
		assertCallTerminate(command);
		lastSequenceTo = command.getSequenceNumber();

		// --------------------CALL FWD TERMINATED---

		// 'from' sends call forward terminated
		Boolean fwdStatus = false;
		utils.login(from);
		command = buildCFTerminated(cfwd, fwdStatus);
		cmdService.executeCommand(command, null, false);
		pc = cmdService.getPendingCommands(fromChan.getUUID(), lastSequenceFrom).iterator();
		command = pc.next();
		assertCallFwdTerminated(command, fwdStatus);
		lastSequenceFrom = command.getSequenceNumber();
		Assert.assertTrue(!pc.hasNext());

		// 'to' receives command
		utils.login(to);
		pc = cmdService.getPendingCommands(toChan.getUUID(), lastSequenceTo).iterator();
		command = pc.next();
		assertCallFwdTerminated(command, fwdStatus);
		lastSequenceTo = command.getSequenceNumber();
		Assert.assertTrue(!pc.hasNext());

		// 'op' receives command
		utils.login(operator);
		pc = cmdService.getPendingCommands(opChan.getUUID(), lastSequenceOp).iterator();
		command = pc.next();
		assertCallFwdTerminated(command, fwdStatus);
		lastSequenceOp = command.getSequenceNumber();
		Assert.assertTrue(!pc.hasNext());
	}

	@Test
	public void testFwdSetupFailed() throws IOException {
		testSetup();

		// Call between 'to' and 'operator'
		Command command = buildCallDial(toChan, operator);
		utils.login(to);
		cmdService.executeCommand(command, null, false);
		Iterator<Command> pc = cmdService.getPendingCommands(toChan.getUUID(),
				lastSequenceTo).iterator();
		command = pc.next();
		assertCallDial(command, to, operator);
		lastSequenceTo = command.getSequenceNumber();

		// op responds with accept
		utils.login(operator);
		pc = cmdService.getPendingCommands(opChan.getUUID(),
				lastSequenceOp).iterator();
		command = pc.next();
		assertCallDial(command, to, operator);
		lastSequenceOp = command.getSequenceNumber();
		command = buildCallAccept(opChan, command);
		cmdService.executeCommand(command, null, false);
		pc = cmdService.getPendingCommands(opChan.getUUID(),
				lastSequenceOp).iterator();
		command = pc.next();
		assertCallAccept(command);
		lastSequenceOp = command.getSequenceNumber();

		// 'to' receives accept:
		// - cmd-1:dial,
		// - cmd-2:accept,
		utils.login(to);
		pc = cmdService.getPendingCommands(toChan.getUUID(), lastSequenceTo).iterator();
		command = pc.next();
		assertCallAccept(command);
		lastSequenceTo = command.getSequenceNumber();

		//...
		//Hypothetical call wait (atm, using data channels)
		//...

		// Call between 'operator' and 'from'
		command = buildCallDial(opChan, from);
		utils.login(operator);
		cmdService.executeCommand(command, null, false);
		pc = cmdService.getPendingCommands(opChan.getUUID(),
				lastSequenceOp).iterator();
		command = pc.next();
		assertCallDial(command, operator, from);
		lastSequenceOp = command.getSequenceNumber();

		// 'from' responds with accept
		utils.login(from);
		pc = cmdService.getPendingCommands(fromChan.getUUID(),
				lastSequenceFrom).iterator();
		command = pc.next();
		assertCallDial(command, operator, from);
		lastSequenceFrom = command.getSequenceNumber();
		command = buildCallAccept(fromChan, command);
		cmdService.executeCommand(command, null, false);
		pc = cmdService.getPendingCommands(fromChan.getUUID(),
				lastSequenceFrom).iterator();
		command = pc.next();
		assertCallAccept(command);
		lastSequenceFrom = command.getSequenceNumber();

		// 'operator' receives accept
		utils.login(operator);
		pc = cmdService.getPendingCommands(opChan.getUUID(), lastSequenceOp).iterator();
		command = pc.next();
		assertCallAccept(command);
		lastSequenceOp = command.getSequenceNumber();

		// --------------------CALL FWD SETUP---

		// 'operator' sends call fwd setup
		utils.login(operator);
		command = buildCFSetup();
		cmdService.executeCommand(command, null, false);
		// Invoker (op) receives command
		pc = cmdService.getPendingCommands(opChan.getUUID(), lastSequenceOp).iterator();
		command = pc.next();
		assertCallFwdSetup(command);
		lastSequenceOp = command.getSequenceNumber();
		Assert.assertTrue(!pc.hasNext());

		// 'to' receives cmd on offered chan only
		utils.login(to);
		Command cfwd = null;
		for (ChannelEntity channel : toChannels) {
			Long cid = channel.getUUID();
			if (cid.equals(toChan.getUUID())) {
				pc = cmdService.getPendingCommands(cid, lastSequenceTo).iterator();
				cfwd = pc.next();
				assertCallFwdSetup(cfwd);
				lastSequenceTo = cfwd.getSequenceNumber();
				Assert.assertTrue(!pc.hasNext());
			} else {
				pc = cmdService.getPendingCommands(cid, 0L).iterator();
				Assert.assertTrue(!pc.hasNext());
			}
		}
		Assert.assertNotNull(cfwd);

		// 'from' receives cmd on offered chan only
		utils.login(from);
		for (ChannelEntity channel : fromChannels) {
			Long cid = channel.getUUID();
			if (cid.equals(fromChan.getUUID())) {
				pc = cmdService.getPendingCommands(cid, lastSequenceFrom).iterator();
				command = pc.next();
				assertCallFwdSetup(command);
				lastSequenceFrom = command.getSequenceNumber();
				Assert.assertTrue(!pc.hasNext());
			} else {
				pc = cmdService.getPendingCommands(cid, 0L).iterator();
				Assert.assertTrue(!pc.hasNext());
			}
		}

		// --------------------ACKNOWLEDGE CALL FWD---

		// 'to' sends call fwd ack
		utils.login(to);
		command = buildCFAcked(cfwd);
		cmdService.executeCommand(command, null, false);
		// Invoker (to) receives command
		pc = cmdService.getPendingCommands(toChan.getUUID(), lastSequenceTo)
				.iterator();
		command = pc.next();
		assertCallFwdAcked(command);
		lastSequenceTo = command.getSequenceNumber();
		Assert.assertTrue(!pc.hasNext());

		// 'op' receives command
		utils.login(operator);
		pc = cmdService.getPendingCommands(opChan.getUUID(), lastSequenceOp)
				.iterator();
		command = pc.next();
		assertCallFwdAcked(command);
		lastSequenceOp = command.getSequenceNumber();
		Assert.assertTrue(!pc.hasNext());

		// 'from' receives command
		utils.login(from);
		pc = cmdService
				.getPendingCommands(fromChan.getUUID(), lastSequenceFrom)
				.iterator();
		command = pc.next();
		assertCallFwdAcked(command);
		lastSequenceFrom = command.getSequenceNumber();
		Assert.assertTrue(!pc.hasNext());

		// --------------------ESTABLISH CALL---

		// Call between 'from' and 'to'
		command = buildCallDial(fromChan, to);
		utils.login(from);
		cmdService.executeCommand(command, null, false);
		pc = cmdService.getPendingCommands(fromChan.getUUID(),
				lastSequenceFrom).iterator();
		command = pc.next();
		assertCallDial(command, from, to);
		lastSequenceFrom = command.getSequenceNumber();

		// 'to' responds with terminate
		utils.login(to);
		pc = cmdService.getPendingCommands(toChan.getUUID(),
				lastSequenceTo).iterator();
		command = pc.next();
		assertCallDial(command, from, to);
		lastSequenceTo = command.getSequenceNumber();
		command = buildCallTerminate(toChan, command);
		cmdService.executeCommand(command, null, false);
		pc = cmdService.getPendingCommands(toChan.getUUID(),
				lastSequenceTo).iterator();
		Command callTerminate = pc.next();
		assertCallTerminate(callTerminate);
		lastSequenceTo = callTerminate.getSequenceNumber();

		// 'from' receives terminate
		utils.login(from);
		pc = cmdService.getPendingCommands(fromChan.getUUID(), lastSequenceFrom).iterator();
		command = pc.next();
		assertCallTerminate(command);
		lastSequenceFrom = command.getSequenceNumber();

		// --------------------MARK CALL FWD AS TERMINATED & FAILED---

		// 'from' sends call forward terminated
		Boolean fwdStatus = true;
		utils.login(from);
		command = buildCFTerminated(cfwd, fwdStatus);
		cmdService.executeCommand(command, null, false);
		pc = cmdService.getPendingCommands(fromChan.getUUID(), lastSequenceFrom).iterator();
		command = pc.next();
		assertCallFwdTerminated(command, fwdStatus);
		lastSequenceFrom = command.getSequenceNumber();
		Assert.assertTrue(!pc.hasNext());

		// 'to' receives command
		utils.login(to);
		pc = cmdService.getPendingCommands(toChan.getUUID(), lastSequenceTo).iterator();
		command = pc.next();
		assertCallFwdTerminated(command, fwdStatus);
		lastSequenceTo = command.getSequenceNumber();
		Assert.assertTrue(!pc.hasNext());

		// 'op' receives command
		utils.login(operator);
		pc = cmdService.getPendingCommands(opChan.getUUID(), lastSequenceOp).iterator();
		command = pc.next();
		assertCallFwdTerminated(command, fwdStatus);
		lastSequenceOp = command.getSequenceNumber();
		Assert.assertTrue(!pc.hasNext());
	}

	private void assertCallFwdAcked(Command command) {
		CallFwdRecv callParams = jsonMapper.convertValue(command.getParams(),
				CallFwdRecv.class);
		Assert.assertTrue(command.getMethod().equals(
				Command.METHOD_CALL_FWD_ACK));
		Assert.assertTrue(callParams.getDispatcher().equals(operator.getUUID()));
		Assert.assertTrue(callParams.getTo().equals(to.getUUID()));
		Assert.assertTrue(callParams.getFrom().equals(from.getUUID()));
	}

	private void assertCallFwdEstablished(Command command) {
		CallFwdRecv callParams = jsonMapper.convertValue(command.getParams(),
				CallFwdRecv.class);
		Assert.assertTrue(command.getMethod().equals(Command.METHOD_CALL_FWD_ESTABLISHED));
		Assert.assertTrue(callParams.getDispatcher().equals(operator.getUUID()));
		Assert.assertTrue(callParams.getTo().equals(to.getUUID()));
		Assert.assertTrue(callParams.getFrom().equals(from.getUUID()));
	}

	private void assertCallFwdSetup(Command command) {
		CallFwdRecv callParams = jsonMapper.convertValue(command.getParams(),
				CallFwdRecv.class);
		Assert.assertTrue(command.getMethod().equals(Command.METHOD_CALL_FWD_SETUP));
		Assert.assertTrue(callParams.getDispatcher().equals(operator.getUUID()));
		Assert.assertTrue(callParams.getTo().equals(to.getUUID()));
		Assert.assertTrue(callParams.getFrom().equals(from.getUUID()));
	}

	private void assertCallFwdTerminated(Command command, Boolean expectedFailed) {
		CallFwdRecv callParams = jsonMapper.convertValue(command.getParams(),
				CallFwdRecv.class);
		Assert.assertTrue(command.getMethod().equals(Command.METHOD_CALL_FWD_TERMINATED));
		Assert.assertTrue(callParams.getDispatcher().equals(operator.getUUID()));
		Assert.assertTrue(callParams.getTo().equals(to.getUUID()));
		Assert.assertTrue(callParams.getFrom().equals(from.getUUID()));
		if (expectedFailed == null)
			Assert.assertNull(callParams.getFailed());
		else
			Assert.assertEquals(expectedFailed, callParams.getFailed());
	}

	private void assertCallDial(Command command,  UserEntity caller, UserEntity callee) {
		CallReceive callParams = jsonMapper.convertValue(command.getParams(),
				CallReceive.class);
		Assert.assertTrue(command.getMethod().equals(Command.METHOD_CALL_DIAL));
		Assert.assertTrue(callParams.getFrom().getId().equals(caller.getUUID()));
		Assert.assertTrue(callParams.getTo().equals(callee.getUUID()));
	}

	private void assertCallAccept(Command command) {
		Assert.assertTrue(command.getMethod()
				.equals(Command.METHOD_CALL_ACCEPT));
	}

	private void assertCallTerminate(Command command) {
		Assert.assertTrue(command.getMethod().equals(
				Command.METHOD_CALL_TERMINATE));
	}

	private Command buildCFSetup() {
		Command command = new Command();
		command.setChannelId(opChan.getUUID());
		command.setMethod(Command.METHOD_CALL_FWD_SETUP);

		CallFwdSend param = new CallFwdSend();
		param.setFrom(from.getUUID());
		param.setTo(to.getUUID());
		param.setLocalId(rnd.nextLong());

		command.setParams(param);
		return command;
	}

	private Command buildCFAcked(Command cfwd) {
		CallFwdRecv callFwdParam = jsonMapper.convertValue(cfwd.getParams(),
				CallFwdRecv.class);

		Command command = new Command();
		command.setChannelId(toChan.getUUID());
		command.setMethod(Command.METHOD_CALL_FWD_ACK);

		CallFwdSend param = new CallFwdSend();
		param.setId(callFwdParam.getId());
		param.setFrom(callFwdParam.getFrom());
		param.setTo(callFwdParam.getTo());
		param.setLocalId(rnd.nextLong());

		command.setParams(param);
		return command;
	}

	private Command buildCFEstablished(Command cfwd) {
		CallFwdRecv callFwdParam = jsonMapper.convertValue(
				cfwd.getParams(), CallFwdRecv.class);

		Command command = new Command();
		command.setChannelId(toChan.getUUID());
		command.setMethod(Command.METHOD_CALL_FWD_ESTABLISHED);

		CallFwdSend param = new CallFwdSend();
		param.setId(callFwdParam.getId());
		param.setFrom(callFwdParam.getFrom());
		param.setTo(callFwdParam.getTo());
		param.setLocalId(rnd.nextLong());

		command.setParams(param);
		return command;
	}

	private Command buildCFTerminated(Command cfwd, Boolean failed) {
		CallFwdRecv callFwdParam = jsonMapper.convertValue(
				cfwd.getParams(), CallFwdRecv.class);

		Command command = new Command();
		command.setChannelId(fromChan.getUUID());
		command.setMethod(Command.METHOD_CALL_FWD_TERMINATED);

		CallFwdSend param = new CallFwdSend();
		param.setId(callFwdParam.getId());
		param.setFrom(callFwdParam.getFrom());
		param.setTo(callFwdParam.getTo());
		param.setLocalId(rnd.nextLong());
		param.setFailed(failed);

		command.setParams(param);
		return command;
	}

	private Command buildCallDial(ChannelEntity invoker, UserEntity callee) {
		Command command = new Command();
		command.setChannelId(invoker.getUUID());
		command.setMethod(Command.METHOD_CALL_DIAL);

		CallSend callParam = new CallSend();
		callParam.setTo(callee.getUUID());
		callParam.setLocalId(rnd.nextLong());
		callParam.setSdp("SDP");
		callParam.setCallFwd(true); //avoids sending UpdateContact

		command.setParams(callParam);
		return command;
	}

	private Command buildCallAccept(ChannelEntity invoker, Command callDial) {
		CallReceive callDialParam = jsonMapper.convertValue(
				callDial.getParams(), CallReceive.class);
		Command command = new Command();
		command.setChannelId(invoker.getUUID());
		command.setMethod(Command.METHOD_CALL_ACCEPT);

		CallSend callParam = new CallSend();
		callParam.setId(callDialParam.getId());
		callParam.setTo(callDialParam.getTo());
		callParam.setLocalId(rnd.nextLong());
		callParam.setSdp("SDP");

		command.setParams(callParam);
		return command;
	}

	private Command buildCallTerminate(ChannelEntity invoker, Command callDialAccept) {
		CallReceive callDialParam = jsonMapper.convertValue(
				callDialAccept.getParams(), CallReceive.class);
		Command command = new Command();
		command.setChannelId(invoker.getUUID());
		command.setMethod(Command.METHOD_CALL_TERMINATE);

		CallSend callParam = new CallSend();
		callParam.setId(callDialParam.getId());
		callParam.setTo(callDialParam.getTo());
		callParam.setLocalId(rnd.nextLong());
		callParam.setSdp("SDP");

		command.setParams(callParam);
		return command;
	}

	private void testSetup() throws IOException {
		lastSequenceOp = 0L;
		lastSequenceFrom = 0L;
		lastSequenceTo = 0L;

		String name = "" + rnd.nextInt();
		acc = utils.createAccount();
		from = utils.createUser(name + "-from", acc, false);
		to = utils.createUser(name + "-to", acc, false);
		operator = utils.createUser(name + "-dispatch", acc, false);

		fromChannels = new ArrayList<ChannelEntity>();
		for (int i = 0; i < 5; i++) {
			fromChannels.add(utils.createChannel(from));
		}
		fromChan = fromChannels.get(0);

		toChannels = new ArrayList<ChannelEntity>();
		for (int i = 0; i < 5; i++) {
			toChannels.add(utils.createChannel(to));
		}
		toChan = toChannels.get(0);

		opChannels = new ArrayList<ChannelEntity>();
		for (int i = 0; i < 5; i++) {
			opChannels.add(utils.createChannel(operator));
		}
		opChan = opChannels.get(0);

		GroupEntity group = utils.createGroup(name, acc);
		utils.addGroupMember(group, from);
		utils.addGroupMember(group, to);
		utils.addGroupMember(group, operator);
	}
}
