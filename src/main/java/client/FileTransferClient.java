package client;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import exceptions.EmptyResponseException;
import exceptions.ExitProgram;
import exceptions.NoMatchingFileException;
import exceptions.NotEnoughFreeSpaceException;
import exceptions.PacketException;
import exceptions.ServerFailureException;
import exceptions.UtilByteException;
import exceptions.UtilDatagramException;
import helpers.DownloadHelper;
import helpers.Helper;
import helpers.UploadHelper;
import network.NetworkLayer;
import network.Packet;
import network.TransportLayer;
import protocol.FileTransferProtocol;
import server.FileTransferClientHandler;
import userInterface.TUICommands;

/**
 * Client to interact with FileTransfer server.
 * @author huub.lievestro
 *
 */
public class FileTransferClient {

	/**
	 * DatagramSocket to receive and send packets.
	 */
	DatagramSocket socket;
	
	/**
	 * Network address of this client.
	 */
	InetAddress ownAddress;
	
	/**
	 * Network port this client, bound to the socket.
	 */
	int ownPort;
	
	/**
	 * Indicates if a session is started with a server.
	 */
	boolean sessionActive;
	
	/**
	 * Network address of the active server.
	 */
	InetAddress serverAddress;
	
	/**
	 * Network port of the active server.
	 */
	int serverPort;
	
	/**
	 * Name of the active server.
	 */
	private String serverName;
	
	/**
	 * TUI of this FileTransferServer. 
	 */
	private userInterface.TUI textUI;
	
	/**
	 * Local filesystem root.
	 */
	Path root;
	
	/**
	 * Local filesystem storage location of this client.
	 */
	Path fileStorage;
	
	/**
	 * Name of the local storage location of this client.
	 */
	String fileStorageDirName;
	
	/*
	 * Last retrieved list of files on server
	 */
	File[] serverFiles;
	
	/**
	 *  List of downloads, one for each connected downloadHelper. 
	 */
	private List<Helper> downloads; 
	
	/**
	 *  List of uploads, one for each connected uploadHelper. 
	 */
	private List<Helper> uploads;
	
	/**
	 * Indicates if client is running.
	 */
	boolean running;
	
	/**
	 * Name of this client.
	 */
	String name;

	
	/**
	 * Construct a new FileTransfer client.
	 * @param port to use for this client
	 */
	public FileTransferClient(int port) {
		this.textUI = new userInterface.TUI();
		
		this.fileStorageDirName = "FTCstorage";

		this.ownPort = port;
		
		this.sessionActive = false;
		
		this.downloads = new ArrayList<>();
		this.uploads = new ArrayList<>();
		
		while (this.name == null || this.name.isBlank()) {
			this.name = textUI.getString("What is the name of this client?");
		}

		// Do setup
		boolean setupSuccess = false;
		while (!setupSuccess) {
			try {
				setupSuccess = this.setup();
			} catch (exceptions.ExitProgram eExit) {
				// If setup() throws an ExitProgram exception, stop the program.
				if (!textUI.getBoolean("Do you want to retry setup?")) {
					setupSuccess = false;
				}
			}
		}
		this.running = true;
		
		this.clientRunning();
	}

	// ------------------ Client Setup --------------------------
	/**
	 * Sets up a new FileTransferClient.
	 * 
	 * @return boolean indicating if succeeded
	 * @throws ExitProgram if the user decides to exit the program.
	 */
	public boolean setup() throws exceptions.ExitProgram {
		this.showNamedMessage("Setting up the client...");
		boolean success = false;

		boolean successFileSystem = this.setupFileSystem();
		boolean succesSocket = this.setupSocket();
		boolean succesNetwork = this.setupOwnAddress();
		boolean succesServer = this.setServer();
		boolean succesSession = this.setupStartSession();

		success = successFileSystem && succesSocket && succesNetwork 
				&& succesServer && succesSession;
		
		if (success) {
			this.showNamedMessage("Setup complete!");
		}
		
		return success;
	}
	
