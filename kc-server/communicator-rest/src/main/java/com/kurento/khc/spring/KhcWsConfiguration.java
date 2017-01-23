package com.kurento.khc.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.kurento.khc.push.WebsocketManager;

@Configuration
@EnableWebSocket
public class KhcWsConfiguration implements WebSocketConfigurer {

	private static final Logger log = LoggerFactory
			.getLogger(KhcWsConfiguration.class);

	@Autowired
	private WebsocketManager wsPush;

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		log.debug("Register WS manager");
		registry.addHandler(wsPush, "/sync");
	}

}