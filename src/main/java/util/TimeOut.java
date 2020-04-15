package util;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Helper class for setting timeouts.
 * 
 * @author Huub Lievestro, adapted from Jaco ter Braak & Frans van Dijk (University of Twente) 
 * @version 04-2020
 */
public class TimeOut implements Runnable {
	private static Map<Date, Map<ITimeoutEventHandler, List<Object>>> eventHandlers
		= new HashMap<>();
	private static Thread eventTriggerThread;
	private static boolean started = false;
	private static ReentrantLock lock = new ReentrantLock();

	/**
	 * Starts the helper thread.
	 */
	public static void start() {
		if (started) {
			throw new IllegalStateException("Already started");
		}
		started = true;
		eventTriggerThread = new Thread(new TimeOut());
		eventTriggerThread.start();
	}

	/**
	 * Stops the helper thread.
	 */
	public static void stop() {
		if (!started) {
			throw new IllegalStateException(
					"Not started or already stopped");
		}
		eventTriggerThread.interrupt();
		try {
			eventTriggerThread.join();
		} catch (InterruptedException e) {
		}
	}

	/**
	 * Set a timeout.
	 * 
	 * @param millisecondsTimeout
	 *            the timeout interval, starting now
	 * @param handler
	 *            the event handler that is called once the timeout elapses
	 */
	public static void setTimeOut(long millisecondsTimeout,
			ITimeoutEventHandler handler, Object tag) {
		Date elapsedMoment = new Date();
		elapsedMoment
		.setTime(elapsedMoment.getTime() + millisecondsTimeout);

		lock.lock();
		if (!eventHandlers.containsKey(elapsedMoment)) {
			eventHandlers.put(elapsedMoment,
					new HashMap<>());
		}
		if (!eventHandlers.get(elapsedMoment).containsKey(handler)) {
			eventHandlers.get(elapsedMoment).put(handler,
					new ArrayList<>());
		}
		eventHandlers.get(elapsedMoment).get(handler).add(tag);
		lock.unlock();
	}

	/**
	 * Do not call this.
	 */
	@Override
	public void run() {
		boolean runThread = true;
		ArrayList<Date> datesToRemove = new ArrayList<>();
		HashMap<ITimeoutEventHandler, List<Object>> handlersToInvoke = new HashMap<>();
		Date now;

		while (runThread) {
			try {
				now = new Date();

				// If any timeouts have elapsed, trigger their handlers
				lock.lock();

				for (Date date : eventHandlers.keySet()) {
					if (date.before(now)) {
						datesToRemove.add(date);
						for (ITimeoutEventHandler handler : eventHandlers.get(date).keySet()) {
							if (!handlersToInvoke.containsKey(handler)) {
								handlersToInvoke.put(handler,
										new ArrayList<>());
							}
							for (Object tag : eventHandlers.get(date).get(
									handler)) {
								handlersToInvoke.get(handler).add(tag);
							}
						}
					}
				}

				// Remove elapsed events
				for (Date date : datesToRemove) {
					eventHandlers.remove(date);
				}
				datesToRemove.clear();

				lock.unlock();

				// Invoke the event handlers outside of the lock, to prevent
				// deadlocks
				for (ITimeoutEventHandler handler : handlersToInvoke
						.keySet()) {
					handlersToInvoke.get(handler).forEach(handler::timeoutElapsed);
				}
				handlersToInvoke.clear();

				Thread.sleep(1);
			} catch (InterruptedException e) {
				runThread = false;
			}
		}

	}
}