	/**
	 * Sets up the file system.
	 * @return boolean indicating if succeeded
	 */
	public boolean setupFileSystem() {
		boolean success = false;
		this.root = Paths.get("").toAbsolutePath();
		this.showNamedMessage("Client root path set to: " + this.root.toString());
		
		this.fileStorage = root.resolve(fileStorageDirName);
		this.showNamedMessage("File storage set to: " + this.fileStorage.toString());

		try {
			Files.createDirectory(fileStorage);
			this.showNamedMessage("File storage directory did not exist:"
					+ " created " + fileStorageDirName + " in client root"); 
		} catch (java.nio.file.FileAlreadyExistsException eExist) {
			this.showNamedMessage("File storage directory already exist:"
					+ " not doing anything with " + fileStorageDirName + " in client root");
		} catch (IOException e) {
			this.showNamedError("Failed to create file storage:"
					+ e.getLocalizedMessage());
		}

		success = true;
		return success;
	}
	
	/**
	 * Sets up the socket.
	 * @return boolean indicating if succeeded
	 */
	public boolean setupSocket() throws ExitProgram {
		boolean success = false;
		
		this.showNamedMessage("Trying to open a new socket...");
		while (this.socket == null) { 
			try {
				this.socket = TransportLayer.openNewDatagramSocket(this.ownPort);
				this.showNamedMessage("Client now bound to port " + ownPort);
				success = true;
			} catch (SocketException e) {
				this.showNamedError("Something went wrong when opening the socket: "
						+ e.getLocalizedMessage());
				if (!textUI.getBoolean("Do you want to try again?")) {
					throw new exceptions.ExitProgram("User indicated to exit the "
							+ "program after socket opening failure.");
				}
			}
		}
		return success;
	}
	
	/**
	 * Sets up the client network information.
	 * @return boolean indicating if succeeded
	 */
	public boolean setupOwnAddress() {
		boolean success = false;
		try {
			this.ownAddress = NetworkLayer.getOwnAddress(); // TODO replace by discover?
			this.showNamedMessage("Client listing on: " + this.ownAddress);
			this.showNamedMessage("NOTE: depending on detection method,"
					+ " this may NOT be the actual interface used");
			this.showNamedMessage("Discovered preferred local address: " 
					+ NetworkLayer.discoverLocalAddress());
			success = true;
		} catch (UnknownHostException e) {
			this.showNamedMessage("Could not determine own address: " + e.getLocalizedMessage());
		} 
		return success;
	}
	
	/**
	 * Sets up the server network information.
	 * @return boolean indicating if succeeded
	 */
	public boolean setServer() {
		boolean success = false;
		
		this.serverPort = FileTransferProtocol.DEFAULT_SERVER_PORT;
		
		boolean searchByHostName = textUI.getBoolean("Do you want to search by hostname? "
				+ "(if not, client will try multicast search)");
		
		if (searchByHostName) {
			success = this.findServerByHostName();
		} else {
			success = this.discoverServer();
		}
		
		if (success) {
			this.showNamedMessage("Server set to " + this.serverName + ", with address " +
					this.serverAddress + " and port " + this.serverPort);
		}
		
		return success;
	}
	
	/**
	 * Sets up a session with server.
	 * Note: may be done later with START_SESSION
	 * @return boolean indicating if succeeded
	 */
	public boolean setupStartSession() { 
		this.showNamedMessage("Initiating session with server...");
		try {
			while (!this.requestSession()) {
				textUI.getBoolean("Try again?");
			}
		} catch (IOException | PacketException | UtilDatagramException e) {
			textUI.showError("Something went wrong while starting the session: " 
					+ e.getLocalizedMessage());
		}
		
		return this.sessionActive;
	}
	
	/**
	 * Find a server network address using its host name,
	 * and set server to found server.
	 * @return boolean indicating if succeeded
	 */
	public boolean findServerByHostName() {
		boolean success = false;
		this.serverName = textUI.getString("What is the hostname of the server?");
		
		try {
			this.serverAddress = NetworkLayer.getAdressByName(this.serverName); 
			success = true;
		} catch (UnknownHostException e) {
			textUI.showError("Something went wrong while searching for host " + this.serverName 
					+ ": " +  e.getLocalizedMessage());
		} 
		
		return success;
	}
	
