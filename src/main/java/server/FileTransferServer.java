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
import exceptions.UtilByteException;
import exceptions.UtilDatagramException;
import network.NetworkLayer;
import network.Packet;
import network.TransportLayer;
import protocol.FileTransferProtocol;

public class FileTransferServer implements Runnable { 

	/** The socket of this FileTranferServer. */
	private DatagramSocket socket;

	/** List of FileTransferClientHandler, one for each connected client. */
	private List<FileTransferClientHandler> clients;

	/** List of GoClientHandlers, waiting to be connected to a port. */
	// TODO: implement with max_concurrent_clients?
	private List<FileTransferClientHandler> clientsWaitingList;

	/** Next client number, increasing for every new connection. */
	private int next_client_no;

	/** The TUI of this FileTransferServer. */
	private UI.TUI TUI; 

	/** Network info for server. */
	InetAddress ownAddress = null;
	int ownPort = 0;
	
	/*
	 * TODO
	 */
	private String name;
	
	/**
	 * TODO
	 */
	Path root;
	Path fileStorage;
	String fileStorageDirName;

	/**
	 * TODO restrict access of client to only their own storage
	 */
	boolean sandboxClients;
	
	/**
	 * Map of <String clientName, Path sandboxPath>. 
	 */
	HashMap<String,Path> sandboxStorages;
	
	/**
	 * Name which will give acces to server file storage
	 */
	String adminName;
	
	// TODO: same as example, because interrupt thread? 	
	boolean running;

	/**
	 * Construct a new FileTransfer server.
	 * @param socket
	 * @param port
	 */
	public FileTransferServer(int port) {
		// Initialise instance variables:
		this.ownPort = port;
		this.running = true;

		this.clients = new ArrayList<>();
		this.next_client_no = 1;
		this.clientsWaitingList = new ArrayList<>();

		this.TUI = new UI.TUI();
		name = "FTServer"; // TODO fixed name, fallback if setup doesn't set it
		
		this.fileStorageDirName = "FTSstorage";
		
		

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
	}
	
	// ------------------ Server Setup --------------------------
	/**
	 * Sets up a new FileTransferServer TODO
	 * 
	 * @throws ExitProgram if a connection can not be created on the given 
	 *                     port and the user decides to exit the program.
	 * @ensures a serverSocket is opened.
	 */
	public boolean setup() throws exceptions.ExitProgram {
		this.showNamedMessage("Setting up the server...");
		boolean success = false;
		
		// First, initialise the Server.
		// SERVERNAME = TUI.getString("What is the name of this server?"); // TODO name? 

		boolean successFileSystem = this.setupFileSystem();
		boolean succesSocket = this.setupSocket();
		this.setupOwnAddress();
		
		this.setupTimeoutThread();
		
		success = successFileSystem && succesSocket;
		
		if (success) {
			this.showNamedMessage("Setup complete!");
		}
		
		return success;
	}
	
	public boolean setupFileSystem() {
		boolean success = false;
		this.root = Paths.get("").toAbsolutePath(); // TODO suitable method? https://www.baeldung.com/java-current-directory
		this.showNamedMessage("Server root path set to: " + this.root.toString());

		this.fileStorage = root.resolve(fileStorageDirName);
		this.showNamedMessage("File storage set to: " + this.fileStorage.toString());


		//		if (!Files.exists(fileStorage)) { // TODO: use if or catch exception
		try {
			Files.createDirectory(fileStorage);
			this.showNamedMessage("File storage directory did not exist: created " + fileStorageDirName + " in server root"); 
		} catch(java.nio.file.FileAlreadyExistsException eExist) {
			this.showNamedMessage("File storage directory already exist: not doing anything with " + fileStorageDirName + " in server root");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			this.showNamedError("Failed to create file storage: server CRASHED because " + e.getLocalizedMessage());
			e.printStackTrace();
		}
		//        } else {
		//	    	this.showNamedMessage("File storage directory already exist: not doing anything with " + fileStorageDirName + " in server root"); 
		//        }

		this.sandboxClients = true; // TODO input on pi returns nullpointer? this.TUI.getBoolean("Do you want to sandbox clients?");
		this.adminName = "admin";
		this.showNamedMessage("Admin is: " + this.adminName);
		this.sandboxStorages = new HashMap<String, Path>(); 

		success = true; // TODO significant?
		return success;
	}

