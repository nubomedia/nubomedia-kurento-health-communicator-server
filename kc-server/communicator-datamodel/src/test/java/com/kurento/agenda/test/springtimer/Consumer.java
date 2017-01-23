package com.kurento.agenda.test.springtimer;

import java.util.Timer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.PostConstruct;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

//@Component
public class Consumer implements Runnable, BeanFactoryAware {

	private BeanFactory beanFactory;

	protected BlockingQueue<String> queue;
	private Timer timer;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@PostConstruct
	public void init() {
		queue = new LinkedBlockingQueue<String>();
		timer = new Timer();

		Thread thread = new Thread(this, "thread");
		thread.start();
	}

	@Override
	public void run() {
		System.out.println("Schedule queue insertion");
		timer.schedule(
				(Backoff) beanFactory.getBean("backoff", "FUNCIONA!!!!"), 10000);

		while (true) {
			try {
				String param = queue.take();
				System.out.println(param);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}