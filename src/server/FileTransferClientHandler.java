package server;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import exceptions.PacketException;
import exceptions.UtilByteException;
import exceptions.UtilDatagramException;
import network.NetworkLayer;
import network.Packet;
import network.TransportLayer;
import protocol.FileTransferProtocol;

public class FileTransferClientHandler implements Runnable {

	/**
	 * TODO 
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
	InetAddress clientAddress;
	int clientPort;
	
	/** The TUI of this FileTransferServer. */
	private UI.TUI TUI; 
	
	boolean running;
	
	
	/**
	 * Construct a new FileTransfer client handler.
	 * @param socket
	 * @param port
	 */
	public FileTransferClientHandler(DatagramSocket socket, Packet initPacket) { // int port
		this.TUI = new UI.TUI();
		
		this.socket = socket;
		this.ownPort = socket.getLocalPort();
		this.ownAddress = socket.getLocalAddress();

		this.running = true;
		this.notifyclient(initPacket);
		
		TUI.showMessage("Listening for client requests...");
		

	}
	
	public void notifyclient(Packet initPacket) {
		this.clientAddress = initPacket.getSourceAddress();
		this.clientPort = initPacket.getSourcePort();
		
		TUI.showMessage("Notifying client of session port... ");
		this.sendBytesToClient(FileTransferProtocol.INIT_SESSION);
		TUI.showMessage("Notification send!");	
	}
	
	/**
	 * Receives a packet, preprocess it and pass it on to process it
	 */
	public void run() { //receiveRequest() {
		// receive
		
		Packet receivedPacket = null;
		
		try {
			receivedPacket = TransportLayer.receivePacket(this.socket);
		} catch (IOException | PacketException | UtilDatagramException e) {
			// TODO Auto-generated catch block
			TUI.showError("Someting went wrong with recieving a packet!");
			TUI.showError("Not going to process it: trying to receive a new packet");
			//e.printStackTrace();
		}

		if (receivedPacket == null) {
			TUI.showError("Someting went wrong with recieving a packet!");
			TUI.showError("Not going to process it: trying to receive a new packet");
		} else {
			System.out.println("Received a packet: going to process it...");
			String receivedString = util.PacketUtil.convertPayloadtoString(receivedPacket);
			this.processRequest(receivedString);
		}
	}

	
	public void processRequest(String requestString) {

		String[] request = this.getArguments(requestString); 
		String command = request[0]; //.charAt(0); // TODO or String?
		// TODO check string being null, to prevent nullpointer exception?!

//		try {
			switch (command) {
				case FileTransferProtocol.LIST_FILES:
					// do something
					TUI.showMessage("Sending list of files to client...");
					String list = this.listFiles();
					this.sendBytesToClient(list.getBytes());
					break;
				
				case FileTransferProtocol.DOWNLOAD_SINGLE:
					// do something
					break;

				default:
					TUI.showError("Unknow command received"); // what TODO with it?
			}
//		} catch (Exception e) { // TODO specify!
//
//		}
		TUI.showMessage("... done!");

	}

	public String[] getArguments(String requestString) {
		String[] split = requestString.split(FileTransferProtocol.DELIMITER);
		return split;
	}
	
	public String listFiles() {
//		try (Stream<Path> walk = Files.walk(Paths.get("/"))) { // "C:\\projects"
//			// https://mkyong.com/java/java-how-to-list-all-files-in-a-directory/
//
//			List<String> result = walk.filter(Files::isRegularFile)
//					.map(x -> x.toString()).collect(Collectors.toList());
//
//			result.forEach(System.out::println);
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
		return "List of all files -to implement-";
		
	}
	
	public String downloadSingle() {
		return "Single file download -to implement-";
		
	}
	
	public void sendBytesToClient(byte[] bytesToSend) {
		try { // to construct and send a packet
			Packet packet = new Packet(
						0, // TODO id
						this.ownAddress,
						this.ownPort, 
						this.clientAddress, 
						this.clientPort,
						bytesToSend
				);
			
			TransportLayer.sendPacket(
					this.socket,
					packet,
					this.ownPort
			); 
			
			TUI.showMessage("Notification send!");
			
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
	
	public void shutdown() {
		// see example on github? 
		this.socket.close(); // TODO make a method for this, ensure!

	}
	
	public int getPort() {
		return this.ownPort;
	}
	
}
