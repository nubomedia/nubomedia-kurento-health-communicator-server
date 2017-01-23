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
import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.services.pojo.CallReceive;
import com.kurento.agenda.services.pojo.CallSend;
import com.kurento.khc.KhcInvalidDataException;
import com.kurento.khc.datamodel.AccountEntity;
import com.kurento.khc.datamodel.ChannelEntity;
import com.kurento.khc.datamodel.GroupEntity;
import com.kurento.khc.datamodel.UserEntity;
import com.kurento.khc.jackson.KhcObjectMapper;
import com.kurento.khc.push.DevNullManager;
import com.kurento.khc.push.DevNullManager.DevNullListener;
import com.kurento.khc.push.Notification;
import com.kurento.khc.services.v2.CommandService;
import com.kurento.khc.spring.KhcDatamodelConfig;
import com.kurento.khc.test.utils.KhcTestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = KhcDatamodelConfig.class)
@ActiveProfiles({ "embed_db", "embed_security" })
public class CallTest {

	private final Logger log = LoggerFactory.getLogger(CallTest.class);

	private static final Random rnd = new SecureRandom();

	@Autowired
	private KhcTestUtils utils;
	@Autowired
	private CommandService cmdService;
	@Autowired
	private DevNullManager pushManager;

	private AccountEntity acc;
	private UserEntity from, to, other;
	private ChannelEntity offerer, offered, otherChan;
	private List<ChannelEntity> fromChannel, toChannel;
	private Boolean callAccepted, callCompleted;
	private Boolean failed = false;

	private ObjectMapper jsonMapper = new KhcObjectMapper();

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void testCallAccept() throws IOException, InterruptedException {
		testSetup();

		// Add receiver listeners
		for (ChannelEntity receiver : toChannel) {
			final UserEntity receiverUser = to;
			final ChannelEntity receiverChannel = receiver;
			pushManager.addListener(receiverChannel.getUUID(),
					new DevNullListener() {

						private Long lastSequence = 0L;

						@Override
						public void push(Notification notification) {
							new Thread("" + receiverChannel) {
								@Override
								public void run() {
									// Change listener for next notification
									pushManager.addListener(
											receiverChannel.getUUID(),
											new DevNullListener() {

												@Override
												public void push(
														Notification notification) {
													Iterator<Command> pc = cmdService
															.getPendingCommands(
																	receiverChannel
																			.getUUID(),
																	lastSequence)
															.iterator();
													assertCallCompleted(pc
															.next());
													Assert.assertTrue(!pc
															.hasNext());

												}

											});
									try {
										Thread.sleep(rnd.nextInt(100) + 100);
									} catch (InterruptedException e) {
										log.debug("thread error", e);
									}
									// Check we receive call dial command
									utils.login(receiverUser);
									Iterator<Command> pc = cmdService
											.getPendingCommands(
													receiverChannel.getUUID(),
													lastSequence).iterator();
									Command callDial = pc.next();
									lastSequence = callDial.getSequenceNumber();
									assertCallDial(callDial);

									// Send accept command (all)
									Command callAccept = buildCallAccept(
											receiverChannel, callDial);
									try {
										cmdService.executeCommand(callAccept,
												null, false);
										assertCallAcceptCommandAccepted();
									} catch (Exception e) {
										assertCallAcceptCommandRejected();
									}

								}
							}.start();
						}
					});
		}

		Command command = buildCallDial(offerer);
		utils.login(from);
		cmdService.executeCommand(command, null, false);

		// Invoker receives command
		Iterator<Command> pendingCommands = cmdService.getPendingCommands(
				offerer.getUUID(), 0L).iterator();
		assertCallDial(pendingCommands.next());
		Assert.assertTrue(!pendingCommands.hasNext());

		assertOtherFromChannelHaveNoCommand();

		Thread.sleep(3000);
		Assert.assertFalse(failed);

	}