	/**
	 * Find a server network address using broadcast of a DISCOVER packet,
	 * and set server to found server.
	 * @return boolean indicating if succeeded
	 */
	public boolean discoverServer() {
		boolean success = false;
		
		this.showNamedMessage("Note: if >1 server responds to DISCOVER, "
				+ "only the first (and probably fastest) one will be learned");
		
		try {
			this.socket.setBroadcast(true);
			String discoverBroadcast = FileTransferProtocol.DISCOVER 
					+ FileTransferProtocol.DELIMITER + this.name;
			
			InetAddress broadcast = NetworkLayer.getAdressByName("255.255.255.255");
			this.serverAddress = broadcast;
		
			Packet serverResponse = this.requestServer(discoverBroadcast);
			String[] responseString = this.getArguments(serverResponse.getPayloadString());
			if (responseString[0].equals(FileTransferProtocol.DISCOVER)) {
				this.serverAddress = serverResponse.getSourceAddress();
				this.serverName = responseString[1];
			} else {
				this.showNamedError("Incorrect response to DISCOVER: failing");
				this.socket.setBroadcast(false);
			}
		
			this.socket.setBroadcast(false);
			success = true;
		} catch (SocketException | UnknownHostException e) {
			textUI.showError("Something went wrong while searching for a server" 
					+ ": " +  e.getLocalizedMessage());
		} catch (EmptyResponseException e) {
			textUI.showError("Something went wrong while searching for a server" 
					+ "> empty response: " +  e.getLocalizedMessage());
		} catch (ServerFailureException e) {
			textUI.showError("Something went wrong while searching for a server" 
					+ "> server failed: " +  e.getLocalizedMessage());
		}
		
		return success;
	}
	
	// ------------------ Client Methods --------------------------

	/**
	 * Takes and processes user input while the client is running.
	 */
	public void clientRunning() {
		while (this.running) {
			String userInputString = textUI.getString("Please input:");
			this.processUserInput(userInputString);
		}
	}
	
	/**
	 * Take input from the user and call corresponding method,
	 *  with some relevant feedback to the user.
	 *  Note: most called methods return true on success = used to check.
	 * @param input from the user as a String
	 */
	public void processUserInput(String input) {
		String[] request = this.getArguments(input); 
		String command = request[0]; 

		try {
			switch (command) {
				case TUICommands.START_SESSION:
					if (!this.sessionActive) {
						this.showNamedMessage("Initiating session with server...");
						while (!this.requestSession()) {
							textUI.getBoolean("Try again?");
						}
					} else {
						this.showNamedMessage("Session is already active");
					}
					break;	

				case TUICommands.LIST_FILES: 
					this.showNamedMessage("Requesting list of files...");
					if (!this.requestListFiles()) { 
						this.showNamedError("Retrieving list of files failed");
					}
					break;
					
				case TUICommands.LIST_FILES_LOCAL: 
					this.showNamedMessage("Making list of local files...");
					this.showNamedMessage(Arrays.toString(this.getLocalFiles()));  
					break;

				case TUICommands.DOWNLOAD_SINGLE:
					File fileToDownload = this.selectServerFile();
					if (!this.downloadSingleFile(fileToDownload)) {
						this.showNamedError("Downloading file failed");
					}
					break;
					
				case TUICommands.UPLOAD_SINGLE:
					File fileToUpload = this.selectLocalFile();
					if (!this.uploadSingleFile(fileToUpload)) { 
						this.showNamedError("Uploading file failed");
					}
					break;
					
				case TUICommands.DELETE_SINGLE:
					File fileToDelete = this.selectServerFile();
					if (!this.deleteSingleFile(fileToDelete)) { 
						this.showNamedError("Deleting file failed");
					}
					break;

				case TUICommands.CHECK_INTEGRITY:
					File fileToCheck = this.selectLocalFile();
					if (!this.checkFile(fileToCheck)) { 
						this.showNamedError("Checking file failed");
					}
					break;
					
				case TUICommands.UPLOAD_MANAGER:
					if (!this.helperManager(this.uploads)) { 
						this.showNamedError("Upload manager failed!");
					}
					break;

				case TUICommands.DOWNLOAD_MANAGER:
					if (!this.helperManager(this.downloads)) {
						this.showNamedError("Download manager failed!");
					}
					break;
					
				case TUICommands.HELP:
					this.textUI.printHelpMenu();
					break;
				
				case TUICommands.EXIT:
					this.shutdown();
					break;
				
				default:
					this.showNamedError("Unknow command received (and ignoring it)"); 
			}
			this.showNamedMessage("... done!");
		} catch (IOException | PacketException | UtilDatagramException e) {
			this.showNamedError("Something went wrong: " + e.getLocalizedMessage());
		}
	}
	
	/**
	 * Split the requestString in command and optional arguments.
	 * @param requestString
	 * @return String[] with command and optional arguments
	 */
	public String[] getArguments(String requestString) {
		if (requestString == null) {
			this.showNamedError("cannot get arguments from a NULL string");
			return null;
		}
		return requestString.split(FileTransferProtocol.DELIMITER);
	}
	
