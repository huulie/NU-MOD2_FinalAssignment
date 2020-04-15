package util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TimeOutTest implements util.ITimeoutEventHandler {
	boolean timeOutHandled;
	
	@Test
	void testTimeOut() {

		this.timeOutHandled = false;
		int timeOutTime = 1000; // milliseconds
		util.TimeOut.start();
		Object testObject = new Object();
		util.TimeOut.setTimeOut(timeOutTime, this, testObject); 

		try {
			Thread.sleep(timeOutTime + 500); // TODO maybe not the best, but does the job for now
		} catch (InterruptedException e) {
			// Auto-generated catch block
			e.printStackTrace();
		} 

		assertTrue(this.timeOutHandled);

	}

	@Override
	public void timeoutElapsed(Object tag) {
		this.timeOutHandled = true;

	}

}

