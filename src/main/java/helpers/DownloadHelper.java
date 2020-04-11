package helpers;

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
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
	 * TODO
	 */
	private List<Packet> receivedPacketList;

	/**
	 * TODO
	 */
	private int LFR;
	private int RWS;

	/**
	 * TODO
	 */
	private byte[] fileContents;
	
	private String name;
	private UI.TUI TUI;

	/** TODO
	 * @param parent
	 * @param downloadSocket
	 * @param uploaderAddress
	 * @param uploaderPort
	 * @param initiate TODO update
	 */
	public DownloadHelper(Object parent, DatagramSocket downloadSocket, InetAddress uploaderAddress, int uploaderPort,
			int totalFileSize, File fileToWrite) {
		super();
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
		
		this.TUI = new UI.TUI();

		List<Packet> receivedPacketList = new ArrayList<Packet>();

		LFR = -1;
		RWS = 1;

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
		this.showNamedMessage("Receiving...");

		if (initiate) { // TODO running on client
			//		try (ProgressBar pb = new ProgressBar("Test", this.totalFileSize,1)) { 
			// TODO update to 1 ms, also see declartive/builder on doc
			try (ProgressBar pb = new ProgressBar("Test", this.totalFileSize, 1, 
					System.err, ProgressBarStyle.COLORFUL_UNICODE_BLOCK, " Bytes",1, false, null)) {
				pb.setExtraMessage("Downloading..."); // Set extra message at end of the bar

				while (!this.complete) { // loop until we are done receiving the file
					this.receiveBytes();
					pb.stepTo(this.fileContents.length); // step directly to n // TODO this way also counting duplicates/resends!
				} 
				pb.setExtraMessage("Done!"); // Set extra message to display at the end of the bar
			}
		} else { // TODO running on server
			while (!this.complete) { // loop until we are done receiving the file
				this.receiveBytes();
			} 
		}

		this.showNamedMessage("File received completely");
		this.writeFile();

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
		
		int packetID = packet.getId();

		// get total file size from header // TODO not including in every packet?
		//  	byte[] totalFileSizeBytes = new byte[MAX_FILE_SIZE_BYTES];
		//  	for(int i = ID_BYTES; i < MAX_FILE_SIZE_BYTES+ID_BYTES; i++) {
		//      	totalFileSizeBytes[i-ID_BYTES] = header[i].byteValue();
		//      	}
		//      
		//  	int totalFileSize = ByteBuffer.wrap(totalFileSizeBytes).getInt(); // assuming Big-endian!
		//  	int totalFileSize = packet.; // get total file size from header
		//  	this.showNamedMessage("Total file size = " + totalFileSize);

		// tell the user
//		this.showNamedMessage("Received packet " + packetID + ", length="+packet.getPayloadLength()); // TODO debug info

		if (packetID > LFR && packetID <= LFR + RWS) {
//			this.showNamedMessage("Processing packet " + packetID); // TODO debug info
			
			// append the packet's data part (excluding the header) to the fileContents array, first making it larger
			int oldlength = fileContents.length;
			int datalen = packet.getPayloadLength(); //packet.length - HEADERSIZE;
			fileContents = Arrays.copyOf(fileContents, oldlength + datalen);
			System.arraycopy(packet.getPayloadBytes(), 0, fileContents, oldlength, datalen); 
			// TODO start at beginning payload

			LFR = packetID;
			
			this.sendAck(packetID);
		} else {
			this.showNamedMessage("DROPPING packet " + packetID);
		}

	}

	public void sendAck(int idToAck) {
		this.sendBytesToUploader(idToAck, FileTransferProtocol.ACK);
		
		if (this.paused) {
			this.sendBytesToUploader(idToAck, FileTransferProtocol.PAUSE_DOWNLOAD); // TODO keep this id? 
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
		this.showNamedMessage("Writing file contents to file...");
		long timestamp = System.currentTimeMillis();
		util.FileOperations.setFileContents(this.fileContents, this.fileToWrite, timestamp);
		this.showNamedMessage("... file written to " + this.fileToWrite.getAbsolutePath());
	}
	

	public void setUploaderPort(int uploaderPort) {
		this.uploaderPort = uploaderPort;
	}
	
	public void setTotalFileSize(int totalFileSize) {
		this.totalFileSize = totalFileSize;
	}
	
	public synchronized void pause() {
		this.paused = true;
		this.sendBytesToUploader(0, FileTransferProtocol.PAUSE_DOWNLOAD); // TODO  id? 
		
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
	
	@Override 
	public String toString() {
		return this.name;
	}
	
}