	/**
	 * Request a session (= client handler) to the server.
	 * @return boolean indicating if succeeded
	 * @throws IOException
	 * @throws PacketException
	 * @throws UtilDatagramException
	 */
	public boolean requestSession() throws IOException, PacketException, UtilDatagramException {
		try {
			String sessionRequest = FileTransferProtocol.INIT_SESSION 
					+ FileTransferProtocol.DELIMITER 
					+ this.name;
			
			Packet responsePacket = this.requestServer(sessionRequest);
			String[] responseSplit = this.getArguments(responsePacket.getPayloadString());

			if (responseSplit[0].equals(FileTransferProtocol.INIT_SESSION)) {
				this.sessionActive = true;
				this.serverPort =  Integer.parseInt(responseSplit[1]); // update to clientHandler
				this.showNamedMessage("Session started with server port = " + this.serverPort);
				return true;
			} else {
				this.sessionActive = false;
				this.showNamedError("Invalid response to session init");
				this.sessionActive = false;
			}
		} catch (EmptyResponseException e) {
			this.showNamedError("Response from server was empty: " + e.getLocalizedMessage());
		} catch (ServerFailureException e) {
			this.showNamedError("FAILURE> " + e.getLocalizedMessage());
		}

		return this.sessionActive;
	}
	
	/**
	 * Request list of files on the server, and update serverFiles.
	 * @return boolean indicating if succeeded
	 * @throws IOException
	 * @throws PacketException
	 * @throws UtilDatagramException
	 */
	public boolean requestListFiles() throws IOException, PacketException, UtilDatagramException {
		boolean success = false;

		try {
			File[] fileArray = null; 

			Packet responsePacket = this.requestServer(FileTransferProtocol.LIST_FILES);

			byte[] responseBytes = responsePacket.getPayloadBytes();
			
			this.showNamedMessage("Server response received, now processing..."); 
			fileArray = util.Bytes.deserialiseByteArrayToFileArray(responseBytes);
			this.serverFiles = fileArray;
			this.showNamedMessage("LIST OF FILES: \n" + Arrays.toString(fileArray));
			
			success = true;

		} catch (ClassNotFoundException e) {
			this.showNamedError("Problems with received data: " + e.getLocalizedMessage());
		} catch (EmptyResponseException e) {
			this.showNamedError("Response from server was empty: " + e.getLocalizedMessage());
		} catch (ServerFailureException e) {
			this.showNamedError("FAILURE> " + e.getLocalizedMessage());
		}

		return success;
	}
	
	/**
	 * Request download of a single file from the server,
	 * and start a downloadHelper to receive it.
	 * @param File fileToDownload (NOTE: file as on server, not as to write on client!)
	 * @return boolean indicating if succeeded
	 */
	public boolean downloadSingleFile(File fileToDownload) {
		boolean success = false;
		this.showNamedMessage("WARNING: overwriting existing files!"); 
		try {
			// create downloadHandler
			File fileToWrite = new File(this.fileStorage.toString() +
					File.separator + fileToDownload.getName());
			DatagramSocket downloadSocket = TransportLayer.openNewDatagramSocket();
			
			DownloadHelper downloadHelper = new DownloadHelper(this,
					downloadSocket, this.serverAddress, -2, -1, fileToWrite, -1); 
			// note: uploaderPort, fileSize and startID still to set
			this.downloads.add(downloadHelper);

			// request file, provide downloaderHelper port
			String singleFileRequest = FileTransferProtocol.DOWNLOAD + 
					FileTransferProtocol.DELIMITER + 
					downloadSocket.getLocalPort();
			
			byte[] fileToDownloadBytes = util.Bytes.serialiseObjectToByteArray(fileToDownload); 
			
			Packet responsePacket = this.requestServer(singleFileRequest, fileToDownloadBytes);
			String[] responseSplit = this.getArguments(responsePacket.getPayloadString());
			
			if (responseSplit[0].contentEquals(FileTransferProtocol.UPLOAD)) {
				downloadHelper.setUploaderPort(Integer.parseInt(responseSplit[1])); 
				this.showNamedMessage("Uploader is on server port = " 
						+ Integer.parseInt(responseSplit[1])); 

				int totalFileSize = Integer.parseInt(responseSplit[2]);
				this.showNamedMessage("Uploader reports total file size = " 
						+ totalFileSize + " bytes");
				
				if (!this.checkFreeSpace(totalFileSize)) {
					this.downloads.remove(downloadHelper);
					return false;
				}
				
				int startID = Integer.parseInt(responseSplit[3]);
				this.showNamedMessage("Uploader starts at ID = " + startID);
				downloadHelper.setStartID(startID);
				
				// now everything is known: start download helper
				new Thread(downloadHelper).start();
				success = true;
			} else {
				this.showNamedError("Invalid response to download request");
				success = false;
			}

		} catch (IOException e) {
			this.showNamedError("Something went wrong: " + e.getLocalizedMessage());
		} catch (EmptyResponseException e) {
			this.showNamedError("Response from server was empty: " + e.getLocalizedMessage());
		} catch (ServerFailureException e) {
			this.showNamedError("SERVER FAILURE> " + e.getLocalizedMessage());
		} catch (NotEnoughFreeSpaceException e) {
			this.showNamedError("Not enough free space to store download:"
					+ e.getLocalizedMessage()); 
		}

		return success;
	}
	
