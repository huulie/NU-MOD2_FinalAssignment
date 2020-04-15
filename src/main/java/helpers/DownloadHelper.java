package helpers;

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import client.FileTransferClient;
import exceptions.PacketException;
import exceptions.UtilByteException;
import exceptions.UtilDatagramException;
import network.Packet;
import network.TransportLayer;
import protocol.FileTransferProtocol;
import server.FileTransferClientHandler;

import me.tongfei.progressbar.*;


/**
 * TODO maybe generalise to downloadHelper (both client and server)??
 * @author huub.lievestro
 *
 */
public class DownloadHelper implements Helper, Runnable {

	/**
	 * Coonected process TODO
	 */
	private Object parent;

	/**
	 * Socket used for download TODO
	 */
	private DatagramSocket downloadSocket;

	/**
	 * Address used by uploader TODO
	 */
	private InetAddress uploaderAddress;

	/**
	 * Port used by uploader TODO
	 */
	private int uploaderPort;

	/**
	 * Indicate running on client side: need to initiate (uploader will wait)
	 */
	private boolean initiate;

	/**
	 * TODO: send only once, not every packet? 
	 */
	private int totalFileSize;
	
	/**
	 * TODO
	 */
	private int totalPackets;
	
	/**
	 * TODO
	 */
	File fileToWrite;
	
	/**
	 * Indicating download complete TODO
	 */
	private boolean complete;
	
	/**
	 * TODO indicate if paused or not
	 */
	private boolean paused;
	
	/**
	 * (re)start time
	 */
	long startTime;
	
	
	/**
	 * Duration of transfer, in nanoseconds
	 */
	long duration;

	/**
	 * TODO
	 */
	private List<Packet> receivedPacketList;

	/**
	 * TODO LFR = lastFrameReceived, RWS = receive window size
	 */
	private int LFR;
	private int RWS;
	
	/**
	 * TODO will not be limiting: because LAR rtc is int
	 */
	private int idWrapCounter;

	/**
	 * TODO
	 */
	private int startID;
	
	/**
	 * 
	 */
	private int droppedPackets;
	
	/**
	 * TODO
	 */
	private byte[] fileContents;
	
	private String name;
	private userInterface.TUI TUI;

	/** TODO
	 * @param parent
	 * @param downloadSocket
	 * @param uploaderAddress
	 * @param uploaderPort
	 * @param initiate TODO update
	 */
	public DownloadHelper(Object parent, DatagramSocket downloadSocket, InetAddress uploaderAddress, int uploaderPort,
			int totalFileSize, File fileToWrite, int startID) {
		this.parent = parent;
		this.downloadSocket = downloadSocket;
		this.uploaderAddress = uploaderAddress;
		this.uploaderPort = uploaderPort;
		
		//this.initiate = initiate;
		if (parent instanceof FileTransferClient) { // TODO or just input manually?
			this.initiate = true;
			this.name = ((FileTransferClient) parent).getName() + "_Downloader-" + fileToWrite.getName();
		} else if (parent instanceof FileTransferClientHandler) {
			this.initiate = false;
			this.name = ((FileTransferClientHandler) parent).getName() + "_Downloader-" + fileToWrite.getName();
		} else {
			this.name = "Downloader-" + fileToWrite.getName();
			this.showNamedError("Unknown parent object type!");
			// TODO set initiate or not? 
		}
		
		this.totalFileSize = totalFileSize;
		
		this.fileToWrite = fileToWrite;
		this.complete = false;
		
		this.paused = false; 
		//this.startTime = LocalDateTime.now(); TODO
		this.startTime = System.nanoTime();
		this.duration = 0;
		
		this.TUI = new userInterface.TUI();

		//List<Packet> receivedPacketList = new ArrayList<Packet>(); TODO this could not be local! and now in run();

		this.LFR = -1;
		this.RWS = (FileTransferProtocol.MAX_ID + 1)/ 2 - 1;// 2; 
		// TODO = SWS
		this.startID = startID; // TODO or set with method and initially assume zero
		
		if (this.startID < (FileTransferProtocol.MAX_ID - this.RWS)) {
			this.idWrapCounter = 0; // TODO only RWS of zero will set it first to zero TODO placement in file1
		} else { // already one wraparound in IDs
			this.idWrapCounter = 1;
		}
		/**
		 * TODO dropped outside windo, not because security
		 */
		this.droppedPackets = 0;
		
	

		// create the array that will contain the file contents
		// note: we don't know yet how large the file will be, so the easiest (but not most efficient)
		//   is to reallocate the array every time we find out there's more data
		fileContents = new byte[0];
	}