	@Test
	public void testCallReject() throws IOException {
		testSetup();

		Command command = buildCallDial(offerer);
		utils.login(from);
		cmdService.executeCommand(command, null, false);
		Iterator<Command> pc = cmdService.getPendingCommands(offerer.getUUID(),
				0L).iterator();
		assertCallDial(pc.next());

		utils.login(to);
		for (ChannelEntity channel : toChannel) {
			pc = cmdService.getPendingCommands(channel.getUUID(), 0L)
					.iterator();
			assertCallDial(pc.next());
			Assert.assertTrue(!pc.hasNext());
		}
		pc = cmdService.getPendingCommands(offered.getUUID(), 0L).iterator();
		command = buildCallTerminate(offered, pc.next());
		cmdService.executeCommand(command, null, false);

		// invoker receives termination cmd-1:dial, cmd-2:terminate
		utils.login(from);
		pc = cmdService.getPendingCommands(offerer.getUUID(), 1L).iterator();
		assertCallTerminate(pc.next());
		Assert.assertTrue(!pc.hasNext());

		// other invoker channels does not receive commands
		assertOtherFromChannelHaveNoCommand();

		// All to channels get termination cmd-1:dial, cmd-2:terminate
		utils.login(to);
		for (ChannelEntity channel : toChannel) {
			pc = cmdService.getPendingCommands(channel.getUUID(), 1L)
					.iterator();
			assertCallTerminate(pc.next());
			Assert.assertTrue(!pc.hasNext());
		}
	}

	@Test
	public void testCallTerminateByCalled() throws IOException {
		testSetup();

		// Caller dial call
		Command command = buildCallDial(offerer);
		utils.login(from);
		cmdService.executeCommand(command, null, false);

		utils.login(to);
		// All Callee channels receives dial
		for (ChannelEntity channel : toChannel) {
			if (channel.equals(offered)) {
				continue;
			}
			Iterator<Command> pc = cmdService.getPendingCommands(
					channel.getUUID(), 0L).iterator();
			assertCallDial(pc.next());
			Assert.assertTrue(!pc.hasNext());
		}

		// Callee accept call
		utils.login(to);
		Iterator<Command> pc = cmdService.getPendingCommands(offered.getUUID(),
				0L).iterator();
		Command callDial = pc.next();
		command = buildCallAccept(offered, callDial);
		cmdService.executeCommand(command, null, false);

		// All other Callee channels receives terminate
		for (ChannelEntity channel : toChannel) {
			if (channel.equals(offered)) {
				continue;
			}
			pc = cmdService.getPendingCommands(channel.getUUID(), 1L)
					.iterator();
			assertCallTerminate(pc.next());
			Assert.assertTrue(!pc.hasNext());
		}

		// Caller receives accept:
		// - cmd-1:dial,
		// - cmd-2:accept,
		utils.login(from);
		pc = cmdService.getPendingCommands(offerer.getUUID(), 1L).iterator();
		assertCallAccept(pc.next());

		// Callee terminates call
		utils.login(to);
		command = buildCallTerminate(offered, callDial);
		cmdService.executeCommand(command, null, false);

		// Caller receives terminate
		utils.login(from);
		pc = cmdService.getPendingCommands(offerer.getUUID(), 2L).iterator();
		assertCallTerminate(pc.next());
		Assert.assertTrue(!pc.hasNext());

		// other invoker channels does not receive commands
		assertOtherFromChannelHaveNoCommand();

		utils.login(to);
		// Receiver channel gets termination cmd-1:dial, cmd-2:accept,
		// cmd-3:terminate
		pc = cmdService.getPendingCommands(offered.getUUID(), 2L).iterator();
		assertCallTerminate(pc.next());
		Assert.assertTrue(!pc.hasNext());
	}