	/**
	 * Request upload of a single file to the server,
	 * and start a uploadHelper to send it.
	 * @param File fileToUpload (NOTE: file as on client!)
	 * @return boolean indicating if succeeded
	 */
	public boolean uploadSingleFile(File fileToUpload) {
		boolean success = false;
		
		this.showNamedMessage("WARNING: overwriting existing files on server!"); 
		try {
			// create uploadHandler
			DatagramSocket uploadSocket = TransportLayer.openNewDatagramSocket();

			int fileSizeToUpload = (int) fileToUpload.length(); // TODO casting long to int!

			UploadHelper uploadHelper = new UploadHelper(this, uploadSocket,
					this.serverAddress, -2, fileSizeToUpload, fileToUpload); 
			// Note: downloaderPort still unset

			this.uploads.add(uploadHelper);

			// request file, provide uploaderHelper port
			String singleFileAnnouncement = FileTransferProtocol.UPLOAD + 
					FileTransferProtocol.DELIMITER + 
					uploadSocket.getLocalPort() + 
					FileTransferProtocol.DELIMITER + 
					fileSizeToUpload +
					FileTransferProtocol.DELIMITER + 
					uploadHelper.getStartId(); 
			
			byte[] fileToUploadBytes = util.Bytes.serialiseObjectToByteArray(fileToUpload); 
			
			Packet responsePacket = this.requestServer(singleFileAnnouncement, fileToUploadBytes);
			String[] responseSplit = this.getArguments(responsePacket.getPayloadString());
			
			if (responseSplit[0].contentEquals(FileTransferProtocol.DOWNLOAD)) {
				uploadHelper.setDownloaderPort(Integer.parseInt(responseSplit[1])); 
				this.showNamedMessage("Downloader is on server port = " 
						+ Integer.parseInt(responseSplit[1])); 

				// now everything is known: start download helper
				new Thread(uploadHelper).start();
				success = true;
			} else {
				this.showNamedError("Invalid response to upload announcement");
				success = false;
			}

		} catch (IOException e) {
			this.showNamedError("Something went wrong: " + e.getLocalizedMessage());
		} catch (EmptyResponseException e) {
			this.showNamedError("Response from server was empty: " + e.getLocalizedMessage());
		} catch (ServerFailureException e) {
			this.showNamedError("SERVER FAILURE> " + e.getLocalizedMessage());
		}

		return success;
	}
	
	/**
	 * Request deletion of a single file to the server.
	 * @param File fileToDelete (NOTE: file as on server!)
	 * @return boolean indicating if succeeded
	 */
	public boolean deleteSingleFile(File fileToDelete) {
		boolean success = false;
		
		this.showNamedMessage("WARNING: this deletes files on server!");
		try {
			String singleFileDeleteRequest = FileTransferProtocol.DELETE;
			
			byte[] fileToDeleteBytes = util.Bytes.serialiseObjectToByteArray(fileToDelete); 
			
			Packet responsePacket = this.requestServer(singleFileDeleteRequest, fileToDeleteBytes);
			String[] responseSplit = this.getArguments(responsePacket.getPayloadString());
			
			if (responseSplit[0].contentEquals(FileTransferProtocol.DELETE)) {
				this.showNamedMessage("File deleted!"); 
				success = true;
			}
		} catch (IOException e) {
			this.showNamedError("Something went wrong while serialising file object: " 
					+ e.getLocalizedMessage());
		} catch (EmptyResponseException e) {
			this.showNamedError("Response from server was empty: " + e.getLocalizedMessage());
		} catch (ServerFailureException e) {
			this.showNamedError("FAILURE> " + e.getLocalizedMessage());
		}
		
		return success;
	}
	
