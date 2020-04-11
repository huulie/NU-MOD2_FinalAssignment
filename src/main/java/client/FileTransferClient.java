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

import UI.TUICommands;
import exceptions.EmptyResponseException;
import exceptions.ExitProgram;
import exceptions.NoMatchingFileException;
import exceptions.PacketException;
import exceptions.UtilByteException;
import exceptions.UtilDatagramException;
import helpers.DownloadHelper;
import helpers.UploadHelper;
import network.NetworkLayer;
import network.Packet;
import network.TransportLayer;
import protocol.FileTransferProtocol;
import server.FileTransferClientHandler;

public class FileTransferClient {

	/**
	 * TODO .
	 */
	DatagramSocket socket;
	
	/**
	 * TODO
	 */
	InetAddress ownAddress;
	
	/**
	 * TODO
	 */
	int ownPort;
	
	/**
	 * TODO
	 */
	InetAddress serverAddress;
	
	/**
	 * TODO 
	 */
	boolean sessionActive;
	
	/**
	 * TODO
	 */
	int serverPort;
	
	/** The TUI of this FileTransferServer. */
	private UI.TUI TUI; 
	
	/**
	 * TODO
	 */
	Path root;
	Path fileStorage;
	String fileStorageDirName;
	
	/*
	 * TODO
	 */
	File[] serverFiles;
	
	
	/**
	 *  List of downloads, one for each connected downloadHelper. 
	 *  */
	private List<DownloadHelper> downloads;
	
	/**
	 *  List of uploads, one for each connected uploadHelper. 
	 *  */
	private List<UploadHelper> uploads;
	
	boolean running;
	
	String name;

	
	/**
	 * Construct a new FileTransfer client.
	 * @param socket
	 * @param port
	 */
	public FileTransferClient(int port) {
		this.TUI = new UI.TUI();
		
		this.fileStorageDirName = "FTCstorage";

		this.ownPort = port;
		
		this.sessionActive = false; // TODO may become int to support multiple sessions for one client? 
		
		this.downloads = new ArrayList<>();
		this.uploads = new ArrayList<>();
		
		name = "FTClient"; // TODO fixed name, let user set it?

		// Do setup
		boolean setupSucces = false;
		while (!setupSucces) {
			try {
				setupSucces = this.setup();
			} catch (exceptions.ExitProgram eExit) {
				// If setup() throws an ExitProgram exception, stop the program.
				if (!TUI.getBoolean("Do you want to retry setup?")) {
					setupSucces = false;
				}
			}
		}
		this.running = true;
		
		this.clientRunning();
	}

	// ------------------ Client Setup --------------------------
	/**
	 * Sets up a new FileTransferClient TODO
	 * 
	 * @throws ExitProgram if a connection can not be created on the given 
	 *                     port and the user decides to exit the program.
	 * @ensures a serverSocket is opened.
	 */
	public boolean setup() throws exceptions.ExitProgram {
		this.showNamedMessage("Setting up the client...");
		boolean success = false;

		// First, initialise the Server.
		// SERVERNAME = TUI.getString("What is the name of this server?"); // TODO name? 

		boolean successFileSystem = this.setupFileSystem();
		boolean succesSocket = this.setupSocket();
		this.setupOwnAddress();
		boolean succesServer = this.setServer();
		boolean succesSession = this.setupStartSession();

		success = successFileSystem && succesSocket && succesServer && succesSession;
		
		if (success) {
			this.showNamedMessage("Setup complete!");
		}
		
		return success;
	}
	