	@Test
	public void testCallTerminateByCaller() throws IOException {
		testSetup();

		// Caller dial call
		Command command = buildCallDial(offerer);
		utils.login(from);
		cmdService.executeCommand(command, null, false);

		utils.login(to);
		// All Callee channels receives dial
		for (ChannelEntity channel : toChannel) {
			if (channel.equals(offered)) {
				continue;
			}
			Iterator<Command> pc = cmdService.getPendingCommands(
					channel.getUUID(), 0L).iterator();
			assertCallDial(pc.next());
			Assert.assertTrue(!pc.hasNext());
		}

		// Callee accept call
		utils.login(to);
		Iterator<Command> pc = cmdService.getPendingCommands(offered.getUUID(),
				0L).iterator();
		Command callDial = pc.next();
		command = buildCallAccept(offered, callDial);
		cmdService.executeCommand(command, null, false);

		// All other Callee channels receives terminate
		for (ChannelEntity channel : toChannel) {
			if (channel.equals(offered)) {
				continue;
			}
			pc = cmdService.getPendingCommands(channel.getUUID(), 1L)
					.iterator();
			assertCallTerminate(pc.next());
			Assert.assertTrue(!pc.hasNext());
		}

		// Caller receives accept:
		// - cmd-1:dial,
		// - cmd-2:accept,
		utils.login(from);
		pc = cmdService.getPendingCommands(offerer.getUUID(), 1L).iterator();
		assertCallAccept(pc.next());

		// Caller terminates call
		utils.login(from);
		command = buildCallTerminate(offerer, callDial);
		cmdService.executeCommand(command, null, false);

		// invoker receives termination cmd-1:dial, cmd-2:accept,
		// cmd-3:terminate
		utils.login(from);
		pc = cmdService.getPendingCommands(offerer.getUUID(), 2L).iterator();
		assertCallTerminate(pc.next());
		Assert.assertTrue(!pc.hasNext());

		// other invoker channels does not receive commands
		assertOtherFromChannelHaveNoCommand();

		utils.login(to);
		// Receiver channel gets termination cmd-1:dial, cmd-2:accept,
		// cmd-3:terminate
		pc = cmdService.getPendingCommands(offered.getUUID(), 2L).iterator();
		assertCallTerminate(pc.next());
		Assert.assertTrue(!pc.hasNext());

		// Other to channels not receiver get termination cmd-1:dial,
		// cmd-2:terminate
		for (ChannelEntity channel : toChannel) {
			if (channel.equals(offered)) {
				continue;
			}
			pc = cmdService.getPendingCommands(channel.getUUID(), 1L)
					.iterator();
			assertCallTerminate(pc.next());
			Assert.assertTrue(!pc.hasNext());
		}
	}

	@Test
	public void testCallCancel() throws IOException {
		testSetup();

		Command command = buildCallDial(offerer);
		utils.login(from);
		cmdService.executeCommand(command, null, false);

		// invoker receives dial
		Iterator<Command> pc = cmdService.getPendingCommands(offerer.getUUID(),
				0L).iterator();
		assertCallDial(pc.next());
		Assert.assertTrue(!pc.hasNext());

		// Receiver receives dial
		utils.login(to);
		for (ChannelEntity channel : toChannel) {
			pc = cmdService.getPendingCommands(channel.getUUID(), 0L)
					.iterator();
			assertCallDial(pc.next());
			Assert.assertTrue(!pc.hasNext());
		}

		utils.login(from);
		pc = cmdService.getPendingCommands(offerer.getUUID(),
				0L).iterator();
		command = buildCallTerminate(offerer, pc.next());
		cmdService.executeCommand(command, null, false);

		// invoker receives terminate
		pc = cmdService.getPendingCommands(offerer.getUUID(), 1L).iterator();
		assertCallTerminate(pc.next());
		Assert.assertTrue(!pc.hasNext());

		// other invoker channels does not receive commands
		assertOtherFromChannelHaveNoCommand();

		// All to channels get termination cmd-1:dial, cmd-2:terminate
		utils.login(to);
		for (ChannelEntity channel : toChannel) {
			pc = cmdService.getPendingCommands(channel.getUUID(), 1L)
					.iterator();
			assertCallTerminate(pc.next());
			Assert.assertTrue(!pc.hasNext());
		}
	}

