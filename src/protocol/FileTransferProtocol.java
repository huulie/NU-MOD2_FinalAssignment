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
	
	/**
	 * TODO .
	 */
	public static final int CLIENT_PORT = 1234; 
	
	/**
	 * TODO .
	 */
	public static final int SERVER_PORT = 4567; 
	

	
	/**
	 * TODO
	 */
	// PACKET DATA FORMAT: header (x bytes) + payload (maximum size: PACKET_SIZE - x bytes)
	// header (bytes): payloadLength ( =
	// NOTE: number of bytes of payloadLength field is dependent on PACKET_SIZE: length (in bytes) = log2(PACKET_SIZE)/8
	// NOTE: part of the datagram header (like address and port) is managed by the used libraries, and thus not included here.

	/**
	 * TODO .
	 */
	public static final int MAX_PACKET_SIZE = 256;
	
	/**
	 * TODO . 
	 * NOTE: payload length should be encoded with big-endian encoding
	 */
	public static final int HEADER_PAYLOAD_LENGTH_START = 0;
	public static final int HEADER_PAYLOAD_LENGTH_LAST = HEADER_PAYLOAD_LENGTH_START 
			//+ (int) Math.ceil(Math.log(MAX_PACKET_SIZE) / Math.log(2) / 8);
			+ 3; // TODO the int2Byte always puts in block of 4 bytes 

	/**
	 * TODO .
	 * set total header size to last assigned header field
	 */
	public static final int TOTAL_HEADER_SIZE = HEADER_PAYLOAD_LENGTH_LAST;
	
	public static final int PAYLOAD_START = HEADER_PAYLOAD_LENGTH_LAST+1;
	

	
	/**
	 * TODO .
	 */
	public static final int MAX_PAYLOAD_LENGTH = MAX_PACKET_SIZE - TOTAL_HEADER_SIZE; 
}
