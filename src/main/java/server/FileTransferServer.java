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

	/** The name of this server. */
	private static String SERVERNAME = null; // final

	/** Network info for server. */
	InetAddress ownAddress = null;
	int ownPort = 0;
	
	/**
	 * TODO
	 */
	Path root;
	Path fileStorage;
	String fileStorageDirName;

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
		TUI.showMessage("Setting up the server...");
		boolean success = false;
		
		// First, initialise the Server.
		// SERVERNAME = TUI.getString("What is the name of this server?"); // TODO name? 

		boolean successFileSystem = this.setupFileSystem();
		boolean succesSocket = this.setupSocket();
		this.setupOwnAddress();
		
		success = successFileSystem && succesSocket;
		
		if (success) {
			TUI.showMessage("Setup complete!");
		}
		
		return success;
	}
	
	public boolean setupFileSystem() {
		boolean success = false;
		this.root = Paths.get("").toAbsolutePath(); // TODO suitable method? https://www.baeldung.com/java-current-directory
		TUI.showMessage("Server root path set to: " + this.root.toString());
		
		this.fileStorage = root.resolve(fileStorageDirName);
		TUI.showMessage("File storage set to: " + this.fileStorage.toString());

		
//		if (!Files.exists(fileStorage)) { // TODO: use if or catch exception
            try {
				Files.createDirectory(fileStorage);
		    	TUI.showMessage("File storage directory did not exist: created " + fileStorageDirName + " in server root"); 
            } catch(java.nio.file.FileAlreadyExistsException eExist) {
            	TUI.showMessage("File storage directory already exist: not doing anything with " + fileStorageDirName + " in server root");
            } catch (IOException e) {
				// TODO Auto-generated catch block
				TUI.showError("Failed to create file storage: server CRASHED because " + e.getLocalizedMessage());
				e.printStackTrace();
			}
//        } else {
//	    	TUI.showMessage("File storage directory already exist: not doing anything with " + fileStorageDirName + " in server root"); 
//        }
            success = true;
    		return success;
	}

	public boolean setupSocket() throws ExitProgram {
		boolean success = false;
		TUI.showMessage("Trying to open a new socket...");
		while (this.socket == null) { // TODO: ask for server port?
			//port = TUI.getInt("Please enter the server port.");

			try {
				this.socket = TransportLayer.openNewDatagramSocket(this.ownPort);
				TUI.showMessage("Server now bound to port " + ownPort);
				success = true;
			} catch (SocketException e) {
				TUI.showMessage("Something went wrong when opening the socket: "
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
			TUI.showMessage("Server listing on: " + this.ownAddress);
			TUI.showMessage("NOTE: depending on detection method, this may NOT be the actual interface used");
		} catch (UnknownHostException e) {
			TUI.showMessage("Could not determine own address: " + e.getLocalizedMessage());
		} 
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
				TUI.showMessage("Waiting for client...");

				Packet receivedPacket = TransportLayer.receivePacket(this.socket);

				System.out.println(Arrays.toString(receivedPacket.getPayload()));
				System.out.println(Arrays.toString(FileTransferProtocol.INIT_SESSION));

				if (Arrays.equals(receivedPacket.getPayload() , (FileTransferProtocol.INIT_SESSION))) { 
					// TODO note: different from .equals() for strings!
					this.handleSessionRequest(receivedPacket);
				} else {
					TUI.showError("Unknown packet: dropping");
					TUI.showError("Content was: : " + receivedPacket.getPayloadAsString()
							+ " (in bytes: " + Arrays.toString(receivedPacket.getPayload()) + ")");
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
	public void handleSessionRequest(Packet sessionInitPacket) {
		InetAddress clientAddres = sessionInitPacket.getSourceAddress();

		String name = "Client " 
				+ String.format("%02d", next_client_no++);
		TUI.showMessage("A new client " + name + "  is trying to connect from "
				+ clientAddres + "...");

		try {
			DatagramSocket sessionSocket = TransportLayer.openNewDatagramSocket(); 
			FileTransferClientHandler handler = new FileTransferClientHandler(sessionSocket,
					sessionInitPacket, this); //TODO (sock, this, name);

			new Thread(handler).start();
			clients.add(handler);
			
			int sessionPortNumber = handler.getPort();
			byte[] initResponse = util.Bytes.concatArray(FileTransferProtocol.INIT_SESSION,
					(FileTransferProtocol.DELIMITER + sessionPortNumber).getBytes());
			this.sendBytesToClient(initResponse, sessionInitPacket.getSourceAddress(), sessionInitPacket.getSourcePort());

			TUI.showMessage("New client [" + name + "] connected, on port " 
					+ handler.getPort() + " !"); 

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		TUI.showMessage("See you later!");

		// see example on github? 
		this.socket.close(); // TODO make a method for this, ensure!

	}
	
	/**
	 * Returns the name of the server.
	 * 
	 * @requires SERVERNAME != null;
	 * @return the name of the sever.
	 */
	public String getServerName() {
		return SERVERNAME;
	}
	
	/**
	 * TODO return fileStorage path of this server
	 * TODO maybe create substorage per client?
	 */
	public Path getFileStorage(String clientName) {
		Path clientFileStorage;

		if (clientName.equals("all")) {
			clientFileStorage = this.fileStorage;
		} else { 
			//		clientFileStorage = clientStorage // TODO implement as key-value pairs
			clientFileStorage = null;
		}

		return clientFileStorage;
	}
	
	public void sendBytesToClient(byte[] bytesToSend, InetAddress clientAddress, int clientPort) { // TODO put in separate utility?
		try { // to construct and send a packet
			Packet packet = new Packet(
						0, // TODO id
						this.ownAddress,
						this.ownPort, 
						clientAddress, 
						clientPort,
						bytesToSend
				);
			
			TransportLayer.sendPacket(
					this.socket,
					packet,
					clientPort
			); 
			
			TUI.showMessage("Bytes send!");
			
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

