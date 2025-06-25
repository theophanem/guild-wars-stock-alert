package org.mart.theo.app;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServiceHandler extends Thread {
	private final List<Thread> threads;

	public ServiceHandler() {
		threads = new ArrayList<>();

		threads.add(AlertService.getInstance());

		Logger logger = LogManager.getLogger(getClass());
		for (Thread t : threads) {
			if (t != null) {
				t.start();
				logger.info(t.getName() + " started");
			}
		}
	}

	@Override
	public void run() {
		while (isInterrupted() == false) {
			try {
				sleep(10);
			} catch (InterruptedException t) {
				interrupt();
			}
		}

		Logger logger = LogManager.getLogger(getClass());
		for (Thread t : threads) {
			if (t != null && !t.isInterrupted()) {
				try {
					t.join();
					t.interrupt();
					logger.info(t.getName() + " stopped");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