	@Override
	public void run() {
		this.showNamedMessage("Starting download helper...");

		
		if (initiate) {
			this.initiateTransfer();
		} 

		this.showNamedMessage("Total file size = " + this.totalFileSize + " bytes");
		this.totalPackets = (int) Math.ceil(this.totalFileSize/FileTransferProtocol.MAX_PAYLOAD_LENGTH) +1;
		this.showNamedMessage("Number of packets to receive: " + this.totalPackets);
		this.receivedPacketList = new ArrayList<Packet>(Collections.nCopies(this.totalPackets, null));
		
		this.showNamedMessage("Receiving...");

		if (initiate) { // TODO running on client
			//		try (ProgressBar pb = new ProgressBar("Test", this.totalFileSize,1)) { 
			// TODO update to 1 ms, also see declartive/builder on doc
//			try (ProgressBar pb = new ProgressBar("Test", this.totalFileSize, 1, 
//					System.err, ProgressBarStyle.COLORFUL_UNICODE_BLOCK, " Bytes",1, false, null)) {
//				pb.setExtraMessage("Downloading..."); // Set extra message at end of the bar

				while (!this.complete) { // loop until we are done receiving the file
					this.receiveBytes();
//					pb.stepTo(this.fileContents.length); // step directly to n // TODO this way also counting duplicates/resends!
//				} 
//				pb.setExtraMessage("Done!"); // Set extra message to display at the end of the bar
			}
		} else { // TODO running on server
			while (!this.complete) { // loop until we are done receiving the file
				this.receiveBytes();
			} 
		}

		this.showNamedMessage("File received completely");
		this.writeFile();
		this.duration += System.nanoTime() - this.startTime;
		this.showStats();
		this.showNamedMessage("Download complete: helper shutting down");
		this.shutdown();
		

	}

	public void initiateTransfer() {
		this.sendBytesToUploader(0, FileTransferProtocol.START_DOWNLOAD); // TODO id?
		this.showNamedMessage("Download initiated...");
	}

