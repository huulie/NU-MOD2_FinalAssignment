package server;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import exceptions.ExitProgram;
import exceptions.PacketException;
import exceptions.ServerFailureException;
import exceptions.UtilByteException;
import exceptions.UtilDatagramException;
import network.NetworkLayer;
import network.Packet;
import network.TransportLayer;
import protocol.FileTransferProtocol;

/**
 * Server to interact with FileTransfer clients, and start new clientHandlers.
 * @author huub.lievestro
 *
 */
public class FileTransferServer implements Runnable { 

	/** 
	 * The socket of this FileTranferServer.
	 */
	private DatagramSocket socket;

	/** 
	 * List of FileTransferClientHandlers, one for each connected client. 
	 */
	private List<FileTransferClientHandler> clients;

	/** 
	 * Next client number, increasing for every new connection. 
	 */
	private int nextClientNr;

	/** 
	 * The TUI of this FileTransferServer. 
	 */
	private userInterface.TUI textUI; 

	/** 
	 * Network address of the server. 
	 */
	InetAddress ownAddress = null;
	
	/** 
	 * Network port of the server. 
	 */
	int ownPort = 0;
	
	/*
	 * Name of the server
	 */
	private String name;
	
	/** 
	 * Local filesystem root.
	 */
	Path root;
	
	/**
	 * Local filesystem storage location of this server.
	 */
	Path fileStorage;
	
	/**
	 * Name of the local storage location of this server.
	 */
	String fileStorageDirName;

	/**
	 * Sandboxing = estrict access of client to only their own storage.
	 */
	boolean sandboxClients;
	
	/**
	 * Map of sandbox storages: <String clientName, Path sandboxPath>. 
	 */
	HashMap<String, Path> sandboxStorages;
	
	/**
	 * Name which will give access to server file storage.
	 */
	String adminName;

