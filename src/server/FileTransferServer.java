package server;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import exceptions.PacketException;
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
	int port = 0;

	// TODO: same as example, because interrupt thread? 	
	boolean running;

	/**
	 * Construct a new FileTransfer server.
	 * @param socket
	 * @param port
	 */
	public FileTransferServer(int port) {
		// Initialise instance variables:
		this.port = port;
		this.running = true;

		this.clients = new ArrayList<>();
		this.next_client_no = 1;
		this.clientsWaitingList = new ArrayList<>();

		this.TUI = new UI.TUI();

		// Do setup
		boolean setup = true;
		while (setup) {
			try {
				this.setup();
			} catch (exceptions.ExitProgram eExit) {
				// If setup() throws an ExitProgram exception, stop the program.
				if (!TUI.getBoolean("Do you want to retry setup?")) {
					setup = false;
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
	public void setup() throws exceptions.ExitProgram {
		TUI.showMessage("Setting up the server...");
		// First, initialise the Server.
		// SERVERNAME = TUI.getString("What is the name of this server?"); // TODO name? 

		TUI.showMessage("Trying to open a new socket...");
		while (this.socket == null) { // TODO: ask for server port?
			//port = TUI.getInt("Please enter the server port.");

			try {
				this.socket = TransportLayer.openNewDatagramSocket(this.port);
			} catch (SocketException e) {
				TUI.showMessage("Something went wrong when opening the socket: "
						+ e.getLocalizedMessage());
				if (!TUI.getBoolean("Do you want to try again?")) {
					throw new exceptions.ExitProgram("User indicated to exit the "
							+ "program after socket opening failure.");
				}
			}
		}
		TUI.showMessage("Server now bound to port " + port);


		try {
			this.ownAddress = NetworkLayer.getOwnAddress(); // TODO replace by discover?
			TUI.showMessage("Server listing on: " + this.ownAddress);
		} catch (UnknownHostException e) {
			TUI.showMessage("Could not determine own address: " + e.getLocalizedMessage());
		} 

		TUI.showMessage("Setup complete!");
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

				if (receivedPacket.getPayload().equals(FileTransferProtocol.INIT_SESSION)) {
					this.handleSessionRequest(receivedPacket);
				} else {
					TUI.showMessage("Unknown packet: dropping");
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
					sessionInitPacket); //TODO (sock, this, name);
			new Thread(handler).start();
			clients.add(handler);

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
			port = FileTransferProtocol.SERVER_PORT;
			System.out.println("Using default server port " + port + "...");
		}
			
		FileTransferServer server =  new FileTransferServer(port);
		System.out.println("Starting server...");
		new Thread(server).start();
	}

}