	public void receiveBytes() {
		// try to receive a packet from the network layer
		try {
			
			Packet packet = TransportLayer.receivePacket(this.downloadSocket);


			// if we indeed received a packet
			if (packet != null) {
				this.processPacket(packet);
				this.checkComplete(); // only stop when whole file is in 
			} else {
				// wait ~10ms (or however long the OS makes us wait) before trying again
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					this.showNamedError("INTERRUPTED EXCEPTION occured");
					//this.complete = true; // TODO
				}
			}
		} catch (SocketTimeoutException e) {
			this.showNamedMessage("Socket timed-out: retry receive");
			this.receiveBytes();
		} catch (IOException | PacketException | UtilDatagramException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	public void processPacket(Packet packet) {
		
		if (!(packet.getSourceAddress().equals(this.uploaderAddress)
				&& packet.getSourcePort() == this.uploaderPort)) { 
			this.showNamedError("SECURITY WARNING: this response is NOT"
					+ " coming for known uploader > dropping it");
			return;
		}
		
		int packetNr = this.IdToNr(packet.getId());


		// tell the user
		this.showNamedMessage("Received packet with ID = " + packet.getId() + ", could be nr " + packetNr); // TODO debug info
//		this.showNamedMessage("Received packet " + packetID + ", length="+packet.getPayloadLength()); // TODO debug info

		System.out.println("Should be > LFR " + LFR);
		System.out.println("Should be <= LFR + RWS " + (LFR + RWS));
		
		if (packetNr > LFR && packetNr <= LFR + RWS) {
			this.showNamedMessage("Processing packet " + packetNr); // TODO debug info

			this.receivedPacketList.add(packetNr, packet);


			for (int iNext = 0; packetNr + iNext < this.receivedPacketList.size(); iNext++) {
				
				
				Packet receivedPacket = this.receivedPacketList.get(packetNr + iNext);
				if (receivedPacket != null) {
					// append the packet's data to the fileContents array
					int oldlength = fileContents.length;
					int datalen = receivedPacket.getPayloadLength(); //packet.length - HEADERSIZE;
					fileContents = Arrays.copyOf(fileContents, oldlength + datalen);
					System.arraycopy(receivedPacket.getPayloadBytes(), 0, fileContents, oldlength, datalen); 
				} else {	
					// set last received to the packet before null (missing packet)
					LFR = packetNr + iNext - 1; // TODO was packetNr
					break;
				}
			}

			this.sendAck(packetNr); // TODO or always ack: no, because ahead of window has to be resend (but resend below window?)
		} else {
			this.showNamedMessage("DROPPING packet with ID = " + packet.getId());
			this.droppedPackets++;
		}

	}

	public void sendAck(int nrToAck) {
		int packetID = nrToId(nrToAck);
		
		int maxIdReceived = nrToId(nrToAck + RWS) ; // TODO naming
		if (maxIdReceived == 0) {
			this.idWrapCounter++;
			this.showNamedMessage("packet ID wrap around"); // TODO debug
		}
		
		this.sendBytesToUploader(packetID, FileTransferProtocol.ACK);
		this.showNamedMessage("Packet " + nrToAck + " with ID = " + packetID + " ACK send");
		
		if (this.paused) {
			this.sendBytesToUploader(packetID, FileTransferProtocol.PAUSE_DOWNLOAD); // TODO keep this id? 
		}
	}
	
	public void checkComplete() {
		if (fileContents.length >= this.totalFileSize) {
//			this.showNamedMessage("File received completely"); // TODO after progressbar
			this.complete = true;
		} else {
			this.complete = false;
		}

	}

	public void sendBytesToUploader(int id, byte[] bytesToSend) { // TODO put in seperate utility?
		try { // to construct and send a packet
			Packet packet = new Packet(
					id, 
					this.downloadSocket.getLocalAddress(), // TODO request once and store? pass on?
					this.downloadSocket.getLocalPort(), 
					this.uploaderAddress, 
					this.uploaderPort,
					bytesToSend
					); // TODO only sending bytes, so no byteOffset

			TransportLayer.sendPacket(
					this.downloadSocket,
					packet,
					this.uploaderPort
					); 

//			this.showNamedMessage("Bytes send!"); // TODO debug info

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

	public void writeFile() {
//		this.showNamedMessage("Building fileContent from packets");
//		
//		for (Packet p : this.receivedPacketList) {
//		// append the packet's data part (excluding the header) to the fileContents array, first making it larger
//		int oldlength = fileContents.length;
//		int datalen = p.getPayloadLength(); //packet.length - HEADERSIZE;
//		fileContents = Arrays.copyOf(fileContents, oldlength + datalen);
//		System.arraycopy(p.getPayloadBytes(), 0, fileContents, oldlength, datalen); 
//		}
		
		this.showNamedMessage("Writing file contents to file...");
		long timestamp = System.currentTimeMillis();
		try {
			util.FileOperations.setFileContents(this.fileContents, this.fileToWrite);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.showNamedMessage("... file written to " + this.fileToWrite.getAbsolutePath());
	}
	

	public void setUploaderPort(int uploaderPort) {
		this.uploaderPort = uploaderPort;
	}
	
	public void setTotalFileSize(int totalFileSize) {
		this.totalFileSize = totalFileSize;
	}
	
	public void setStartID(int startID) {
		this.startID = startID;
	}


	public synchronized void pause() {
		this.paused = true;
		this.sendBytesToUploader(0, FileTransferProtocol.PAUSE_DOWNLOAD); // TODO  id? 
		this.duration += System.nanoTime() - this.startTime;
		
		try {
			this.downloadSocket.setSoTimeout(1000); // TODO otherwise, will block in .receive and not transmit local resume
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		this.showNamedMessage("=PAUSED");
	}
	
	public synchronized void resume() {
		this.paused = false;
		this.sendBytesToUploader(0, FileTransferProtocol.RESUME_DOWNLOAD); // TODO ACK, to resend when lost! 
		this.startTime = System.nanoTime(); // restart timer
		
		try {
			this.downloadSocket.setSoTimeout(0); // TODO revert socket to default operation
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		this.showNamedMessage("=RESUMED"); // TODO not able to see difference between paused or otherwise non-sending uploader
		// TODO so not able to resume download from paused uploader: blocking in receive
	}
	
	public boolean isPaused() {
		return this.paused;
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
	
	public void showStats() {
		this.showNamedMessage("Transferred file: " + fileToWrite.getName());
		this.showNamedMessage("Transfer complete: " + this.complete);
		this.showNamedMessage("-------------------------------->");
		this.showNamedMessage("Total file size: " + this.totalFileSize + " bytes");
		this.showNamedMessage("Transfer duration: " + this.duration + " nanoseconds"); // TODO units
		this.showNamedMessage("Average transferspeed: " + (this.totalFileSize / this.duration) + " bytes/nanosec"); // TODO units?
		this.showNamedMessage("Number of dropped packets: " + this.droppedPackets);
		this.showNamedMessage("--------------------------------<");


	}
	
	public void shutdown() {
		if (!this.complete) {
			this.showNamedError("WARNING! preliminairy shutdown: transfer not complete!");
		}
		this.showNamedMessage("Helper is shutting down.");
		
		this.downloadSocket.close();
	}
	
	@Override 
	public String toString() {
		return this.name;
	}
	
	// private methods //TODO why make private? TODO or in utility?! >> note static to use intancevar
	/**
	 * TODO
	 * @param packetNumber
	 * @return
	 */
	private int nrToId(int packetNumber) {
		return (packetNumber + this.startID) % FileTransferProtocol.MAX_ID ;
	}
	
	private int IdToNr(int packetID) {
		int unwrappedId = -1; // TODO need to initialize
		
		int correctedId = packetID ;

		
		int previousWraparoundRangeID = correctedId 
				+ (this.idWrapCounter - 1) * FileTransferProtocol.MAX_ID;
		int currentWraparoundRangeID = correctedId 
				+ (this.idWrapCounter) * FileTransferProtocol.MAX_ID;

//		System.out.println("max: " + FileTransferProtocol.MAX_ID); // TODO debug
//		System.out.println("recvd: " + receivedId); // TODO debug
//		System.out.println("LFR: " + receivedId); // TODO debug
		
		if (!(this.LFR > previousWraparoundRangeID)) { 
			// check if packet from previous wraparound is already received (= expected earlier)
			unwrappedId = previousWraparoundRangeID;
		} else {//if (this.currentPacketToSend > currentWraparoundId) {
			// TODO no way to know is packet was already sent?!! TODO check with Djurre
			unwrappedId = currentWraparoundRangeID;
//		} else {
//			this.showNamedError("Something weird happend while wrapping around packet IDs");
//			this.shutdown();
		}
//		System.out.println(unwrappedId); // TODO debug
		
		//return unwrappedId - this.startID; // TODO ????
		return unwrappedId - this.startID; // TODO will not return negative, als currentWraparound add correspondings mutiple of MAX_ID

	}
	
	
}