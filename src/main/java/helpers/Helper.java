package helpers;

/**
 * Interface of a Helper class.
 * @author huub.lievestro
 *
 */
public interface Helper {

	/**
	 * Check if Helper is paused.
	 * @return true if Helper is paused
	 */
	public boolean isPaused(); // interface method (does not have a body)
	
	/**
	 * Pause Helper.
	 */
	public void pause(); // interface method (does not have a body)
	
	/**
	 * REsume Helper.
	 */
	public void resume(); // interface method (does not have a body)
	
}