	/**
	 * Tests mute cmds sent during an estd. call from both endpoints.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testCallMute() throws IOException, InterruptedException {
		testSetup();

		Long lastSequenceFrom = 0L;
		Long lastSequenceTo = 0L;

		// Caller dial call
		Command command = buildCallDial(offerer);
		utils.login(from);
		cmdService.executeCommand(command, null, false);

		utils.login(to);
		// All Callee channels receive dial
		for (ChannelEntity channel : toChannel) {
			if (channel.equals(offered)) {
				continue;
			}
			Iterator<Command> pc = cmdService.getPendingCommands(
					channel.getUUID(), 0L).iterator();
			assertCallDial(pc.next());
			Assert.assertTrue(!pc.hasNext());
		}

		// Callee accept call
		utils.login(to);
		Iterator<Command> pc = cmdService.getPendingCommands(offered.getUUID(),
				0L).iterator();
		Command callDial = pc.next();
		assertCallDial(callDial);
		lastSequenceTo = callDial.getSequenceNumber();
		command = buildCallAccept(offered, callDial);
		cmdService.executeCommand(command, null, false);

		// All other Callee channels receives terminate
		for (ChannelEntity channel : toChannel) {
			if (channel.equals(offered)) {
				continue;
			}
			pc = cmdService.getPendingCommands(channel.getUUID(), 1L)
					.iterator();
			assertCallTerminate(pc.next());
			Assert.assertTrue(!pc.hasNext());
		}

		// Callee receives accept
		utils.login(to);
		pc = cmdService.getPendingCommands(offered.getUUID(),
				lastSequenceTo).iterator();
		command = pc.next();
		assertCallAccept(command);
		lastSequenceTo = command.getSequenceNumber();
		Assert.assertTrue(!pc.hasNext());

		// Caller receives accept, 2 cmds: dial, accept
		utils.login(from);
		pc = cmdService.getPendingCommands(offerer.getUUID(),
				lastSequenceFrom).iterator();
		assertCallDial(pc.next());
		command = pc.next();
		lastSequenceFrom = command.getSequenceNumber();
		assertCallAccept(command);
		Assert.assertTrue(!pc.hasNext());

		// Caller sends mute
		command = buildCallMute(offerer, callDial, to, true, null);
		utils.login(from);
		cmdService.executeCommand(command, null, false);
		// Invoker doesn't receive any commands
		pc = cmdService.getPendingCommands(
				offerer.getUUID(), lastSequenceFrom).iterator();
		Assert.assertTrue(!pc.hasNext());

		// Callee receives mute
		utils.login(to);
		pc = cmdService.getPendingCommands(offered.getUUID(),
				lastSequenceTo).iterator();
		command = pc.next();
		lastSequenceTo = command.getSequenceNumber();
		assertCallMute(command, from, to, true, null);
		Assert.assertTrue(!pc.hasNext());

		// Callee sends mute
		command = buildCallMute(offered, command, from, true, true);
		utils.login(to);
		cmdService.executeCommand(command, null, false);
		// Invoker doesn't receive any commands
		pc = cmdService.getPendingCommands(
				offered.getUUID(), lastSequenceTo).iterator();
		Assert.assertTrue(!pc.hasNext());

		// Caller receives mute
		utils.login(from);
		pc = cmdService.getPendingCommands(offerer.getUUID(),
				lastSequenceFrom).iterator();
		command = pc.next();
		lastSequenceFrom = command.getSequenceNumber();
		assertCallMute(command, to, from, true, true);
		Assert.assertTrue(!pc.hasNext());

		// Callee terminates call
		utils.login(to);
		command = buildCallTerminate(offered, callDial);
		cmdService.executeCommand(command, null, false);

		// Caller receives terminate
		utils.login(from);
		pc = cmdService.getPendingCommands(offerer.getUUID(),
				lastSequenceFrom).iterator();
		command = pc.next();
		lastSequenceFrom = command.getSequenceNumber();
		assertCallTerminate(command);
		Assert.assertTrue(!pc.hasNext());

		// other invoker channels does not receive commands
		assertOtherFromChannelHaveNoCommand();

		// Receiver channel gets termination
		utils.login(to);
		pc = cmdService.getPendingCommands(offered.getUUID(),
				lastSequenceTo).iterator();
		assertCallTerminate(pc.next());
		Assert.assertTrue(!pc.hasNext());
	}

	/**
	 * Tests rejected mute cmds.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testCallMuteRejected() throws IOException, InterruptedException {
		testSetup();

		Long lastSequenceFrom = 0L;
		Long lastSequenceTo = 0L;

		// Caller dial call
		Command command = buildCallDial(offerer);
		utils.login(from);
		cmdService.executeCommand(command, null, false);

		// Callee receives call, will not accept it yet
		utils.login(to);
		Iterator<Command> pc = cmdService.getPendingCommands(offered.getUUID(),
				0L).iterator();
		Command callDial = pc.next();
		assertCallDial(callDial);
		lastSequenceTo = callDial.getSequenceNumber();

		// Other sends mute, gets access denied
		command = buildCallMute(otherChan, callDial, from, true, true);
		utils.login(other);
		exception.expect(AccessDeniedException.class);
		cmdService.executeCommand(command, null, false);
		// Invoker receives command
		pc = cmdService.getPendingCommands(
				otherChan.getUUID(), 0L).iterator();
		Assert.assertTrue(!pc.hasNext());

		// Caller receives dial
		utils.login(from);
		pc = cmdService.getPendingCommands(offerer.getUUID(),
				lastSequenceFrom).iterator();
		command = pc.next();
		assertCallDial(command);
		lastSequenceFrom = command.getSequenceNumber();
		Assert.assertTrue(!pc.hasNext());

		// Caller sends mute, gets rejected
		command = buildCallMute(offerer, callDial, to, true, null);
		utils.login(from);
		exception.expect(KhcInvalidDataException.class);
		exception.expectMessage(StringContains.containsString(
				"Unable to accept call mute"));
		cmdService.executeCommand(command, null, false);
		// Invoker receives command
		pc = cmdService.getPendingCommands(
				offerer.getUUID(), lastSequenceFrom).iterator();
		Assert.assertTrue(!pc.hasNext());

		// Callee accepts call
		utils.login(to);
		command = buildCallAccept(offered, callDial);
		cmdService.executeCommand(command, null, false);

		// Callee receives accept
		pc = cmdService.getPendingCommands(offered.getUUID(),
				lastSequenceTo).iterator();
		command = pc.next();
		assertCallAccept(command);
		lastSequenceTo = command.getSequenceNumber();
		Assert.assertTrue(!pc.hasNext());

		// Caller receives accept
		utils.login(from);
		pc = cmdService.getPendingCommands(offerer.getUUID(),
				lastSequenceFrom).iterator();
		command = pc.next();
		lastSequenceFrom = command.getSequenceNumber();
		assertCallAccept(command);
		Assert.assertTrue(!pc.hasNext());

		// Callee terminates call
		utils.login(to);
		command = buildCallTerminate(offered, callDial);
		cmdService.executeCommand(command, null, false);

		// Caller receives terminate
		utils.login(from);
		pc = cmdService.getPendingCommands(offerer.getUUID(),
				lastSequenceFrom).iterator();
		command = pc.next();
		lastSequenceFrom = command.getSequenceNumber();
		assertCallTerminate(command);
		Assert.assertTrue(!pc.hasNext());

		// Receiver channel gets termination
		utils.login(to);
		pc = cmdService.getPendingCommands(offered.getUUID(),
				lastSequenceTo).iterator();
		command = pc.next();
		lastSequenceTo = command.getSequenceNumber();
		assertCallTerminate(command);
		Assert.assertTrue(!pc.hasNext());

		// Callee sends mute, gets rejected
		command = buildCallMute(offered, command, from, true, true);
		utils.login(to);
		exception.expect(KhcInvalidDataException.class);
		exception.expectMessage(StringContains.containsString(
				"Unable to accept call mute"));
		cmdService.executeCommand(command, null, false);
		// Invoker receives command
		pc = cmdService.getPendingCommands(
				offered.getUUID(), lastSequenceTo).iterator();
		Assert.assertTrue(!pc.hasNext());

		// Caller has no pending cmds
		utils.login(from);
		pc = cmdService.getPendingCommands(offerer.getUUID(),
				lastSequenceFrom).iterator();
		Assert.assertTrue(!pc.hasNext());
	}

	private Command buildCallDial(ChannelEntity invoker) {
		Command command = new Command();
		command.setChannelId(invoker.getUUID());
		command.setMethod(Command.METHOD_CALL_DIAL);

		CallSend callParam = new CallSend();
		callParam.setTo(to.getUUID());
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
		callParam.setTo(to.getUUID());
		callParam.setLocalId(rnd.nextLong());
		callParam.setSdp("SDP");

		command.setParams(callParam);
		return command;
	}

	private Command buildCallTerminate(ChannelEntity invoker, Command callDial) {
		CallReceive callDialParam = jsonMapper.convertValue(
				callDial.getParams(), CallReceive.class);
		Command command = new Command();
		command.setChannelId(invoker.getUUID());
		command.setMethod(Command.METHOD_CALL_TERMINATE);

		CallSend callParam = new CallSend();
		callParam.setId(callDialParam.getId());
		callParam.setTo(to.getUUID());
		callParam.setLocalId(rnd.nextLong());
		callParam.setSdp("SDP");

		command.setParams(callParam);
		return command;
	}

	private Command buildCallMute(ChannelEntity invokerChan, Command callDial, UserEntity muteTo, Boolean videoOff, Boolean soundOff) {
		CallReceive callDialParam = jsonMapper.convertValue(
				callDial.getParams(), CallReceive.class);
		Command command = new Command();
		command.setChannelId(invokerChan.getUUID());
		command.setMethod(Command.METHOD_CALL_MUTE);

		CallSend callParam = new CallSend();
		callParam.setId(callDialParam.getId());
		callParam.setTo(muteTo.getUUID());
		callParam.setLocalId(rnd.nextLong());
		callParam.setSdp("SDP");
		callParam.setVideoOff(videoOff);
		callParam.setSoundOff(soundOff);
		command.setParams(callParam);
		return command;
	}

	private void assertCallDial(Command command) {
		CallReceive callParams = jsonMapper.convertValue(command.getParams(),
				CallReceive.class);
		Assert.assertTrue(command.getMethod().equals(Command.METHOD_CALL_DIAL));
		Assert.assertTrue(callParams.getFrom().getId().equals(from.getUUID()));
		Assert.assertTrue(callParams.getTo().equals(to.getUUID()));

	}

	synchronized private void assertCallAcceptCommandAccepted() {
		Assert.assertFalse(callAccepted);
		if (callAccepted)
			failed = true;
		callAccepted = true;
	}

	synchronized private void assertCallAcceptCommandRejected() {
		Assert.assertTrue(callAccepted);
		if (!callAccepted)
			failed = true;
	}

	synchronized private void assertCallCompleted(Command command) {
		if (!callCompleted) {
			Assert.assertTrue(command.getMethod().equals(
					Command.METHOD_CALL_ACCEPT));
			if (!command.getMethod().equals(Command.METHOD_CALL_ACCEPT))
				failed = true;
		} else {
			Assert.assertTrue(command.getMethod().equals(
					Command.METHOD_CALL_TERMINATE));
			if (!command.getMethod().equals(Command.METHOD_CALL_TERMINATE))
				failed = true;
		}
	}

	private void assertCallTerminate(Command command) {
		Assert.assertTrue(command.getMethod().equals(
				Command.METHOD_CALL_TERMINATE));
	}

	private void assertCallAccept(Command command) {
		Assert.assertTrue(command.getMethod()
				.equals(Command.METHOD_CALL_ACCEPT));
	}

	private void assertOtherFromChannelHaveNoCommand() {
		// Other invoker channels' don't get command
		for (ChannelEntity channel : fromChannel) {
			if (channel.getId().equals(offerer.getId())) {
				continue;
			}
			Iterator<Command> pendingCommands = cmdService.getPendingCommands(
					channel.getUUID(), 0L).iterator();
			Assert.assertTrue(!pendingCommands.hasNext());
		}
	}

	private void assertCallMute(Command command,
			UserEntity expectedMuteFrom, UserEntity expectedMuteTo, Boolean expectedVideoOff, Boolean expectedSoundOff) {
		CallReceive callParams = jsonMapper.convertValue(command.getParams(),
				CallReceive.class);
		Assert.assertTrue("Cmd method not " + Command.METHOD_CALL_MUTE +
				" but " + command.getMethod(), command.getMethod().equals(Command.METHOD_CALL_MUTE));
		Assert.assertTrue(callParams.getFrom().getId().equals(expectedMuteFrom.getUUID()));
		Assert.assertTrue(callParams.getTo().equals(expectedMuteTo.getUUID()));
		Assert.assertEquals(expectedVideoOff, callParams.getVideoOff());
		Assert.assertEquals(expectedSoundOff, callParams.getSoundOff());
	}

	private void testSetup() throws IOException {
		String name = "" + rnd.nextInt();
		acc = utils.createAccount();
		from = utils.createUser(name + "-from", acc, false);
		to = utils.createUser(name + "-to", acc, false);
		other = utils.createUser(name + "-other", acc, false);

		fromChannel = new ArrayList<ChannelEntity>();
		for (int i = 0; i < 5; i++) {
			fromChannel.add(utils.createChannel(from));
		}
		offerer = fromChannel.get(0);

		toChannel = new ArrayList<ChannelEntity>();
		for (int i = 0; i < 5; i++) {
			toChannel.add(utils.createChannel(to));
		}
		offered = toChannel.get(0);

		otherChan = utils.createChannel(other);

		GroupEntity group = utils.createGroup(name, acc);
		utils.addGroupMember(group, from);
		utils.addGroupMember(group, to);
		utils.addGroupMember(group, other);

		callCompleted = false;
		callAccepted = false;
	}
}