	/**
	 * Request hash of a single file on the server.
	 * @param File fileToCheck (NOTE: file as on client, will be matched to server!)
	 * @return boolean indicating if succeeded
	 */
	public boolean checkFile(File fileToCheck) {
		boolean success = false;

		try {
			boolean sameHash = this.compareLocalRemoteHash(fileToCheck);

			if (sameHash) {
				this.showNamedMessage("Local and remote files have the same hash: INTEGRITY OK");
			} else {
				this.showNamedMessage("Local and remote files have the different hash:"
						+ " INTEGRITY FAILED");
			}
			success = true;

		} catch (NoMatchingFileException e) {
			this.showNamedError("Something went wrong while comparing the files: "
					 + e.getLocalizedMessage());
		} catch (EmptyResponseException e) {
			this.showNamedError("Response from server was empty: " + e.getLocalizedMessage());
		} catch (ServerFailureException e) {
			this.showNamedError("FAILURE> " + e.getLocalizedMessage());
		}

		return success;
	}

	/**
	 * Compare hash of local file with hash of matching file on server.
	 * @param fileToCompare (as on client)
	 * @return boolean indicating if both files have same hash
	 * @throws NoMatchingFileException 
	 * @throws EmptyResponseException 
	 * @throws ServerFailureException 
	 */
	public boolean compareLocalRemoteHash(File fileToCompare) 
			throws NoMatchingFileException, EmptyResponseException, ServerFailureException {
		boolean sameHash = false; // TODO default to false??
		String fileName = fileToCompare.getName();

		try {
			String localHash = util.FileOperations.getHashHexString(fileToCompare);
			this.showNamedMessage("Hash of local file: " + localHash);

			String remoteHash = null;
			
			File fileOnServer = this.matchToServerFile(fileName);
			
			if (fileOnServer != null) {
				String checkFileRequest = FileTransferProtocol.HASH +
						FileTransferProtocol.DELIMITER +
						localHash;

				byte[] fileToCheckBytes = util.Bytes.serialiseObjectToByteArray(fileOnServer); 

				Packet responsePacket = this.requestServer(checkFileRequest, fileToCheckBytes);
				String[] responseSplit = this.getArguments(responsePacket.getPayloadString());

				if (responseSplit[0].contentEquals(FileTransferProtocol.HASH)) {
					remoteHash = responseSplit[1];
					this.showNamedMessage("Hash of remote file: " + remoteHash);
				}

				if (remoteHash.equals(localHash)) {
					sameHash = true;
				} else {
					sameHash = false;
				}

			} else {
				this.showNamedError("Matched file on server is null!"); 
				throw new NoMatchingFileException("Matched file on server is null!");
			}

		} catch (NoSuchAlgorithmException | IOException e) {
			this.showNamedError("Something went wrong while comparing the files: "
					 + e.getLocalizedMessage());
		} catch (NoMatchingFileException e) {
			this.showNamedError("No matching file on the server to check!"); 
			throw new NoMatchingFileException("No matching file found on the server!");
		} catch (ServerFailureException e) {
			throw new ServerFailureException(e.getLocalizedMessage());
		}

		return sameHash;
	}
	
	/**
	 * Manages helper threads: can pause/resume.
	 * @param listOfHelpers to manage
	 * @return boolean indicating if succeeded
	 */
	public boolean helperManager(List<Helper> listOfHelpers) { 
		boolean success = false;

		this.showNamedMessage("Found helpers: \n" + Arrays.toString(listOfHelpers.toArray()));

		int selectedIndex = -1;
		while (!(selectedIndex >= 0 && selectedIndex < listOfHelpers.size())) {
			selectedIndex = textUI.getInt("Which helper to select?"); 
		}

		Helper selectedHelper = listOfHelpers.get(selectedIndex);

		if (selectedHelper.isPaused()) {
			if (textUI.getBoolean("Would you like to resume this helper?")) {
				selectedHelper.resume();
			}
		} else {
			if (textUI.getBoolean("Would you like to pause this helper?")) {
				selectedHelper.pause();
			}
		}
		success = true;

		return success;
	}
	
