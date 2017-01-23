package com.kurento.agenda.test.springtimer;

import java.util.TimerTask;

import org.springframework.beans.factory.annotation.Autowired;

//@Component
//@Scope("prototype")
public class Backoff extends TimerTask {

	@Autowired
	private Consumer consumer;
	private String param;

	private Backoff(String param) {
		this.param = param;
	}

	@Override
	public void run() {
		System.out.println("Insert into QUEUE");
		consumer.queue.add(param);

	}

}