	public boolean setupFileSystem() {
		boolean success = false;
		this.root = Paths.get("").toAbsolutePath(); // TODO suitable method? https://www.baeldung.com/java-current-directory
		this.showNamedMessage("Client root path set to: " + this.root.toString());
		
		this.fileStorage = root.resolve(fileStorageDirName);
		this.showNamedMessage("File storage set to: " + this.fileStorage.toString());

		
//		if (!Files.exists(fileStorage)) { // TODO: use if or catch exception
            try {
				Files.createDirectory(fileStorage);
		    	this.showNamedMessage("File storage directory did not exist: created " + fileStorageDirName + " in client root"); 
            } catch(java.nio.file.FileAlreadyExistsException eExist) {
            	this.showNamedMessage("File storage directory already exist: not doing anything with " + fileStorageDirName + " in client root");
            } catch (IOException e) {
				// TODO Auto-generated catch block
				this.showNamedError("Failed to create file storage: server CRASHED because " + e.getLocalizedMessage());
				e.printStackTrace();
			}
//        } else {
//	    	this.showNamedMessage("File storage directory already exist: not doing anything with " + fileStorageDirName + " in client root"); 
//        }
            success = true;
    		return success;
	}
	
	public boolean setupSocket() throws ExitProgram {
		boolean success = false;
		
		this.showNamedMessage("Trying to open a new socket...");
		while (this.socket == null) { // TODO: ask for server port?
			//port = TUI.getInt("Please enter the server port.");

			try {
				this.socket = TransportLayer.openNewDatagramSocket(this.ownPort);
				this.showNamedMessage("Client now bound to port " + ownPort);
				success = true;
			} catch (SocketException e) {
				this.showNamedError("Something went wrong when opening the socket: "
						+ e.getLocalizedMessage());
				if (!TUI.getBoolean("Do you want to try again?")) {
					throw new exceptions.ExitProgram("User indicated to exit the "
							+ "program after socket opening failure.");
				}
			}
		}
		return success;
	}
	
	public void setupOwnAddress() {
		try {
			this.ownAddress = NetworkLayer.getOwnAddress(); // TODO replace by discover?
			this.showNamedMessage("Client listing on: " + this.ownAddress);
			this.showNamedMessage("NOTE: depending on detection method, this may NOT be the actual interface used");
		} catch (UnknownHostException e) {
			this.showNamedMessage("Could not determine own address: " + e.getLocalizedMessage());
		} 
	}
	