	/**
	 * Get list of local files in client storage. 
	 * @return File[] list of local Files
	 */
	public File[] getLocalFiles() {
		File[] filesArray = new File(this.fileStorage.toString()).listFiles(
				new FileFilter() {
					@Override
					public boolean accept(File file) {
						return !file.isHidden();
					}
				});
		return filesArray;
	}
	
	/**
	 * Select a file from list of local files in client storage. 
	 * @return File selected from local file storage.
	 */
	public File selectLocalFile() {
		File[] localFiles = this.getLocalFiles();
		
		int selectedIndex = -1;
		while (!(selectedIndex >= 0 && selectedIndex < localFiles.length)) {
			selectedIndex = textUI.getInt("Which index to select?"); 
		}
		return localFiles[selectedIndex];
	}
	
	/**
	 * Select a file from list of local files in client storage. 
	 * @return File (as on server), selected from server file storage.
	 */
	public File selectServerFile() {
		int selectedIndex = -1;
		try {
			this.requestListFiles();

			while (!(selectedIndex >= 0 && selectedIndex < this.serverFiles.length)) {
				selectedIndex = textUI.getInt("Which index to select?"); 
			}

		} catch (IOException | PacketException | UtilDatagramException e) {
			this.showNamedError("Something went wrong while refreshing"
					+ " the server file list: " + e.getLocalizedMessage());
		}

		return this.serverFiles[selectedIndex];
	}
	
	/**
	 * Match file from client storage to a file on the server storage.
	 * @param fileName (as local file) to match on server.
	 * @return File (as on server), matching the local File.
	 * @throws NoMatchingFileException
	 */
	public File matchToServerFile(String fileName) throws NoMatchingFileException {
		File fileOnServer = null;
		
		try {
			this.showNamedMessage("Matching local file to file on server...");
			this.requestListFiles();

		} catch (IOException | PacketException | UtilDatagramException e) {
			this.showNamedMessage("Something went wrong while communicating with"
					+ " the server file list: " + e.getLocalizedMessage());
		}
		
		List<File> matchesOnServer = new ArrayList<File>();
		
		for (int i = 0; i < this.serverFiles.length; i++) {
			if (this.serverFiles[i].getName().equals(fileName)) {
				matchesOnServer.add(this.serverFiles[i]);
			}
		}
		
		if (matchesOnServer.size() <= 0) {
			this.showNamedError("No matching file found!"); 
			throw new NoMatchingFileException("No matching file found on the server!");
		} else if (matchesOnServer.size() == 1) {
			fileOnServer = matchesOnServer.get(0);
			this.showNamedMessage("One matching file found: " + fileOnServer);
		} else { // more matching
			this.showNamedMessage("Found matches: \n" 
					+ Arrays.toString(matchesOnServer.toArray()));

			int selectedIndex = -1;
			while (!(selectedIndex >= 0 && selectedIndex < this.serverFiles.length)) {
				selectedIndex = textUI.getInt("Which index is the correct match?"); 
			}
			fileOnServer = matchesOnServer.get(selectedIndex);
		}
		return fileOnServer;		
	}
	
	/**
	 * Check if there is enough space to store the file.
	 * @param totalFileSize of the file
	 * @return true if there more than totalFileSize free space
	 * @throws NotEnoughFreeSpaceException
	 */
	public boolean checkFreeSpace(int totalFileSize) throws NotEnoughFreeSpaceException {
		int freeSpace = (int) this.fileStorage.toFile().getUsableSpace(); // TODO casting long->int!
		if (freeSpace > totalFileSize) {
			this.showNamedMessage("Free space remaining after upload: " 
					+ (freeSpace - totalFileSize) + " bytes");
			return true;
		} else {
			throw new NotEnoughFreeSpaceException(totalFileSize 
					+ "bytes don't fit in " + freeSpace + "bytes of free space");
		}
	}
	
