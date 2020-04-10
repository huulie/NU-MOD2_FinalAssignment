package server;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import exceptions.PacketException;
import exceptions.UtilByteException;
import exceptions.UtilDatagramException;
import helpers.DownloadHelper;
import helpers.UploadHelper;
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
	
	/**
	 *  List of download, one for each connected downloadHelper. 
	 *  */
	private List<UploadHelper> uploads;
	

	
	/** The TUI of this FileTransferServer. */
	private UI.TUI TUI; 
	
	private String name;
	
	boolean running;
	
	
	/**
	 * Construct a new FileTransfer client handler.
	 * @param socket
	 * @param ownPort
	 */
	public FileTransferClientHandler(DatagramSocket socket, Packet initPacket, FileTransferServer server, String name) { // int port
		this.TUI = new UI.TUI();
		
		this.socket = socket;
		this.ownPort = socket.getLocalPort();
		this.ownAddress = socket.getLocalAddress();
		
		this.name = name;
		
		this.server = server;
		this.fileStorage = server.getFileStorage("all"); // TODO for now hardcoded

		this.uploads = new ArrayList<>();
		
		this.running = true;
		this.setClient(initPacket);
		
		
		// TODO add setName for handler, and then printWithPrefix
		

	}
	
	public void setClient(Packet initPacket) {
		this.clientAddress = initPacket.getSourceAddress();
		this.clientPort = initPacket.getSourcePort();
		
		this.showNamedMessage("Set client information in handler: done ");
	}
	
	/**
	 * Receives a packet, preprocess it and pass it on to process it
	 */
	public void run() { //receiveRequest() {
		// receive
		
		while(running) { // keep listening
			Packet receivedPacket = null;

			this.showNamedMessage("Listening for client requests...");

			try {
				receivedPacket = TransportLayer.receivePacket(this.socket);
			} catch (IOException | PacketException | UtilDatagramException e) {
				// TODO Auto-generated catch block
				this.showNamedError("Someting went wrong with recieving a packet!");
				this.showNamedError("Not going to process it: trying to receive a new packet");
				//e.printStackTrace();
			}

			if (receivedPacket == null) {
				this.showNamedError("Someting went wrong with recieving a packet!");
				this.showNamedError("Not going to process it: trying to receive a new packet");
			} else {
				this.showNamedMessage("Received a packet: going to process it...");
				this.showNamedMessage("Packet payload: " + new String(receivedPacket.getPayload()));
				
				String receivedString = receivedPacket.getPayloadString();
				this.showNamedMessage("Received String: " + receivedString);
				byte[] receivedBytes = receivedPacket.getPayloadBytes();
				this.showNamedMessage("Received bytes: " + receivedString);

				this.processRequest(receivedString, receivedBytes);
			}
		}
	}

	
	public void processRequest(String requestString, byte[] requestBytes) {
		
		// TODO NOT convert from bytes to String and back!
		//https://stackoverflow.com/questions/22519346/how-to-split-a-byte-array-around-a-byte-sequence-in-java/29084734
		// https://stackoverflow.com/questions/2758654/conversion-of-byte-into-a-string-and-then-back-to-a-byte
		//this.showNamedMessage("RequestBytes: " + Arrays.toString(requestBytes));
		
		String[] request = this.getArguments(requestString); 
		this.showNamedMessage("Received request: " + Arrays.toString(request));
		
		String command = request[0]; //.charAt(0); // TODO or String?
		// TODO check string being null, to prevent nullpointer exception?!

//		try {
		switch (command) {
			case FileTransferProtocol.LIST_FILES:
				// do something
				this.showNamedMessage("Client requested list of files...");
				//String list = this.listFiles();
				//this.sendBytesToClient(list.getBytes());
				this.sendBytesToClient(this.listFiles(), 0);
				break;

			case FileTransferProtocol.DOWNLOAD:
				this.showNamedMessage("Client requested download of single file...");
				try {
					File fileToUpload = util.Bytes.deserialiseByteArrayToFile(requestBytes); //(request[1].getBytes());
					this.showNamedMessage("File: " + fileToUpload.getAbsolutePath());
					
					int downloaderPort = Integer.parseInt(request[1]);
					this.showNamedMessage("To downloader on port: " + downloaderPort);
					
					this.downloadSingle(fileToUpload, downloaderPort);
				} catch (NumberFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ClassNotFoundException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				break;

			default:
				this.showNamedError("Unknow command received"); // what TODO with it?
		}
//		} catch (Exception e) { // TODO specify!
//
//		}
		this.showNamedMessage("... done!");

	}

	public String[] getArguments(String requestString) { // TODO in shared sperate method?
		String[] split = requestString.split(FileTransferProtocol.DELIMITER);
		return split;
	}
	
	public byte[] listFiles() {
		
		// TODO: store this info in clientHandler?!! (aks to request update if changed since last time)

		this.showNamedMessage("Creating list of files in current directory..");
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
		// TODO do without files?
	        
		File[] filesArray = new File(this.fileStorage.toString()).listFiles( // TODO only non-hidden
				new FileFilter() {
					@Override
					public boolean accept(File file) {
						return !file.isHidden();
					}
				});

	        System.out.println(Arrays.toString(filesArray));
	     // https://stackoverflow.com/questions/14669820/how-to-convert-a-string-array-to-a-byte-array-java
			try {
	       	filesByteArray = util.Bytes.serialiseObjectToByteArray(filesArray);
	          
	    } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return filesByteArray;
		//return "List of all files -to implement-";
	}
	
	public void downloadSingle(File fileToUpload, int downloaderPort) {
		// create uploader helper with file and port from request
		
		DatagramSocket uploadSocket;
		try {
			uploadSocket = TransportLayer.openNewDatagramSocket();

			int fileSizeToUpload = (int) fileToUpload.length(); // TODO casting long to int!

			UploadHelper uploadHelper = new UploadHelper(this, uploadSocket, 
					this.clientAddress, downloaderPort, fileSizeToUpload, fileToUpload);

			this.uploads.add(uploadHelper);

			// start upload helper
			new Thread(uploadHelper).start();

			// let downloadHelper know about uploader
			byte[] singleFileResponse = (FileTransferProtocol.UPLOAD +
					FileTransferProtocol.DELIMITER +
					uploadSocket.getLocalPort() + // TODO ask to helper/?
					FileTransferProtocol.DELIMITER + 
					fileSizeToUpload).getBytes();
			
			byte[] fileToUploadBytes = util.Bytes.serialiseObjectToByteArray(fileToUpload);
			
			
			this.sendBytesToClient(util.Bytes.concatArray(singleFileResponse, fileToUploadBytes),
					singleFileResponse.length - 1 + 1); // TODO make this more nice + note offset is string end +1 (note length starts at 1)


		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public void sendBytesToClient(byte[] bytesToSend, int byteOffset) { // TODO put in separate utility?
		try { // to construct and send a packet
			Packet packet = new Packet(
						0, // TODO id
						this.ownAddress,
						this.ownPort, 
						this.clientAddress, 
						this.clientPort,
						bytesToSend,
						byteOffset
				);
			
			TransportLayer.sendPacket(
					this.socket,
					packet,
					this.clientPort
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
	
	public void shutdown() {
		// see example on github? 
		this.socket.close(); // TODO make a method for this, ensure!

	}
	
	public int getPort() {
		return this.ownPort;
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
	
}