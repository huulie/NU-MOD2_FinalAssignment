package util;

/**
 * Interface for timeout event handlers.
 * 
 * @author Jaco ter Braak, University of Twente.
 * @version 11-01-2014
 */
public interface ITimeoutEventHandler {
    /**
     * Is triggered when the timeout has elapsed.
     */
    void timeoutElapsed(Object tag);
}
