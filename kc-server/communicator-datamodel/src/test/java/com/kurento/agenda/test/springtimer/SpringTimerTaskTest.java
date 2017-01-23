package com.kurento.agenda.test.springtimer;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringTimerTaskTest {

	public static void main(String[] args) throws InterruptedException {
		System.out.println("Start Spring TimerTask test");
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				"classpath:scheduler-test-context.xml");
		System.out.println("Terminate Sprint TimerTast test");
	}

}