	/**
	 * Construct a new FileTransfer server.
	 * @param port to bind socket to
	 */
	public FileTransferServer(int port) {
		this.ownPort = port;
	
		this.clients = new ArrayList<>();
		this.nextClientNr = 1;

		this.textUI = new userInterface.TUI();
		name = "FTServer"; // fallback if setup doesn't set it
		
		this.fileStorageDirName = "FTSstorage";

		// Do setup
		boolean setupSucces = false;
		while (!setupSucces) {
			try {
				setupSucces = this.setup();
			} catch (exceptions.ExitProgram eExit) {
				// If setup() throws an ExitProgram exception, stop the program.
				if (!textUI.getBoolean("Do you want to retry setup?")) {
					this.shutdown(); 
				}
			}
		}
	}
	
	
	// ------------------ Server Setup --------------------------
	/**
	 * Sets up a new FileTransferServer.
	 * @return boolean indicating if succeeded
	 * @throws ExitProgram if the user decides to exit the program.
	 */
	public boolean setup() throws exceptions.ExitProgram {
		this.showNamedMessage("Setting up the server...");
		boolean success = false;

		boolean successFileSystem = this.setupFileSystem();
		boolean succesSocket = this.setupSocket();
		boolean succesNetwork = this.setupOwnAddress();
		
		this.setupTimeoutThread();
		
		success = successFileSystem && succesSocket && succesNetwork;
		
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
		this.showNamedMessage("Server root path set to: " + this.root.toString());

		this.fileStorage = root.resolve(fileStorageDirName);
		this.showNamedMessage("File storage set to: " + this.fileStorage.toString());

		try {
			Files.createDirectory(fileStorage);
			this.showNamedMessage("File storage directory did not exist:"
					+ " created " + fileStorageDirName + " in server root"); 
		} catch (java.nio.file.FileAlreadyExistsException eExist) {
			this.showNamedMessage("File storage directory already exist: not doing anything with "
					+ fileStorageDirName + " in server root");
			return success;
		} catch (IOException e) {
			this.showNamedError("Failed to create file storage because: " 
					+ e.getLocalizedMessage());
			return success;
		}

		this.sandboxClients = true; 
		//PM: getting cmd input on pi returns nullpointers, 
		//		could not use: this.TUI.getBoolean("Do you want to sandbox clients?");
		this.adminName = "admin";
		this.showNamedMessage("Admin is: " + this.adminName);
		this.sandboxStorages = new HashMap<String, Path>(); 

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
				this.showNamedMessage("Server now bound to port " + ownPort);
				success = true;
			} catch (SocketException e) {
				this.showNamedMessage("Something went wrong when opening the socket: "
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
	 * Sets up the server network information.
	 * @return boolean indicating if succeeded
	 */
	public boolean setupOwnAddress() {
		boolean success = false;
		try {
			this.ownAddress = NetworkLayer.getOwnAddress(); // TODO replace by discover?
			this.showNamedMessage("Server listing on: " + this.ownAddress);
			this.showNamedMessage("NOTE: depending on detection method,"
					+ " this may NOT be the actual interface used");
			this.showNamedMessage("Discovered preferred local address: " 
					+ NetworkLayer.discoverLocalAddress());
			
			this.name = this.ownAddress.getHostName();
			success = true;
		} catch (UnknownHostException e) {
			this.showNamedMessage("Could not determine own address: " + e.getLocalizedMessage());
		} 
		
		return success;
	}
	
	/**
	 * Start helper thread for time outs.
	 */
	public void setupTimeoutThread() {
		util.TimeOut.start();
		this.showNamedMessage("TimeOut helper started...");
	}
	
	
	// ------------------ Server Methods --------------------------
	
	/**
	 * Starts a new FileTransferHandler for every connecting client.
	 */
	public void run() {
		try {
			while (true) {
				this.showNamedMessage("Waiting for client...");

				Packet receivedPacket = TransportLayer.receivePacket(this.socket);

				if (receivedPacket.getPayloadString().startsWith(FileTransferProtocol.DISCOVER)) {
					this.handleDiscover(receivedPacket);
				} else if (receivedPacket.getPayloadString()
						.startsWith(FileTransferProtocol.INIT_SESSION)) {
					this.handleSessionRequest(receivedPacket);
				} else {
					this.showNamedError("Unknown packet: dropping");
					this.showNamedError("Content was: : " + receivedPacket.getPayloadString()
							+ " (in bytes: " + Arrays.toString(receivedPacket.getPayload()) + ")");
				}
			}
		
			
		} catch (IOException | PacketException | UtilDatagramException e) {
			this.showNamedError("Something went wrong: " + e.getLocalizedMessage());
			if (textUI.getBoolean("Do you want to quit?")) {
				this.shutdown();
			}
		} 
	}

	/**
	 * Respond to a DISCOVER send by a client.
	 */
	public void handleDiscover(Packet discoverPacket) {
		InetAddress clientAddress = discoverPacket.getSourceAddress();
		
		this.showNamedMessage("DISCOVER packet received from " + clientAddress + " >> responding");
		byte[] discoverResponse = (FileTransferProtocol.DISCOVER +
				FileTransferProtocol.DELIMITER + this.name).getBytes();
		this.sendBytesToClient(discoverResponse,
				discoverPacket.getSourceAddress(),
				discoverPacket.getSourcePort(),
				discoverResponse.length);
		
	}
	
	/**
	 * Respond to a INIT_SESSION send by a client, and start a new clientHandler.
	 */
	public void handleSessionRequest(Packet sessionInitPacket) {
		InetAddress clientAddress = sessionInitPacket.getSourceAddress();
		String[] sessionRequest = sessionInitPacket.getPayloadString()
				.split(FileTransferProtocol.DELIMITER);
		String clientName;
		
		if (sessionRequest[1] != null && !sessionRequest[1].isBlank()) {
			clientName = sessionRequest[1];
		} else {
			clientName = "Client " + String.format("%02d", nextClientNr++);
		}
		this.showNamedMessage("A new client [" + clientName + "] (" + nextClientNr 
				+  ") is trying to connect from " + clientAddress + "...");

		if (this.sandboxClients && !clientName.equals(this.adminName)) {
			this.showNamedMessage("Assinging sandbox to this client...");
			try {
				this.assignClientSandbox(clientName);
			} catch (ServerFailureException e) {
				byte[] failure = (FileTransferProtocol.FAILED + FileTransferProtocol.DELIMITER 
						+ "Sandboxing failed: " + e.getLocalizedMessage()).getBytes();
				this.sendBytesToClient(failure,
						clientAddress, sessionInitPacket.getSourcePort(), failure.length); 
				this.showNamedError("Client notified of failure: " + e.getLocalizedMessage());
			}
		}

		try {
			// create new clientHandler and start it
			DatagramSocket sessionSocket = TransportLayer.openNewDatagramSocket(); 
			FileTransferClientHandler handler = new FileTransferClientHandler(sessionSocket,
					sessionInitPacket, this, clientName);
			new Thread(handler).start();
			clients.add(handler);

			// respond to user
			int sessionPortNumber = handler.getPort();
			byte[] initResponse = (FileTransferProtocol.INIT_SESSION +
					FileTransferProtocol.DELIMITER + sessionPortNumber).getBytes();
			this.sendBytesToClient(initResponse,
					sessionInitPacket.getSourceAddress(),
					sessionInitPacket.getSourcePort(),
					initResponse.length);

			this.showNamedMessage("New client [" + clientName + "] connected, on port " 
					+ handler.getPort() + " !"); 

		} catch (IOException e) {
			byte[] failure = (FileTransferProtocol.FAILED + FileTransferProtocol.DELIMITER 
					+ "Handeling new session failed: " + e.getMessage()).getBytes();
			this.sendBytesToClient(failure,
					sessionInitPacket.getSourceAddress(),
					sessionInitPacket.getSourcePort(),
					failure.length); 
		}
	}


	/**
	 * Removes a clientHandler from the client list,
	 *  e.g. after a certain silent time.
	 * @requires client != null
	 */
	public void removeClient(FileTransferClientHandler client) {
		this.clients.remove(client);
	}
	
	/**
	 * Shutdown server.
	 */
	public void shutdown() {
		this.showNamedMessage("See you later!");
		this.socket.close(); 
		util.TimeOut.stop();
		this.showNamedMessage("TimeOut helper stopped.");

	}
	
	/**
	 * Returns the name of this server.
	 * 
	 * @requires this.name != null;
	 * @return the name of the sever.
	 */
	public String getServerName() {
		return this.name;
	}
	
	/**
	 * Lookup the fileStorage path of a user on this server.
	 * @return Path to file storage of client
	 */
	public Path getFileStorage(String clientName) {
		Path clientFileStorage;

		if (clientName.equals(this.adminName) || !this.sandboxClients) {
			this.showNamedMessage("Client [" + clientName + "]=> "
					+ "Not sandboxing or admin detected: access to server storage granted");
			clientFileStorage = this.fileStorage;
		} else { 
			this.showNamedMessage("Client [" + clientName + "]=> "
					+ "Sandboxing: access to server fileStorage denied");
			clientFileStorage = this.sandboxStorages.get(clientName);
		}

		return clientFileStorage;
	}
	
	/**
	 * Send bytes to the client, contained in a Packet.
	 * @param bytesToSend to server
	 * @param clientAddress to send to
	 * @param clientPort to send to
	 * @param byteOffset due to string part
	 */
	public void sendBytesToClient(byte[] bytesToSend,
			InetAddress clientAddress, int clientPort, int byteOffset) {
		try { // to construct and send a packet
			Packet packet = new Packet(
						0,
						this.ownAddress,
						this.ownPort, 
						clientAddress, 
						clientPort,
						bytesToSend,
						byteOffset
				);
			
			TransportLayer.sendPacket(
					this.socket,
					packet,
					clientPort
			); 
			
			// this.showNamedMessage("Bytes send!");  // for debugging
			
		} catch (PacketException | IOException | UtilByteException | UtilDatagramException e) {
			this.showNamedError("Something went wrong while sending bytes: "
					+ e.getLocalizedMessage());
		}
	}
	
	/**
	 * Create a separate storage for this individual client.
	 * @param name of the client
	 * @return Path to file storage of this client
	 * @throws ServerFailureException 
	 */
	public Path assignClientSandbox(String clientName) throws ServerFailureException {
		this.showNamedMessage("Server file storage set to: " + this.fileStorage.toString());

		Path sandboxedStorage = this.fileStorage.resolve(clientName);
		this.showNamedMessage("Client File storage set to: " + sandboxedStorage.toString());

		try {
			Files.createDirectory(sandboxedStorage);
			this.showNamedMessage("File storage directory did not exist:"
					+ " created " + sandboxedStorage + " in server file storage"); 
		} catch (java.nio.file.FileAlreadyExistsException eExist) {
			this.showNamedMessage("File storage directory already exist:"
					+ " not doing anything with " + sandboxedStorage + " in server file storage");
		} catch (IOException e) {
			this.showNamedError("Failed to create file storage because: " 
					+ e.getLocalizedMessage());
			throw new ServerFailureException("Failed to create file storage because: " 
					+ e.getLocalizedMessage());
		}

		
		if (this.sandboxStorages.containsKey(clientName)) {
			this.sandboxStorages.replace(clientName, sandboxedStorage);
			this.showNamedMessage("Client storage for " + clientName 
					+ "reset to " + sandboxedStorage.toString());
			this.showNamedError("Note: access to previously assigned storage(s) is now replaced!");
		} else {
			this.sandboxStorages.put(clientName, sandboxedStorage);
			this.showNamedMessage("Client storage for " + clientName 
					+ "set to " + sandboxedStorage.toString());
		}
		return sandboxedStorage;
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
	 * Use this main method to boot a new FileTransfer server.
	 * @param args
	 */
	public static void main(String[] args) {

		System.out.println("Welcome to the FileTransfer Server! \n Starting...");
		
		int port;
		
		if (args.length < 0 || args.length > 1) {
			System.out.println("Syntax: FileTranferServer <port>");
			return;
		} else if (args.length == 1) {
			port = Integer.parseInt(args[0]);
			System.out.println("Using specified server port " + port + "...");
		} else {
			port = FileTransferProtocol.DEFAULT_SERVER_PORT;
			System.out.println("Using default server port " + port + "...");
		}
			
		FileTransferServer server =  new FileTransferServer(port);
		System.out.println("Starting server...");
		new Thread(server).start();
	}

}

