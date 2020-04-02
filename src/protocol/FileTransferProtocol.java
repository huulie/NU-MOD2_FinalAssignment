package protocol;

/**
 * Protocol for NU-5 File Transfer (final assignment module 2).
 * This is version: ‘1.0’
 */
public class FileTransferProtocol {

	/**
	 * Version of this protocol.
	 */
	public static final String VERSION = "1.0";
	
	/**
	 * Delimiter used to separate arguments sent over the network.
	 */
	public static final String DELIMITER = ";";
	
	/**
	 * Signal to indicate unknown message.
	 */
	public static final char ERROR = '?'; 
			//From either side, indicates that message is not known/understood
	
}
