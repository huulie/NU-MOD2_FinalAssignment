package protocol;

/**
 * Protocol for NU-5 File Transfer (final assignment module 2).
 * This is version: 2020-04-15
 */
public class FileTransferProtocol {
	// -----------------------------------------------------------------------
	// Network settings:
	
	/**
	 * Default port used by client (may be changed).
	 */
	public static final int DEFAULT_CLIENT_PORT = 1234; 
	
	/**
	 * Default port used by server (note: clients need fixed port).
	 */
	public static final int DEFAULT_SERVER_PORT = 4567; 
	
	/**
	 * Maximal size of a Packet (data for the DatagramPacket), consisting of header and payload.
	 */
	public static final int MAX_PACKET_SIZE = 1024;


	// -----------------------------------------------------------------------
	// Commands and arguments (send as String in a Packet, objects are send as byte[]):
	
	/**
	 * Delimiter used to separate arguments sent over the network.
	 */
	public static final String DELIMITER = ";";
	
	/**
	 * Signal to indicate unknown message.
	 * (from either side, indicates that message is not known/understood)
	 */
	public static final char ERROR = '?'; 
	
	/**
	 * When send by client: Discover server in local network.
	 * 	Arguments (separated by delimiter): String nameOfClient
	 * 	Bytes: -
	 * 
	 * When send by server: Respond to client sending DISCOVER in local network.
	 * 	Arguments (separated by delimiter): String nameOfServer
	 * 	Bytes: -
	 */
	public static final String DISCOVER = "DISCOVER";
	
	/**
	 * When send by client: initialise session with server.
	 * 	Arguments (separated by delimiter): String nameOfClient
	 * 	Bytes: -
	 * 
	 * When send by server: Respond to client sending INIT.
	 * 	Arguments (separated by delimiter): int serverPort, running clientHandler
	 * 	Bytes: -
	 */
	public static final String INIT_SESSION = "INIT"; 
	
	/**
	 * When send by client: request list of file on server.
	 * 	Arguments (separated by delimiter): -
	 * 	Bytes: -
	 * 
	 * When send by server: Respond to client sending LIST_FILES.
	 * 	Arguments (separated by delimiter): -
	 * 	Bytes: File[] fileArray with all files in server storage
	 */
	public static final String LIST_FILES = "ls";
	
	/**
	 * When send by client: request download of file from server.
	 * 	Arguments (separated by delimiter): int portOfDownloader
	 * 	Bytes: File fileToDownload
	 * 
	 * When send by server: Respond to client sending UPLOAD.
	 * 	Arguments (separated by delimiter): 
	 * 		int portOfUploader; int totalFileSize (in bytes); int startID
	 * 	Bytes: File[] fileToDownload (to client)
	 */
	public static final String DOWNLOAD = "DOWNLOAD";
	
	/**
	 * When send by client: request upload of file to server.
	 * 	Arguments (separated by delimiter): 
	 * 		int portOfUploader; int totalFileSize (in bytes); int startID
	 * 	Bytes: File[] fileToUpload 
	 * 
	 * When send by server: Respond to client sending DOWNLOAD.
	 * 	Arguments (separated by delimiter): int portOfDownloader
	 * 	Bytes: File fileToUpload (to server)
	 */
	public static final String UPLOAD = "UPLOAD";
	
	/**
	 * When send by client: request deletion of file on server.
	 * 	Arguments (separated by delimiter): -
	 * 	Bytes: File[] fileToDelete 
	 * 
	 * When send by server: Respond to client confirming DELETE.
	 * 	Arguments (separated by delimiter): -
	 * 	Bytes: File fileToDelete (on server)
	 */
	public static final String DELETE = "DELETE";
	
	/**
	 * When send by client: request hash of file on server.
	 * 	Arguments (separated by delimiter): String localHash
	 * 	Bytes: File[] fileToCheck 
	 * 
	 * When send by server: Respond to client sending HASH.
	 * 	Arguments (separated by delimiter): String remoteHash
	 * 	Bytes: File fileToCheck (on server)
	 */
	public static final String HASH = "HASH";
	
