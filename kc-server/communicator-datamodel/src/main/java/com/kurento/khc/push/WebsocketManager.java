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

package com.kurento.khc.push;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.kurento.agenda.services.pojo.ChannelCreateResponse;
import com.kurento.khc.datamodel.ChannelEntity;
import com.kurento.khc.jackson.KhcObjectMapper;
import com.kurento.khc.jsonrpc.JsonRpcError;
import com.kurento.khc.jsonrpc.JsonRpcError.Code;
import com.kurento.khc.jsonrpc.JsonRpcRequest;
import com.kurento.khc.jsonrpc.JsonRpcRequest.Method;
import com.kurento.khc.jsonrpc.JsonRpcResponse;
import com.kurento.khc.jsonrpc.JsonRpcResult;
import com.kurento.khc.push.PushResult.Cause;

@Component("khcWebSocketManager")
public class WebsocketManager extends TextWebSocketHandler implements
		NotificationManager {

	private static Logger log = LoggerFactory.getLogger(WebsocketManager.class);

	// @Value("${kurento.ws.connection-ttl-miliseconds:#{null}}")
	private Long CONNECTION_TTL_MILISECONDS = 2000L;

	private ObjectMapper jsonMapper = new KhcObjectMapper();

	private ConcurrentMap<Long, WebSocketSession> sessions = new ConcurrentHashMap<Long, WebSocketSession>();
	private ConcurrentMap<Long, Transaction> transactions = new ConcurrentHashMap<Long, Transaction>();
	private ConcurrentLinkedQueue<Transaction> transactionTimer = new ConcurrentLinkedQueue<Transaction>();
	private AtomicLong sequence = new AtomicLong(0);

	@Autowired
	private NotificationServer notificationServer;

	// //////////////
	// WS API
	// //////////////
	@Override
	public void afterConnectionClosed(WebSocketSession session,
			CloseStatus status) throws Exception {

		Iterator<WebSocketSession> it = sessions.values().iterator();
		while (it.hasNext()) {
			WebSocketSession mapSession = it.next();
			if (session == mapSession) {
				it.remove();
			}
		}

		super.afterConnectionClosed(session, status);
	}

	@Override
	public void handleTextMessage(WebSocketSession session, TextMessage message)
			throws IOException {

		JsonNode payload = jsonMapper.readTree(message.getPayload());
		if (payload.get("method") != null) {
			JsonRpcResponse response;
			try {
				try {
					response = processRequest(session, jsonMapper.convertValue(
							payload, JsonRpcRequest.class));
				} catch (Exception e) {
					log.warn("Malformed WS message", e);
					response = new JsonRpcResponse();
					response.setId(null);
					JsonRpcError error = new JsonRpcError();
					error.setCode(Code.PARSE_ERROR);
					response.setError(error);
					session.sendMessage(new TextMessage(jsonMapper
							.writeValueAsString(response)));
				}
				session.sendMessage(new TextMessage(jsonMapper
						.writeValueAsString(response)));
			} catch (IOException e) {
				log.warn("Unable to send WS response", e);
			}
		} else {
			try {
				processResponse(jsonMapper.convertValue(payload,
						JsonRpcResponse.class));
			} catch (Exception e) {
				log.warn("Unable to process WS response", e);
			}
		}
	}

	private JsonRpcResponse processRequest(WebSocketSession session,
			JsonRpcRequest request) throws IOException {
		JsonRpcResponse response = new JsonRpcResponse();
		response.setId(request.getId());

		switch (request.getMethod()) {
		case REGISTER:
			ChannelCreateResponse channel;
			try {
				channel = jsonMapper.convertValue(request.getParams(),
						ChannelCreateResponse.class);
			} catch (Exception e) {
				response.setId(request.getId());
				JsonRpcError error = new JsonRpcError();
				error.setCode(Code.MISSING_CHANNEL_ID);
				error.setMessage("Missing channel ID");
				response.setError(error);
				return response;
			}

			sessions.put(channel.getChannelId(), session);
			log.debug("WS register on channel {}", channel.getChannelId());
			break;
		case PING:
			break;
		default:
			response.setId(request.getId());
			JsonRpcError error = new JsonRpcError();
			error.setCode(Code.METHOD_NOT_FOUND);
			error.setMessage("Method not found");
			response.setError(error);
			return response;
		}
		response.setId(request.getId());
		JsonRpcResult result = new JsonRpcResult();
		result.setCode(JsonRpcResult.Code.OK);
		response.setResult(result);
		return response;
	}

	private void processResponse(JsonRpcResponse response) {
		JsonRpcResult result;
		if ((result = response.getResult()) != null) {
			switch (result.getCode()) {
			case OK:
				// Remove transaction that compeltes OK. OTherwise they will
				// timeout
				Transaction transaction = transactions.remove(response.getId());
				if (transaction != null) {
					Notification notification = transaction.getNotification();
					log.trace(
							"WS transaction {} confirms notification for channel {} - {}",
							transaction.getSequence(), notification
									.getChannel().getRegisterType(),
							notification.getChannel().getUUID());
				} else {
					log.trace(
							"WS notification confirmed for unknown transaction with ID: {}",
							response.getId());
				}
			}
		}
	}

	// ////////////////////
	// NOTIFICATION API
	// ///////////////////
	@Override
	public Boolean isEnabled(ChannelEntity channel) {
		return sessions.containsKey(channel.getUUID());
	}

	@Override
	public void sendNotification(Notification notification) {
		Long channelId = notification.getChannel().getUUID();
		WebSocketSession session = this.sessions.get(channelId);

		if (session == null) {
			// request inmediate try of PUSH channel
			notificationServer.processResult(new PushResult(notification,
					Cause.RETRY));
		} else {
			Long nextSequence = sequence.getAndIncrement();
			// Record transaction
			Transaction transaction = new Transaction(notification,
					nextSequence);
			transactions.put(nextSequence, transaction);
			// Start transaction timer (adding to the timer queue)
			transactionTimer.add(transaction);

			JsonRpcRequest request = new JsonRpcRequest();
			request.setId(nextSequence);
			request.setMethod(Method.SYNC);

			try {
				log.trace(
						"WS transaction {} sending notification to channel {} }",
						nextSequence, channelId);
				session.sendMessage(new TextMessage(jsonMapper
						.writeValueAsString(request)));
			} catch (IOException e) {
				// request inmediate try of PUSH channel
				notificationServer.processResult(new PushResult(notification,
						Cause.RETRY));
			}
		}
	}

	// //////////////////
	// TIMER MANAGER
	// //////////////////

	@Scheduled(fixedDelay = 100)
	public void wsNotificationTimerService() {
		// log.trace("Start WS notification timer service");
		// Get timeout transactions
		Transaction transaction;
		Long transactionTimeout = System.currentTimeMillis()
				- CONNECTION_TTL_MILISECONDS;
		while ((transaction = transactionTimer.peek()) != null
				&& transaction.getTimestamp() < transactionTimeout) {
			transaction = transactionTimer.poll();
			Long recvSequence = transaction.getSequence();
			Notification notification = transaction.getNotification();
			if (transactions.containsKey(recvSequence)) {
				// Transaction timed-out request notification inmediate retry
				log.trace(
						"WS transaction {} timed out notification for channel {}",
						transaction.getSequence(), notification.getChannel()
								.getUUID());
				notificationServer.processResult(new PushResult(notification,
						Cause.RETRY));
			} else {
				log.trace(
						"WS transaction {} successfully delivered notification for channel {}",
						transaction.getSequence(), notification.getChannel()
								.getUUID());
			}
		}
	}

	private class Transaction {

		private Notification notification;
		private Long timestamp;
		private Long sequence;

		private Transaction(Notification notification, Long sequence) {
			this.notification = notification;
			this.timestamp = System.currentTimeMillis();
			this.sequence = sequence;
		}

		private Notification getNotification() {
			return this.notification;
		}

		private Long getTimestamp() {
			return this.timestamp;
		}

		private Long getSequence() {
			return this.sequence;
		}
	}
}