	public boolean setServer() { //(InetAddress serverAdress, int serverPort) {
		boolean success = false;
		
//		String serverName = "nvc4122.nedap.local";
		String serverName = "nu-pi-huub";
		
		try {
			this.serverAddress = NetworkLayer.getAdressByName(serverName); // TODO let user set server
			success = true;
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		this.serverPort = FileTransferProtocol.DEFAULT_SERVER_PORT;
		
		this.showNamedMessage("Server set to " + serverName + ", with address " +
				this.serverAddress + " and port " + this.serverPort);
		
		return success;
	}
	
	public boolean setupStartSession() { // TODO or require at startup
		this.showNamedMessage("Initiating session with server...");
		try {
			while (!this.requestSession()) { // TODO clear?
				TUI.getBoolean("Try again?");
			}
		} catch (IOException | PacketException | UtilDatagramException e) {
			// TODO Auto-generated catch block
			TUI.showError("Something went wrong wile starting the session: " + e.getLocalizedMessage());
		}
		
		return this.sessionActive;
	}
	
	
	// ------------------ Client Methods --------------------------

	public void clientRunning() {
		while (this.running) {
			String userInputString = TUI.getString("Please input:");
			this.processUserInput(userInputString);
		}
	}
	
	public void processUserInput(String input) {
		String[] request = this.getArguments(input); 
		String command = request[0]; //.charAt(0); // TODO or String?
		// TODO check string being null, to prevent nullpointer exception?!

		try {
			switch (command) {
				case TUICommands.START_SESSION:
					if (!this.sessionActive) {
						this.showNamedMessage("Initiating session with server...");
						while (!this.requestSession()) { // TODO clear?
							TUI.getBoolean("Try again?");
						}
					} else {
						this.showNamedMessage("Session is already active");
					}
					break;	

				case TUICommands.LIST_FILES: 
					this.showNamedMessage("Requesting list of files...");
					if (!this.requestListFiles()) { // TODO clear?
						this.showNamedError("Retrieving list of files failed");
					}
					break;

				case TUICommands.DOWNLOAD_SINGLE:
					File fileToDownload = this.selectServerFile();

					if (!this.downloadSingleFile(fileToDownload)) { // TODO clear?
						this.showNamedError("Downloading file failed");
					}
					break;
					
				case TUICommands.UPLOAD_SINGLE:
					File fileToUpload = this.selectLocalFile();

					if (!this.uploadSingleFile(fileToUpload)) { // TODO clear?
						this.showNamedError("Uploading file failed");
					}
					break;
					
				case TUICommands.DELETE_SINGLE:
					File fileToDelete= this.selectServerFile();

					if (!this.deleteSingleFile(fileToDelete)) { // TODO clear?
						this.showNamedError("Deleting file failed");
					}
					break;

				case TUICommands.EXIT:
					// do something
					this.shutdown();
					break;
					
				case TUICommands.CHECK_INTEGRITY:
					File fileToCheck = this.selectLocalFile();

					if (!this.checkFile(fileToCheck)) { // TODO clear?
						this.showNamedError("Checking file failed");
					}
					break;

				default:
					this.showNamedError("Unknow command received"); // what TODO with it?
			}
		} catch (IOException | PacketException | UtilDatagramException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.showNamedMessage("... done!");
	}
	
	public String[] getArguments(String requestString) {
		String[] split = requestString.split(FileTransferProtocol.DELIMITER);
		return split;
	}
	
	public boolean requestSession() throws IOException, PacketException, UtilDatagramException {
		try {
			Packet responsePacket = this.requestServer(FileTransferProtocol.INIT_SESSION);
			String[] responseSplit = this.getArguments(responsePacket.getPayloadString());

			//		if (Arrays.equals(responseBytes, FileTransferProtocol.INIT_SESSION)) {
			if (responseSplit[0].equals(FileTransferProtocol.INIT_SESSION)) {
				// TODO note: different from .equals() for strings!
				this.sessionActive = true;
				//			this.serverPort = receivedPacket.getSourcePort(); // update to clientHandler
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
		}

		return this.sessionActive;
	}
	
	public boolean requestListFiles() throws IOException, PacketException, UtilDatagramException {
		boolean succes = false;

		try {
			File[] fileArray = null; 

			Packet responsePacket = this.requestServer(FileTransferProtocol.LIST_FILES);

			byte[] responseBytes = responsePacket.getPayloadBytes();
			this.showNamedMessage("Server response received, now processing..."); // TODO or also in requestServer?


			fileArray = util.Bytes.deserialiseByteArrayToFileArray(responseBytes);

			this.serverFiles = fileArray;
			this.showNamedMessage("LIST OF FILES: \n" + Arrays.toString(fileArray)); // TODO make nice UI
			succes = true;

		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (EmptyResponseException e) {
			this.showNamedError("Response from server was empty: " + e.getLocalizedMessage());
		}

		return succes;
	}
	
	/**
	 * TODO
	 * @param fileToDownload TODO: NOTE this is file as on server, not as to write on client!
	 * @return
	 */
	public boolean downloadSingleFile(File fileToDownload) {
		boolean succes = false;
		this.showNamedMessage("WARNING: overwriting existing files!"); // TODO
		try {
			// create downloadHandler
			File fileToWrite = new File(this.fileStorage.toString() +
					File.separator + fileToDownload.getName());
//			int fileSizeToDownload = (int) fileToDownload.length(); // TODO will return zero at this local filesystem! TODO casting long to int!
			DatagramSocket downloadSocket = TransportLayer.openNewDatagramSocket();
			
			DownloadHelper downloadHelper = new DownloadHelper(this,
					downloadSocket, this.serverAddress, -2, -1, fileToWrite); // TODO unset port uploader and fileSize
			// TODO uploaderPort still to set
			this.downloads.add(downloadHelper);

			// TODO request file, provide downloaderHelper port
			String singleFileRequest = FileTransferProtocol.DOWNLOAD + 
					FileTransferProtocol.DELIMITER + 
					downloadSocket.getLocalPort();
			
			byte[] fileToDownloadBytes = util.Bytes.serialiseObjectToByteArray(fileToDownload); 
			
			Packet responsePacket = this.requestServer(singleFileRequest, fileToDownloadBytes);
			String[] responseSplit = this.getArguments(responsePacket.getPayloadString());
			
			if (responseSplit[0].contentEquals(FileTransferProtocol.UPLOAD)) {
				downloadHelper.setUploaderPort(Integer.parseInt(responseSplit[1])); 
				// TODO keep protocol in mind!
				this.showNamedMessage("Uploader is on server port = " + Integer.parseInt(responseSplit[1])); //TODO efficiency
				downloadHelper.setTotalFileSize(Integer.parseInt(responseSplit[2])); 
				// TODO keep protocol in mind!
				this.showNamedMessage("Uploader reports total file size = " + Integer.parseInt(responseSplit[2])+ " bytes"); //TODO efficiency
				
				
				// TODO now everything is known: start download helper
				new Thread(downloadHelper).start();
				
				succes = true;
			} else {
				this.showNamedError("Invalid response to download request");
				succes = false;
			}

		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (EmptyResponseException e) {
			this.showNamedError("Response from server was empty: " + e.getLocalizedMessage());
		}
		
		// TODO actual downloading of file takes place in helper
//		succes = true;
				
		return succes;
	}
	
	public boolean uploadSingleFile(File fileToUpload) {
		boolean succes = false;
		
		
		this.showNamedMessage("WARNING: overwriting existing files on server!"); // TODO
		try {
			// create uploadHandler
			DatagramSocket uploadSocket = TransportLayer.openNewDatagramSocket();

			int fileSizeToUpload = (int) fileToUpload.length(); // TODO casting long to int!

			UploadHelper uploadHelper = new UploadHelper(this, uploadSocket, 
					this.serverAddress, -2, fileSizeToUpload, fileToUpload); 
			// TODO downloader port unset

			this.uploads.add(uploadHelper);

			// TODO request file, provide uploaderHelper port
			String singleFileAnnouncement = FileTransferProtocol.UPLOAD + 
					FileTransferProtocol.DELIMITER + 
					uploadSocket.getLocalPort() + 
					FileTransferProtocol.DELIMITER + 
					fileSizeToUpload; // + // TODO ask to helper/
					//FileTransferProtocol.DELIMITER.getBytes());
			
			byte[] fileToUploadBytes = util.Bytes.serialiseObjectToByteArray(fileToUpload); 
			
			Packet responsePacket = this.requestServer(singleFileAnnouncement, fileToUploadBytes);
			String[] responseSplit = this.getArguments(responsePacket.getPayloadString());
			
			if (responseSplit[0].contentEquals(FileTransferProtocol.DOWNLOAD)) {
				uploadHelper.setDownloaderPort(Integer.parseInt(responseSplit[1])); 
				// TODO keep protocol in mind!
				this.showNamedMessage("Downloader is on server port = " + Integer.parseInt(responseSplit[1])); //TODO efficiency
				// TODO keep protocol in mind!
				
				// TODO now everything is known: start download helper
				new Thread(uploadHelper).start();
				
				succes = true;
			} else {
				this.showNamedError("Invalid response to upload announcement");
				succes = false;
			}

		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (EmptyResponseException e) {
			this.showNamedError("Response from server was empty: " + e.getLocalizedMessage());
		}
		
		// TODO actual uploading of file takes place in helper

				
		return succes;
	}
	
	public boolean deleteSingleFile(File fileToUpload) {
		boolean succes = false;
		
		this.showNamedMessage("WARNING: this deletes files on server!"); // TODO
		try {
			String singleFileDeleteRequest = FileTransferProtocol.DELETE;
			
			byte[] fileToDeleteBytes = util.Bytes.serialiseObjectToByteArray(fileToUpload); 
			
			Packet responsePacket = this.requestServer(singleFileDeleteRequest, fileToDeleteBytes);
			String[] responseSplit = this.getArguments(responsePacket.getPayloadString());
			
			if (responseSplit[0].contentEquals(FileTransferProtocol.DELETE)) {
				this.showNamedMessage("File deleted!"); //TODO print more?
				// TODO keep protocol in mind!
				succes = true;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			this.showNamedError("Something went wrong while serialising file object");
		} catch (EmptyResponseException e) {
			this.showNamedError("Response from server was empty: " + e.getLocalizedMessage());
		}
		
		return succes;
	}
	
	public boolean checkFile(File fileToCheck) {
		boolean succes = false;

		try {
			boolean sameHash = this.compareLocalRemoteHash(fileToCheck);

			if(sameHash) {
				this.showNamedMessage("Local and remote files have the same hash: INTEGRITY OK");
			} else {
				this.showNamedMessage("Local and remote files have the different hash: INTEGRITY FAILED");
			}

			succes = true;

		} catch (NoMatchingFileException e) {
			this.showNamedError("Something went wrong while comparing the files: " + e.getLocalizedMessage());
		} catch (EmptyResponseException e) {
			this.showNamedError("Response from server was empty: " + e.getLocalizedMessage());
		}

		return succes;
	}

	/**
	 * TODO
	 * @param fileToCompare
	 * @return
	 * @throws NoMatchingFileException 
	 * @throws EmptyResponseException 
	 */
	public boolean compareLocalRemoteHash(File fileToCompare) throws NoMatchingFileException, EmptyResponseException {
		boolean sameHash = false; // TODO defult to false??
		String fileName = fileToCompare.getName(); // TODO search on path plus name?

		try {

			String localHash = util.FileOperations.getHashHexString(fileToCompare);

			String remoteHash = null;

			this.showNamedMessage("Hash of local file: " + localHash);

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
					// TODO keep protocol in mind!
				}

				if (remoteHash.equals(localHash)) {
					sameHash = true;
				} else {
					sameHash = false;
				}

			} else {
				this.showNamedError("Matched file on server is null!"); // TODO something?
				throw new NoMatchingFileException("Matched file on server is null!");
			}

		} catch (NoSuchAlgorithmException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoMatchingFileException e) {
			this.showNamedError("No matching file on the server to check!"); // TODO also print? 
			throw new NoMatchingFileException("No matching file found on the server!");
		}

		return sameHash;
	}
	
	
	public File[] getLocalFiles() {
		File[] filesArray = new File(this.fileStorage.toString()).listFiles( // TODO only non-hidden
				new FileFilter() {
					@Override
					public boolean accept(File file) {
						return !file.isHidden();
					}
				});

		System.out.println(Arrays.toString(filesArray));
		return filesArray;
	}
	
	public File selectLocalFile() {
		File[] localFiles = this.getLocalFiles();
		
		int selectedIndex = -1;
		while (!(selectedIndex >= 0 && selectedIndex < localFiles.length)) {
			selectedIndex = TUI.getInt("Which index to select?"); 
		}
		return localFiles[selectedIndex];
	}
	
	public File selectServerFile() {
		int selectedIndex = -1;
		try {
			this.requestListFiles();

			while (!(selectedIndex >= 0 && selectedIndex < this.serverFiles.length)) {
				selectedIndex = TUI.getInt("Which index to select?"); 
			}

		} catch (IOException | PacketException | UtilDatagramException e) {
			this.showNamedMessage("Something went wrong while refreshing"
					+ " the server file list: " + e.getLocalizedMessage());
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return this.serverFiles[selectedIndex];
	}
	
	public File matchToServerFile(String fileName) throws NoMatchingFileException { // TODO match on other then file name?
		File fileOnServer = null;
		
		try {
			this.showNamedMessage("Matching local file to file on server...");
			this.requestListFiles();

		} catch (IOException | PacketException | UtilDatagramException e) {
			this.showNamedMessage("Something went wrong while communicating with"
					+ " the server file list: " + e.getLocalizedMessage());
			// TODO Auto-generated catch block
			e.printStackTrace();
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
			this.showNamedMessage("Found matches: \n" + Arrays.toString(matchesOnServer.toArray())); // TODO make nice UI

			int selectedIndex = -1;
			while (!(selectedIndex >= 0 && selectedIndex < this.serverFiles.length)) {
				selectedIndex = TUI.getInt("Which index is the correct match?"); 
			}
			fileOnServer = matchesOnServer.get(selectedIndex);
		}
		return fileOnServer;		
	}
	
	/**
	 * TODO
	 * @param requestString
	 * @param requestBytes
	 * @return
	 * @throws EmptyResponseException 
	 */
	public Packet requestServer(String requestString, byte[] requestBytes) throws EmptyResponseException {
		byte[] requestStringBytes = requestString.getBytes();
		
		byte[] bytesToServer = requestStringBytes;

		if (requestBytes != null) {
			bytesToServer = util.Bytes.concatArray(requestStringBytes, requestBytes);
		}
		this.sendBytesToServer(bytesToServer, requestStringBytes.length);

		// TODO now wait for response, 
		Packet receivedPacket = this.receiveServerResponse(); // TODO handle null gracefully

		if (receivedPacket != null) {
			return receivedPacket; 
		} else {
			throw new EmptyResponseException("Response from server was null");
		}
	}

	public void sendBytesToServer(byte[] bytesToSend, int byteOffset) { // TODO put in seperate utility?
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
			
			this.showNamedMessage("Bytes send to server!");
			
		} catch (UnknownHostException | PacketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UtilByteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UtilDatagramException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * TODO
	 * @param requestString
	 * @return
	 * @throws EmptyResponseException
	 */
	public Packet requestServer(String requestString) throws EmptyResponseException {
		return this.requestServer(requestString, null);
	}
	
	/**
	 * TODO
	 * @return
	 */
	public Packet receiveServerResponse() {
		this.showNamedMessage("Waiting for server response...");

		Packet receivedPacket = null;
		try {
			//if the socket does not receive anything in 1 second, 
			//it will timeout and throw a SocketTimeoutException
			//you can catch the exception if you need to log, or you can ignore it
			this.socket.setSoTimeout(10000); // TODO explain  and TODO set it permanently? in setup? 

			receivedPacket = TransportLayer.receivePacket(this.socket);

			if (!(receivedPacket.getSourceAddress().equals(this.serverAddress)
					&& receivedPacket.getSourcePort() == this.serverPort)) { 
				this.showNamedError("SECURITY WARNING: this response is NOT"
						+ " coming for known server > dropping it");
				return null;
			}

		} catch (SocketTimeoutException e) { // on TimeOut
			this.showNamedError("Receiving a server response timed out"); // TODO retry?
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
	 * Shutdown server TODO (try with resources?!)
	 */
	public void shutdown() {
		this.showNamedMessage("See you later!");
		this.running = false;

		// see example on github? 
		this.socket.close(); // TODO make a method for this, ensure!

	}
	
	public String getName() {
		return name;
	}
	
	/**
	 * TODO cannot override from TUI?
	 * @param message
	 */
	public void showNamedMessage(String message) {
		TUI.showNamedMessage(this.name, message);
	}
	
	/**
	 * TODO cannot override from TUI?
	 * @param message
	 */
	public void showNamedError(String message) {
		TUI.showNamedError(this.name, message);
	}
	
	// ------------------ Main --------------------------

	public static void main(String[] args) {
		System.out.println("Welcome to the FileTransfer Client! \n Starting...");
		
		int port; // TODO duplicate name: change? 
		
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
		//new Thread(server).start();
	}

}