	/**
	 * Make a request to the server, and receive response.
	 * @param requestString part of the request
	 * @param requestBytes part of the request
	 * @return Packet response from the server
	 * @throws EmptyResponseException 
	 * @throws ServerFailureException 
	 */
	public Packet requestServer(String requestString, byte[] requestBytes) 
			throws EmptyResponseException, ServerFailureException {
		byte[] requestStringBytes = requestString.getBytes();
		
		byte[] bytesToServer = requestStringBytes;

		if (requestBytes != null) {
			bytesToServer = util.Bytes.concatArray(requestStringBytes, requestBytes);
		}
		this.sendBytesToServer(bytesToServer, requestStringBytes.length);

		// now wait for response 
		Packet receivedPacket = this.receiveServerResponse();

		if (receivedPacket == null) {
			throw new EmptyResponseException("Response from server was null");
		} else if (receivedPacket.getPayloadString().startsWith(FileTransferProtocol.FAILED)) {
			String failureMessage = this.getArguments(receivedPacket.getPayloadString())[1];
			throw new ServerFailureException("SERVER FAILED: " + failureMessage);
		} else {
			return receivedPacket; 
		}
	}

	/**
	 * Make a request to the server with only a requestString, and receive response.
	 * @param requestString part of the request	 
	 * @return Packet response from the server
	 * @throws EmptyResponseException
	 * @throws ServerFailureException 
	 */
	public Packet requestServer(String requestString) 
			throws EmptyResponseException, ServerFailureException {
		return this.requestServer(requestString, null);
	}
	
	/**
	 * Send bytes to the server, contained in a Packet.
	 * @param bytesToSend to server
	 * @param byteOffset due to String part
	 */
	public void sendBytesToServer(byte[] bytesToSend, int byteOffset) {
		try { // to construct and send a packet
			Packet packet = new Packet(
						0, // TODO id
						this.ownAddress,
						this.ownPort, 
						this.serverAddress, 
						this.serverPort,
						bytesToSend, byteOffset
				);
			
			TransportLayer.sendPacket(
					this.socket,
					packet,
					this.serverPort
			); 
			
			// this.showNamedMessage("Bytes send to server!"); // for debugging
			
		} catch (PacketException | IOException | UtilByteException | UtilDatagramException e) {
			this.showNamedError("Something went wrong while sending bytes: "
					+ e.getLocalizedMessage());
		}
	}
	
	/**
	 * Receive response from server.
	 * @return Packet response from server
	 */
	public Packet receiveServerResponse() {
		this.showNamedMessage("Waiting for server response...");

		Packet receivedPacket = null;
		try {
			this.socket.setSoTimeout(10000); //TODO set it permanently in setup? 

			receivedPacket = TransportLayer.receivePacket(this.socket);

			if (!(receivedPacket.getSourceAddress().equals(this.serverAddress)
					&& receivedPacket.getSourcePort() == this.serverPort)
					&& !this.socket.getBroadcast()) {  
				this.showNamedError("SECURITY WARNING: this response is NOT"
						+ " coming for known server > dropping it");
				return null;
			}

		} catch (SocketTimeoutException e) { // on TimeOut
			this.showNamedError("Receiving a server response timed out");
			return null;
		} catch (SocketException e) {
			this.showNamedError("Something went wrong while receiving"
					+ " a server response: " + e.getLocalizedMessage());
			return null;
		} catch (IOException | PacketException | UtilDatagramException e) {
			this.showNamedError("Something went wrong while receiving"
					+ " a server response: " + e.getLocalizedMessage());
			return null;
		}

		return receivedPacket;
	}
	
	/**
	 * Shutdown client.
	 */
	public void shutdown() {
		this.showNamedMessage("See you later!");
		this.running = false;
		this.socket.close();

	}
	
	public String getName() {
		return name;
	}
	
	/**
	 * Show message on the textUIT with name of this client.
	 * @param message to display
	 */
	public void showNamedMessage(String message) {
		textUI.showNamedMessage(this.name, message);
	}
	
	/**
	 * Show error on the textUIT with name of this client.
	 * @param message to display
	 */
	public void showNamedError(String message) {
		textUI.showNamedError(this.name, message);
	}
	
	// ------------------ Main --------------------------

	/**
	 * Use this main method to boot a new FileTransfer client.
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Welcome to the FileTransfer Client! \n Starting...");
		
		int port; 
		
		if (args.length < 0 || args.length > 1) {
			System.out.println("Syntax: FileTranferClient <port>");
			return;
		} else if (args.length == 1) {
			port = Integer.parseInt(args[0]);
			System.out.println("Using specified client port " + port + "...");
		} else {
			port = FileTransferProtocol.DEFAULT_CLIENT_PORT;
			System.out.println("Using default client port " + port + "...");
		}
			
		FileTransferClient client = new FileTransferClient(port);
		System.out.println("Starting client...");
	}

}
