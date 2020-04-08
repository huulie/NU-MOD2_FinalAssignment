package client;

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

import UI.TUICommands;
import exceptions.ExitProgram;
import exceptions.PacketException;
import exceptions.UtilByteException;
import exceptions.UtilDatagramException;
import network.NetworkLayer;
import network.Packet;
import network.TransportLayer;
import protocol.FileTransferProtocol;

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
	
	boolean running;

	
	/**
	 * Construct a new FileTransfer client.
	 * @param socket
	 * @param port
	 */
	public FileTransferClient(int port) {
		this.TUI = new UI.TUI();

		this.ownPort = port;
		try {
			this.ownAddress = NetworkLayer.getOwnAddress();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		this.sessionActive = false;

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
		TUI.showMessage("Setting up the client...");
		boolean success = false;

		// First, initialise the Server.
		// SERVERNAME = TUI.getString("What is the name of this server?"); // TODO name? 

		boolean successFileSystem = true; // TODO this.setupFileSystem();
		boolean succesSocket = this.setupSocket();
		this.setupOwnAddress();
		boolean succesServer = this.setServer();

		success = successFileSystem && succesSocket && succesServer;
		
		if (success) {
			TUI.showMessage("Setup complete!");
		}
		
		return success;
	}
	
	public boolean setupSocket() throws ExitProgram {
		boolean success = false;
		
		TUI.showMessage("Trying to open a new socket...");
		while (this.socket == null) { // TODO: ask for server port?
			//port = TUI.getInt("Please enter the server port.");

			try {
				this.socket = TransportLayer.openNewDatagramSocket(this.ownPort);
				TUI.showMessage("Client now bound to port " + ownPort);
				success = true;
			} catch (SocketException e) {
				TUI.showError("Something went wrong when opening the socket: "
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
			TUI.showMessage("Client listing on: " + this.ownAddress);
			TUI.showMessage("NOTE: depending on detection method, this may NOT be the actual interface used");
		} catch (UnknownHostException e) {
			TUI.showMessage("Could not determine own address: " + e.getLocalizedMessage());
		} 
	}
	
	public boolean setServer() { //(InetAddress serverAdress, int serverPort) {
		boolean success = false;
		
		try {
//			this.serverAddress = NetworkLayer.getAdressByName("nvc4122.nedap.local");
			this.serverAddress = NetworkLayer.getAdressByName("nu-pi-huub");
			success = true;
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		this.serverPort = FileTransferProtocol.DEFAULT_SERVER_PORT;
		
		return success;
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
				case "start session":
					TUI.showMessage("Initiating session with server...");
					while (!this.requestSession()) { // TODO clear?
						TUI.getBoolean("Try again?");
					}

					break;	

				case FileTransferProtocol.LIST_FILES:
					TUI.showMessage("Requesting list of files...");
					if (!this.requestListFiles()) { // TODO clear?
						TUI.showError("Retrieving list of files failed");
					}
					break;

				case FileTransferProtocol.DOWNLOAD_SINGLE:
					// do something
					break;

				case TUICommands.EXIT:
					// do something
					this.shutdown();
					break;

				default:
					TUI.showError("Unknow command received"); // what TODO with it?
			}
		} catch (IOException | PacketException | UtilDatagramException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		TUI.showMessage("... done!");
	}
	
	public String[] getArguments(String requestString) {
		String[] split = requestString.split(FileTransferProtocol.DELIMITER);
		return split;
	}
	
	public boolean requestSession() throws IOException, PacketException, UtilDatagramException {
		this.sendBytesToServer(FileTransferProtocol.INIT_SESSION);
		
		TUI.showMessage("Waiting for server response...");
		Packet receivedPacket = TransportLayer.receivePacket(this.socket);
		//byte[] responseBytes = receivedPacket.getPayload(); 
		
		String responseString = receivedPacket.getPayloadAsString();
		String[] responseSplit = this.getArguments(responseString);
		
//		if (Arrays.equals(responseBytes, FileTransferProtocol.INIT_SESSION)) {
		if (Arrays.equals(responseSplit[0].getBytes(), FileTransferProtocol.INIT_SESSION)) {
			// TODO note: different from .equals() for strings!
			this.sessionActive = true;
//			this.serverPort = receivedPacket.getSourcePort(); // update to clientHandler
			this.serverPort =  Integer.parseInt(responseSplit[1]); // update to clientHandler
			TUI.showMessage("Session started with server port = " + this.serverPort);
			return true;
		} else {
			this.sessionActive = false;
			TUI.showError("Invalid response to session init");
			this.sessionActive = false;
		}
		return this.sessionActive;
	}
	
	public boolean requestListFiles() throws IOException, PacketException, UtilDatagramException {
		// TODO: store this info in client?!!
		boolean succes = false;
		
		this.sendBytesToServer(FileTransferProtocol.LIST_FILES.getBytes());

		File[] fileArray = null; // String[]

		TUI.showMessage("Waiting for server response...");
		Packet receivedPacket = TransportLayer.receivePacket(this.socket);
		byte[] responseBytes = receivedPacket.getPayload();
		TUI.showMessage("Server response received, now processing...");

		try {
			fileArray = util.Bytes.deserialiseByteArrayTofileArray(responseBytes);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		TUI.showMessage("LIST OF FILES: \n" + Arrays.toString(fileArray));
		succes = true;
		
		return succes;
	}
	
	public void sendBytesToServer(byte[] bytesToSend) { // TODO put in seperate utility?
		try { // to construct and send a packet
			Packet packet = new Packet(
						0, // TODO id
						this.ownAddress,
						this.ownPort, 
						this.serverAddress, 
						this.serverPort,
						bytesToSend
				);
			
			TransportLayer.sendPacket(
					this.socket,
					packet,
					this.serverPort
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
	
	/**
	 * Shutdown server TODO (try with resources?!)
	 */
	public void shutdown() {
		TUI.showMessage("See you later!");
		this.running = false;

		// see example on github? 
		this.socket.close(); // TODO make a method for this, ensure!

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
