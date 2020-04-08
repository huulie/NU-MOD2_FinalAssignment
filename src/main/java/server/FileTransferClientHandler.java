package server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import exceptions.PacketException;
import exceptions.UtilByteException;
import exceptions.UtilDatagramException;
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
	
	
	FileTransferServer server;
	/**
	 * TODO
	 */
	Path fileStorage;

	
	/** The TUI of this FileTransferServer. */
	private UI.TUI TUI; 
	
	boolean running;
	
	
	/**
	 * Construct a new FileTransfer client handler.
	 * @param socket
	 * @param ownPort
	 */
	public FileTransferClientHandler(DatagramSocket socket, Packet initPacket, FileTransferServer server) { // int port
		this.TUI = new UI.TUI();
		
		this.socket = socket;
		this.ownPort = socket.getLocalPort();
		this.ownAddress = socket.getLocalAddress();
		
		this.server = server;
		this.fileStorage = server.getFileStorage("all"); // TODO for now hardcoded

		this.running = true;
		this.notifyclient(initPacket);
		
		TUI.showMessage("Listening for client requests...");
		
		// TODO add setName for handler, and then printWithPrefix
		

	}
	
	public void notifyclient(Packet initPacket) {
		this.clientAddress = initPacket.getSourceAddress();
		this.clientPort = initPacket.getSourcePort();
		
		TUI.showMessage("Notifying client of session port... ");
//		this.sendBytesToClient(FileTransferProtocol.INIT_SESSION);
//		TUI.showMessage("Notification send!");
		TUI.showMessage("Already done by server!");
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
			TUI.showMessage("Received a packet: going to process it...");
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
				TUI.showMessage("Client requested list of files...");
				//String list = this.listFiles();
				//this.sendBytesToClient(list.getBytes());
				this.sendBytesToClient(this.listFiles());
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

	public String[] getArguments(String requestString) { // TODO in shared sperate method?
		String[] split = requestString.split(FileTransferProtocol.DELIMITER);
		return split;
	}
	
	public byte[] listFiles() {
		TUI.showMessage("Creating list of files in current directory..");
		// https://www.baeldung.com/java-list-directory-files
		int depth = 1;
		//Path dir = this.fileStorage; // Paths.get(dir)
		
		byte[] filesByteArray = null;
		
//		try (Stream<Path> stream = Files.walk(this.fileStorage, depth)) { // TODO: this is try with resources
//	        stream // not return  ... 
//	          .filter(file -> !Files.isDirectory(file))
//	          .map(Path::getFileName)
//	          .map(Path::toString)
//	          .collect(Collectors.toSet()); // this returns Set<String> if returning stream
//	          
//	        String[]  filesArray = stream.toArray();//stream.toArray(String[]::new);
	        
		File[] filesArray = new File(this.fileStorage.toString()).listFiles(); // TODO do without files?

	        System.out.println(Arrays.toString(filesArray));
	     // https://stackoverflow.com/questions/14669820/how-to-convert-a-string-array-to-a-byte-array-java
			try {
	        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			ObjectOutputStream objectOutputStream =
			    new ObjectOutputStream(byteArrayOutputStream);
			objectOutputStream.writeObject(filesArray);
			objectOutputStream.flush();
			objectOutputStream.close();

			filesByteArray = byteArrayOutputStream.toByteArray();
	          
	    } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return filesByteArray;
		//return "List of all files -to implement-";
	}
	
	public String downloadSingle() {
		return "Single file download -to implement-";
		
	}
	
	public void sendBytesToClient(byte[] bytesToSend) { // TODO put in separate utility?
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
					this.clientPort
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
	
	public void shutdown() {
		// see example on github? 
		this.socket.close(); // TODO make a method for this, ensure!

	}
	
	public int getPort() {
		return this.ownPort;
	}
	
}
