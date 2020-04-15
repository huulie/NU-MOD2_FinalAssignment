package userInterface;

/**
 * All commands used in a TUI for the FileTranfer client and server.
 * 
 * @author Huub Lievestro
 */
public class TUICommands {

	// Commands to control local TUI and/or program	
	public static final String EXIT = "exit";
	public static final String HELP = "help";
		
	/**
	 * start a session with the server.
	 */
	public static final String START_SESSION = "start session";
	
	/**
	 * list files on the server.
	 */
	public static final String LIST_FILES = "ls";
	
	/**
	 * list files on this client.
	 */
	public static final String LIST_FILES_LOCAL = "lsLocal";
	
	/**
	 * download a single file from the server.
	 */
	public static final String DOWNLOAD_SINGLE = "download";
	
	/**
	 * upload a single file to the server
	 */
	public static final String UPLOAD_SINGLE = "upload";
	
	/**
	 * delete a single file from the server.
	 */
	public static final String DELETE_SINGLE = "delete";
	
	/**
	 * check integrity of single file against the server
	 */
	public static final String CHECK_INTEGRITY = "check";
	
	/**
	 * manage uploaders.
	 */
	public static final String UPLOAD_MANAGER = "upManager";
	
	/**
	 * manage downloaders.
	 */
	public static final String DOWNLOAD_MANAGER = "downManager";
	
	/**
	 * NO FUNCTION YET.
	 */
	public static final String PAUSE = "pause";
	
	/**
	 * NO FUNCTION YET.
	 */
	public static final String RESUME = "resume";
}