	public boolean setupSocket() throws ExitProgram {
		boolean success = false;
		this.showNamedMessage("Trying to open a new socket...");
		while (this.socket == null) { // TODO: ask for server port?
			//port = TUI.getInt("Please enter the server port.");

			try {
				this.socket = TransportLayer.openNewDatagramSocket(this.ownPort);
				this.showNamedMessage("Server now bound to port " + ownPort);
				success = true;
			} catch (SocketException e) {
				this.showNamedMessage("Something went wrong when opening the socket: "
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
			this.showNamedMessage("Server listing on: " + this.ownAddress);
			this.showNamedMessage("NOTE: depending on detection method, this may NOT be the actual interface used");
			
			this.name = this.ownAddress.getHostName();
		} catch (UnknownHostException e) {
			this.showNamedMessage("Could not determine own address: " + e.getLocalizedMessage());
		} 
	}
	
	public void setupTimeoutThread() {
		util.TimeOut.Start();
		this.showNamedMessage("TimeOut helper started...");
	}
	
	// ------------------ Server Methods --------------------------
	
	/**
	 * starts a new FileTransferHandler for every connecting client.
	 * 
	 * If {@link #setup()} throws a ExitProgram exception, stop the program. 
	 * In case of any other errors, ask the user whether the setup should be 
	 * ran again to open a new socket.
	 */
	public void run() {
		try {
			while (true) {
				this.showNamedMessage("Waiting for client...");

				Packet receivedPacket = TransportLayer.receivePacket(this.socket);

				System.out.println("DEBUG:"); // TODO remove
				System.out.println(Arrays.toString(receivedPacket.getPayload())); // TODO payloadBytes?
				System.out.println(Arrays.toString(FileTransferProtocol.INIT_SESSION.getBytes()));

				if (receivedPacket.getPayloadString().startsWith(FileTransferProtocol.DISCOVER)) {
					this.handleDiscover(receivedPacket);
				} else if (receivedPacket.getPayloadString().startsWith(FileTransferProtocol.INIT_SESSION)) {
					this.handleSessionRequest(receivedPacket);
				} else {
					this.showNamedError("Unknown packet: dropping");
					this.showNamedError("Content was: : " + receivedPacket.getPayloadString()
							+ " (in bytes: " + Arrays.toString(receivedPacket.getPayload()) + ")"); //TODO no payloadBytes
					// TODO send unknown message back? 
				}

			}
//		} catch (exceptions.ExitProgram eExit) { // TODO how to throw? 
//			if (TUI.getBoolean("Are you sure you want to quit?")) {
//				this.shutdown();
//			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (PacketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UtilDatagramException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * TODO
	 */
	public void handleDiscover(Packet sessionInitPacket) {
		InetAddress clientAddress = sessionInitPacket.getSourceAddress();
		
		this.showNamedMessage("DISCOVER packet received from " + clientAddress + " >> responding");
		byte[] discoverResponse = (FileTransferProtocol.DISCOVER +
				FileTransferProtocol.DELIMITER + this.name).getBytes();
		this.sendBytesToClient(discoverResponse,
				sessionInitPacket.getSourceAddress(),
				sessionInitPacket.getSourcePort(),
				discoverResponse.length - 1 + 1); // TODO make this more nice + note offset is string end +1 (note length starts at 1)
		
	}
	
	/**
	 * TODO
	 */
	public void handleSessionRequest(Packet sessionInitPacket) {
		InetAddress clientAddress = sessionInitPacket.getSourceAddress();

		String[] sessionRequest = sessionInitPacket.getPayloadString().split(FileTransferProtocol.DELIMITER);
		String clientName;
		
		if (sessionRequest[1] != null && !sessionRequest[1].isBlank()) {
			clientName = sessionRequest[1];
		} else {
			clientName = "Client " 
					+ String.format("%02d", next_client_no++);
		}
		this.showNamedMessage("A new client [" + clientName 
				+ "] (" + next_client_no 
				+  ") is trying to connect from "
				+ clientAddress + "...");

		if (this.sandboxClients && !clientName.equals(this.adminName)) {
			this.showNamedMessage("Assinging sandbox to this client...");
			this.assignClientSandbox(clientName);
		}

		try {
			DatagramSocket sessionSocket = TransportLayer.openNewDatagramSocket(); 
			FileTransferClientHandler handler = new FileTransferClientHandler(sessionSocket,
					sessionInitPacket, this, clientName); //TODO (sock, this, name);

			new Thread(handler).start();
			clients.add(handler);

			int sessionPortNumber = handler.getPort();
			byte[] initResponse = (FileTransferProtocol.INIT_SESSION +
					FileTransferProtocol.DELIMITER + sessionPortNumber).getBytes();
			this.sendBytesToClient(initResponse,
					sessionInitPacket.getSourceAddress(),
					sessionInitPacket.getSourcePort(),
					initResponse.length - 1 + 1); // TODO make this more nice + note offset is string end +1 (note length starts at 1)

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
	 * Removes a clientHandler from the client list.
	 * @requires client != null
	 */
	public void removeClient(FileTransferClientHandler client) {
		this.clients.remove(client);
		if (this.clientsWaitingList.contains(client)) {
			this.clientsWaitingList.remove(client);
		}
		// TODO remove client after certain silent time
	}
	
	/**
	 * Shutdown server TODO (try with resources?!)
	 */
	public void shutdown() {
		this.showNamedMessage("See you later!");

		// see example on github? 
		this.socket.close(); // TODO make a method for this, ensure!
		
		util.TimeOut.Stop();
		this.showNamedMessage("TimeOut helper stopped.");

	}
	
	/**
	 * Returns the name of the server.
	 * 
	 * @requires SERVERNAME != null;
	 * @return the name of the sever.
	 */
	public String getServerName() {
		return this.name;
	}
	
	/**
	 * TODO return fileStorage path of this server
	 * TODO maybe create substorage per client?
	 */
	public Path getFileStorage(String clientName) {
		Path clientFileStorage;

		if (clientName.equals(this.adminName) || !this.sandboxClients) {
			this.showNamedMessage("Client [" + clientName + "]=> Not sandboxing or admin detected: access to server storage granted");
			clientFileStorage = this.fileStorage;
		} else { 
			//		clientFileStorage = clientStorage // TODO implement as key-value pairs
			this.showNamedMessage("Client [" + clientName + "]=> Sandboxing: access to server fileStorage denied");
			clientFileStorage = this.sandboxStorages.get(clientName);
		}

		return clientFileStorage;
	}
	
	public void sendBytesToClient(byte[] bytesToSend, InetAddress clientAddress, int clientPort, int byteOffset) { // TODO put in separate utility?
		try { // to construct and send a packet
			Packet packet = new Packet(
						0, // TODO id
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
			
			this.showNamedMessage("Bytes send!");
			
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
	 * Setup a separate storage for this individual client
	 * @param name
	 * @return
	 */
	public Path assignClientSandbox(String clientName) {
		this.showNamedMessage("Server file storage set to: " + this.fileStorage.toString());

		Path sandboxedStorage = this.fileStorage.resolve(clientName);
		this.showNamedMessage("Client File storage set to: " + sandboxedStorage.toString());


		//			if (!Files.exists(fileStorage)) { // TODO: use if or catch exception
		try {
			Files.createDirectory(sandboxedStorage);
			this.showNamedMessage("File storage directory did not exist: created " + sandboxedStorage + " in server file storage"); 
		} catch (java.nio.file.FileAlreadyExistsException eExist) {
			this.showNamedMessage("File storage directory already exist: not doing anything with " + sandboxedStorage + " in server file storage");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			this.showNamedError("Failed to create file storage: server CRASHED because " + e.getLocalizedMessage());
			e.printStackTrace();
		}
		//	        } else {
		//		    	this.showNamedMessage("File storage directory already exist: not doing anything with " + fileStorageDirName + " in server root"); 
		//	        }
		
		if (this.sandboxStorages.containsKey(clientName)) {
			this.sandboxStorages.replace(clientName, sandboxedStorage);
			this.showNamedMessage("Client storage for " + clientName + "reset to " + sandboxedStorage.toString());
			this.showNamedError("Note: access to previously assigned storage(s) is now replaced!");
		} else {
			this.sandboxStorages.put(clientName, sandboxedStorage);
			this.showNamedMessage("Client storage for " + clientName + "set to " + sandboxedStorage.toString());
		}
		return sandboxedStorage;
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

	/** Start a new FileTransferServer. */
	public static void main(String[] args) {

		System.out.println("Welcome to the FileTransfer Server! \n Starting...");
		
		int port; // TODO duplicate name: change? 
		
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