	/**
	 * When send by client: NOT USED (YET)
	 * 
	 * When send by server: server has failed.
	 * 	Arguments (separated by delimiter): String failureMessage
	 * 	Bytes: -
	 */
	public static final String FAILED = "FAILED";


	// -----------------------------------------------------------------------
		// Up-/download helper communication (send as byte[] in a Packet):
	/**
	 * From downloader to uploader: start transmission of bytes
	 * (used when downloader has to contact uploader first, e.g. trough firewalls)
	 */
	public static final byte[] START_DOWNLOAD = "START".getBytes(); 
	
	/**
	 * Mostly from downloader to uploader (but may be used v.v.): packet received!
	 */
	public static final byte[] ACK = "ACK".getBytes(); 
	
	/**
	 * Mostly from downloader to uploader (but may be used v.v.): pause transfer.
	 */
	public static final byte[] PAUSE_DOWNLOAD = "PAUSE".getBytes(); 
	
	/**
	 * Mostly from downloader to uploader (but may be used v.v.): resume transfer.
	 */
	public static final byte[] RESUME_DOWNLOAD = "RESUME".getBytes();


	// -----------------------------------------------------------------------
	// Packet header format (send as bytes in a DatagramPacket, to extend datagram header)
	
	/**
	 * Header field: id. 
	 * 	Length: 4 bytes (the int2Byte always puts in block of 4 bytes)
	 * 	Representing: id of Packet
	 * 	NOTE: ID should be encoded with big-endian encoding
	 * 	NOTE: ID may not be larger than MAX_ID
	 */
	public static final int HEADER_ID_START = 0;
	public static final int HEADER_ID_LAST = HEADER_ID_START + 3; 
	
	
	/**
	 * Header field: header length. 
	 * 	Length: 4 bytes (the int2Byte always puts in block of 4 bytes)
	 * 	Representing: lenght of header
	 * 	NOTE: header length should be encoded with big-endian encoding
	 * 	NOTE: value may not be larger than 4 bytes [= 2^(4*8)]
	 */
	public static final int HEADER_HEADER_LENGTH_START = HEADER_ID_LAST + 1;
	public static final int HEADER_HEADER_LENGTH_LAST = HEADER_HEADER_LENGTH_START + 3; 
	
	/**
	 * Header field: byte offset. 
	 * 	Length: 4 bytes (the int2Byte always puts in block of 4 bytes)
	 * 	Representing: offset of bytes in payload of Packet (after String payload)
	 * 	NOTE: byte offset should be encoded with big-endian encoding
	 *  NOTE: value may not be larger than 4 bytes [= 2^(4*8)]
	 */
	public static final int HEADER_BYTE_OFFSET_START = HEADER_HEADER_LENGTH_LAST + 1;
	public static final int HEADER_BYTE_OFFSET_LAST = HEADER_BYTE_OFFSET_START + 3;

	/**
	 * Actual value of total header size: set to last assigned header field, in bytes.
	 * NOTE: plus one, because indices start at zero (and lengths at one)
	 * NOTE: this length may not be larger than 4 bytes [= 2^(4*8)]
	 */
	public static final int TOTAL_HEADER_SIZE = HEADER_BYTE_OFFSET_LAST + 1;
	
	/**
	 * Index of first byte of the payload, starting after the header. 
	 * (note: header size is length, so already plus one)
	 */
	public static final int PAYLOAD_START = TOTAL_HEADER_SIZE; // TODO header size already plus one
	
	/**
	 * Maximal value of the ID field in the header (based on field size in bytes).
	 */
	public static final int MAX_ID = (int) 
			Math.pow(2, (double) ((HEADER_ID_LAST - HEADER_ID_START + 1) * 8)) - 1;
	
	/**
	 * Maximal value of the payload in a Packet (based on max packetSize - headerSize).
	 */
	public static final int MAX_PAYLOAD_LENGTH = MAX_PACKET_SIZE - TOTAL_HEADER_SIZE; 
}
