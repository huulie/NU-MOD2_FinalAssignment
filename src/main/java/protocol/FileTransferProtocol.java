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
	public static final int DEFAULT_CLIENT_PORT = 1234; 
	
	/**
	 * TODO .
	 */
	public static final int DEFAULT_SERVER_PORT = 4567; 
	
	/**
	 * TODO .
	 */
	public static final byte[] INIT_SESSION = "INIT".getBytes(); 
	
	/**
	 * TODO . only in current directory
	 */
	public static final String LIST_FILES = "ls";
	
	/**
	 * TODO download: dowloadHelper port; file (object as bytes); 
	 */
	public static final String DOWNLOAD = "DOWNLOAD";
	
	/**
	 * TODO upload: uploadHelper port, file (object as bytes)  
	 */
	public static final String UPLOAD = "UPLOAD";
	
	/**
	 * TODO delete: file (object as bytes); 
	 */
	public static final String DELETE = "DELETE";
	
	/**
	 * TODO .
	 */
	public static final byte[] START_DOWNLOAD = "START".getBytes(); 
	
	/**
	 * TODO .
	 */
	public static final byte[] ACK = "ACK".getBytes(); 
	
	
	/**
	 * TODO
	 */
	// PACKET DATA FORMAT: header (x bytes) + payload (maximum size: PACKET_SIZE - x bytes)
	// header (bytes): id (), headerLength ( = , byteOffset()
	// NOTE: number of bytes of payloadLength field is dependent on PACKET_SIZE: length (in bytes) = log2(PACKET_SIZE)/8
	// NOTE: part of the datagram header (like address and port) is managed by the used libraries, and thus not included here.

	/**
	 * TODO .
	 */
	public static final int MAX_PACKET_SIZE = 1024; // TODO was 256
	
	/**
	 * TODO . 
	 * NOTE: ID should be encoded with big-endian encoding
	 * NOTE: ID may not be larger than 4 bytes = 4*8 bits => (4*8)^2 = 1024
	 */
	public static final int HEADER_ID_START = 0;
	public static final int HEADER_ID_LAST = HEADER_ID_START 
			//+ (int) Math.ceil(Math.log(MAX_PACKET_SIZE) / Math.log(2) / 8);
			+ 3; // TODO the int2Byte always puts in block of 4 bytes
	
	/**
	 * TODO . 
	 * NOTE: payload length should be encoded with big-endian encoding
	 * NOTE: may not be larger than 4 bytes = 4*8 bits => (4*8)^2 = 1024
	 */
	public static final int HEADER_HEADER_LENGTH_START = HEADER_ID_LAST + 1;
	public static final int HEADER_HEADER_LENGTH_LAST = HEADER_HEADER_LENGTH_START 
			//+ (int) Math.ceil(Math.log(MAX_PACKET_SIZE) / Math.log(2) / 8);
			+ 3; // TODO the int2Byte always puts in block of 4 bytes 
	
	/**
	 * TODO . 
	 * NOTE: payload length should be encoded with big-endian encoding
	 */
	public static final int HEADER_BYTE_OFFSET_START = HEADER_HEADER_LENGTH_LAST + 1;
	public static final int HEADER_BYTE_OFFSET_LAST = HEADER_BYTE_OFFSET_START 
			//+ (int) Math.ceil(Math.log(MAX_PACKET_SIZE) / Math.log(2) / 8);
			+ 3; // TODO the int2Byte always puts in block of 4 bytes 

	/**
	 * TODO .
	 * set total header size to last assigned header field, in bytes
	 * NOTE: plus one, because indices start at zero (and lengths at one)
	 * NOTE: may not be larger than 4 bytes = 4*8 bits => (4*8)^2 = 1024
	 */
	public static final int TOTAL_HEADER_SIZE = HEADER_BYTE_OFFSET_LAST + 1;
	
	public static final int PAYLOAD_START = TOTAL_HEADER_SIZE; // TODO header size already plus one
	

	
	/**
	 * TODO .
	 */
	public static final int MAX_PAYLOAD_LENGTH = MAX_PACKET_SIZE - TOTAL_HEADER_SIZE; 
}
