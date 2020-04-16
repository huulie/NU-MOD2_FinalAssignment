package helpers;

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import client.FileTransferClient;
import exceptions.PacketException;
import exceptions.UtilByteException;
import exceptions.UtilDatagramException;
import network.Packet;
import network.TransportLayer;
import protocol.FileTransferProtocol;
import server.FileTransferClientHandler;


/**
 * UploadHelper to upload a file.
 * @author huub.lievestro
 *
 */
public class UploadHelper implements Helper, Runnable, util.ITimeoutEventHandler { 

	/**
	 * Connected process, which started this helper.
	 */
	private Object parent;

	/**
	 * Socket used for upload.
	 */
	private DatagramSocket uploadSocket;

	/**
	 * Address used by downloader, to upload to.
	 */
	private InetAddress downloaderAddress;

	/**
	 * Port used by downloader, to upload to.
	 */
	private int downloaderPort;

	/**
	 * Indicate running on client side: need to initiate (uploader will wait).
	 * Note: this may be needed to let downloader open a way through Firewall(s) first.
	 */
	private boolean waitForInitiate;

	/**
	 * Total file size to upload.
	 */
	private long totalFileSize;

	/**
	 * File object to read and upload.
	 */
	File fileToRead;

	/** 
	 * List of send packets.
	 */
	private List<Packet> packetList;

	/** 
	 * Last Acknowledged Frame (by downloader).
	 */
	private int LAR;
	
	/**
	 * Sliding Window Size, 
	 * represents how many unacknowledged packets may be send.
	 * Note: may not exceed (FileTransferProtocol.MAX_ID + 1)/ 2 - 1;
	 */
	private int SWS;
	
	/**
	 * Count of how many times the ID has wrapped around. 
	 * 	(wraparound = passed ID_MAX and started at zero again)
	 */
	private int idWrapCounter;
	
	/*
	 * ID to use at start of transfer, when uploader sends first packet.
	 */
	private int startID;
	
	/**
	 * Keeps track of where we are in the data.
	 */
	private int filePointer;
	
	/**
	 * Contents of the file, read from the File object.
	 */
	private byte[] fileContents;
	
	/** 
	 * Number of packets to send in total.
	 */
	private int totalPackets;
	
	/**
	 * Number of packets currently send.
	 */
	private int currentPacketToSend;
	
	/**
	 * Number of packets currently acknowledged.
	 */
	private int totalAckPackets;
	
	/**
	 * Name of this uploadHelper (mainly for printing named messages).
	 */
	private String name;
	
	/**
	 * TUI, to provide status messages to user.
	 */
	private userInterface.TUI textUI;

	/**
	 * Number of packets currently resend.
	 * (including duplicate resends)
	 */
	private int totalResendPackets;
	
	/**
	 * Threshold of fraction of resend packets relative to total packets to transmit, in percentage
	 * If above this threshold, the upload will be aborted due to too unstable network. 
	 */
	private int thresholdResend;
	

	/**
	 * Indicating upload complete.
	 */
	private boolean complete;
	
	/**
	 * Indicate if paused or not.
	 */
	private boolean paused;
	
	/**
	 * (re)start time of upload.
	 */
	long startTime;
	
	/**
	 * Duration of transfer, in nanoseconds.
	 */
	long duration;
	
	/**
	 * Create a new UploadHelper, and initialise instance variables.
	 * @param parent creating this UploadHelper
	 * @param uploadSocket to use
	 * @param downloaderAddress to upload to
	 * @param downloaderPort to upload to
	 * @param totalFileSize to upload
	 * @param fileToRead to read and upload
	 */
	public UploadHelper(Object parent, DatagramSocket uploadSocket, InetAddress downloaderAddress,
			int downloaderPort, long totalFileSize, File fileToRead) {
		this.parent = parent;
		this.uploadSocket = uploadSocket;
		this.downloaderAddress = downloaderAddress;
		this.downloaderPort = downloaderPort;
		
		if (parent instanceof FileTransferClient) {
			this.waitForInitiate = false;
			this.name = ((FileTransferClient) parent).getName() 
					+ "Uploader-" + fileToRead.getName();
		} else if (parent instanceof FileTransferClientHandler) {
			this.waitForInitiate = true;
			this.name = ((FileTransferClientHandler) parent).getName() 
					+ "Uploader-" + fileToRead.getName();
		} else {
			this.name = "Uploader-" + fileToRead.getName();
			this.showNamedError("Unknown parent object type!");
			this.showNamedError("Will wait for initalise, to be sure... ");
			this.waitForInitiate = true; 
		}
		
		this.totalFileSize = totalFileSize;
		this.fileToRead = fileToRead;
		this.complete = false;
		this.paused = false;
		
		this.startTime = System.nanoTime();
		this.duration = 0;
		
		this.textUI = new userInterface.TUI();
		
		this.packetList = new ArrayList<Packet>();

		this.LAR = -1;
		this.SWS = 10; // MAY NOT EXCEED (FileTransferProtocol.MAX_ID + 1)/ 2 - 1;

		this.startID = 0; // TODO for now always starting at zero, because of some weird behaviour
		// would be better to start at random: 
		// = new Random().nextInt((FileTransferProtocol.MAX_ID) + 1); // zero to max_ID
		
		this.totalResendPackets = 0;
		this.thresholdResend = 25; 
		
		this.filePointer = 0;
	}

	/**
	 * Upload file.
	 */
	@Override
	public void run() {
		this.showNamedMessage("Starting upload helper...");
		this.readFile();
		
		this.waitForInitiate();

		this.showNamedMessage("Starting byte transfer...");
		this.totalAckPackets = 0;
		this.currentPacketToSend = 0;
		this.idWrapCounter = 0;
		this.transferBytes();
		
		this.showNamedMessage("File send completely");
		this.duration += System.nanoTime() - this.startTime;
		this.showStats();
		this.showNamedMessage("Upload complete: helper shutting down");
		this.shutdown();

		
	}

	/**
	 * Read contents of File object into uploader byte[].
	 */
	public void readFile() {
		try {
			this.fileContents = util.FileOperations.getFileContents(this.fileToRead);
		} catch (IOException e) {
			this.showNamedError("Reading file failed: " + e.getLocalizedMessage());
			this.showNamedError("Cannot continue to upload: going to shutdown");
			this.shutdown();
		}

		this.totalPackets = (int) 
				Math.ceil(fileContents.length / FileTransferProtocol.MAX_PAYLOAD_LENGTH) + 1;
		this.showNamedMessage("Total number of packets to send: " + this.totalPackets);

	}
	
	/** 
	 * Wait for initiation by downloader,
	 *  (may be needed to let downloader open a way through Firewall(s) first).
	 */
	public void waitForInitiate() {
		if (waitForInitiate) {
			this.showNamedMessage("Waiting for initiation by downloader...");
			boolean proceed = false;

			while (!proceed) {
				try {
					Packet receivedPacket = TransportLayer.receivePacket(this.uploadSocket);

					if (!this.checkSource(receivedPacket)) {
						continue;
					}

					if (Arrays.equals(receivedPacket.getPayloadBytes(), 
							FileTransferProtocol.START_DOWNLOAD)) {
						proceed = true;
					} else {
						this.showNamedError("Unknown packet received: " 
								+ new String(receivedPacket.getPayload()));
					}
				} catch (IOException | PacketException | UtilDatagramException e) {
					this.showNamedError("Receiving packet failed: " + e.getLocalizedMessage());
					this.showNamedError("Uploader continues, but may have missed initiation!");
				}
			}
			this.showNamedMessage("Downloader initiated upload!");
		}
	}
	
	/**
	 * Transfer the byte[] of the File to the downloader, contained in Packets.
	 */
	public void transferBytes() {
		// TODO when running on client, also here use a progress bar (first solve repeating issue)
		
		while (!(filePointer >= fileContents.length && totalAckPackets == totalPackets)) { 
			// while not (reached end of the file AND all packets are acknowledged)

			if ((currentPacketToSend <= LAR + SWS // inside send window size = send the packet
					&& currentPacketToSend < totalPackets)
					&& !this.paused) { // if paused only listen 
				this.sendNextPacket();
			} else {
				this.listenForAck();
			}
		}
		this.showNamedMessage("Sending completed!"); 
		
		this.complete = true;
	}
	
	/**
	 * Send next packet to the downloader.
	 */
	public void sendNextPacket() {
		int packetID = nrToId(this.currentPacketToSend);
		
		if (packetID == 0 && this.currentPacketToSend != 0) {
			this.idWrapCounter++;
			//this.showNamedMessage("packet ID wrap around"); // for debugging
		}
		
		this.sendBytesToDownloader(packetID, generatePayload()); 

		filePointer += Math.min(FileTransferProtocol.MAX_PAYLOAD_LENGTH,
				fileContents.length - filePointer); 
		
		if (waitForInitiate) { // running on server: more textual output
			this.showNamedMessage("Packet " + currentPacketToSend 
					+ " with ID = " + packetID + " send..");
		}
		currentPacketToSend++;
	}
	
	/**
	 * Listen for ACK-packet from the downloader, and process it.
	 */
	public void listenForAck() {
		try {
			if (!this.paused) {
				if (waitForInitiate) { // running on server: more textual output
					this.showNamedMessage("Listening for ACK(s)...");
				}
			}
			
			boolean ackReceived = false;

			while (!ackReceived) {
				Packet receivedPacket = TransportLayer.receivePacket(this.uploadSocket);
				
				if (receivedPacket == null) {
					this.showNamedError("NULL packet received");
					continue;
				} else if (!this.checkSource(receivedPacket)) {
					continue;
				}

				if (Arrays.equals(receivedPacket.getPayloadBytes(), FileTransferProtocol.ACK)) {
					int packetNr = this.idToNr(receivedPacket.getId());
					LAR = packetNr;
					this.setPacketAck(packetNr);
					ackReceived = true;
				} else if (Arrays.equals(receivedPacket.getPayloadBytes(),
						FileTransferProtocol.PAUSE_DOWNLOAD)) {
					this.pause();
				} else if (Arrays.equals(receivedPacket.getPayloadBytes(),
						FileTransferProtocol.RESUME_DOWNLOAD)) {
					this.resume();
				} else {
					this.showNamedError("Unknown packet received: " 
							+ new String(receivedPacket.getPayload()));
				}
			}
		} catch (SocketTimeoutException e) {
			// this.showNamedMessage("Socket timed-out: retry receive"); // for debugging
			this.listenForAck();
		} catch (IOException | PacketException | UtilDatagramException e) {
			this.showNamedError("Receiving packet failed: " + e.getLocalizedMessage());
			this.showNamedError("Uploader continues, but may have missed an ACK!");
		}
	}
	
	/**
	 * Generate payload for next packet to send.
	 * @return byte[] payload for next packet
	 */
	public byte[] generatePayload() {
		int datalen = Math.min(FileTransferProtocol.MAX_PAYLOAD_LENGTH,
				fileContents.length - filePointer);
		
		byte[] payload = new byte[datalen];
		System.arraycopy(fileContents, filePointer, payload, 0, datalen); 

		return payload;
	}

	/**
	 * Set a Packet from the packetList to acknowledged.
	 * @param nrToAck of packet to set to acknowledged
	 */
	public void setPacketAck(int nrToAck) {
		boolean found = false;
		Packet p = this.packetList.get(nrToAck); 
		if (p.getId() == nrToId(nrToAck)) { // check if no shift in list
			if (!p.isAck()) {
				p.setAck(true);
				
				if (waitForInitiate) { // running on server: more textual output
					this.showNamedMessage("Packet " + nrToAck + " ACKed!");
				}
				
				totalAckPackets++;
			} else {
				
				if (waitForInitiate) { // running on server: more textual output
					this.showNamedMessage("Packet " + nrToAck + " was already ACKed: duplicate!");
				}
			}
			found = true;
		}

		if (!found) {
			if (waitForInitiate) { // running on server: more textual output
				this.showNamedError("Packet with number " + nrToAck + " not found!");
			}
		}
	}

	/**
	 * Send byte[] to the corresponding downloadHelper, contained in a Packet.
	 * @param id of the Packet to send.
	 * @param bytesToSend to downloader
	 */
	public void sendBytesToDownloader(int id, byte[] bytesToSend) {
		try { // to construct and send a packet
			Packet packet = new Packet(
					id,
					this.uploadSocket.getLocalAddress(),
					this.uploadSocket.getLocalPort(), 
					this.downloaderAddress, 
					this.downloaderPort,
					bytesToSend
					);

			this.sendPacketToDownloader(packet);

			packetList.add(packet);

			//this.showNamedMessage("Bytes send!"); // for debugging 

		} catch (PacketException e) {
			this.showNamedError("Sending packet failed: " + e.getLocalizedMessage());
			this.showNamedError("Uploader continues, but something may be missing!");
		}
	}

	/**
	 * Send packet to the corresponding downloadHelper.
	 * @param packet to send to downloader.
	 */
	public void sendPacketToDownloader(Packet packet) {
		try {
			TransportLayer.sendPacket(
					this.uploadSocket,
					packet,
					this.downloaderPort
			);
		} catch (IOException | UtilByteException | UtilDatagramException e) {
			this.showNamedError("Sending packet failed: " + e.getLocalizedMessage());
			this.showNamedError("Uploader continues, but something may be missing!");
		}
		
		util.TimeOut.setTimeOut(1000, this, packet); 
	}
		
	/**
	 * If time-out elapsed and packet is not acknowledged: resend packet.	
	 * @param tag Object that called timeoutElapsed	    
	 */
	@Override
	public void timeoutElapsed(Object tag) {
		Packet packet = (Packet) tag;
		if (!packet.isAck()) {
			if (waitForInitiate) { // running on server: more textual output
				this.showNamedMessage("TIME OUT packet with ID = " 
						+ packet.getId() + " without ACK: resend!");
			}
			sendPacketToDownloader(packet);
			
			this.restrictResend(packet);
		}
	}
	
	/**
	 * Stops resending timed-out packets when number of resends is above threshold.
	 * @param packet that is being resend
	 */
	public synchronized void restrictResend(Packet packet) {
		this.totalResendPackets++;
		
		int currentResendRatio = this.totalResendPackets / this.totalPackets * 100;
		if (currentResendRatio > this.thresholdResend) {
			this.showNamedError("Relative packet resend ratio of " + currentResendRatio 
					+ " is above threshold (" + this.thresholdResend + "):"
							+ " network too unreliable = aborting transfer");
			this.showNamedError("Cannot continue to upload: going to shutdown");
			this.shutdown();

			packet.setAck(true); // TODO not actually true, abort timeout in other way!
		}
	}
		
	/**
	 * Check if source of Packet is the corresponding downloader.
	 * @param receivedPacket to check
	 * @return true if source if the corresponding downloader, and false if not.
	 */
	public boolean checkSource(Packet receivedPacket) {
		if (!(receivedPacket.getSourceAddress().equals(this.downloaderAddress) 
				&& receivedPacket.getSourcePort() == this.downloaderPort)) { 
			this.showNamedError("SECURITY WARNING: this response is NOT"
					+ " coming for known downloader > dropping it");
			return false;
		} else {
			return true;
		}
	}
	
	
	public void setDownloaderPort(int downloaderPort) {
		this.downloaderPort = downloaderPort;
	}

	/**
	 * Pause this uploader.
	 * Note: downloader will wait and know implicitly; maybe notify explicitly?
	 */
	public synchronized void pause() {
		this.paused = true;
		this.duration += System.nanoTime() - this.startTime;

		try {
			this.uploadSocket.setSoTimeout(1000); 
			// otherwise, thread will block in .receive and not be able to get local resume command
		} catch (SocketException e) {
			this.showNamedError("Setting socket time-out failed: " + e.getLocalizedMessage());
			this.showNamedError("Uploader continues, "
					+ "but may not continue while waiting for RESUME packet!");
		} 
		this.showNamedMessage("=PAUSED");
	}
	
	/**
	 * Resumes this uploader.
	 * Note: downloader will receive packets and know implicitly; maybe notify explicitly? 
	 */
	public synchronized void resume() {
		this.paused = false;
		this.startTime = System.nanoTime(); // restart timer

		try {
			this.uploadSocket.setSoTimeout(0); // revert socket to default operation
		} catch (SocketException e) {
			this.showNamedError("Removing socket time-out failed: " + e.getLocalizedMessage());
			this.showNamedError("Uploader continues, but socket may time out!");
		} 
		
		this.sendNextPacket(); // otherwise there will be no new ACK, to open sender window
		
		this.showNamedMessage("=RESUMED");
	}
	
	public boolean isPaused() {
		return this.paused;
	}
	
	/**
	 * Check if helper has closed its socket (= shutdown).
	 * @return true if socket is closed
	 */
	public boolean isSocketClosed() {
		return this.uploadSocket.isClosed();
	}
	
	/**
	 * Show statistic of this uploader.
	 */
	public void showStats() {
		this.showNamedMessage("Transferred file: " + fileToRead.getName());
		this.showNamedMessage("Transfer complete: " + this.complete);
		this.showNamedMessage("-------------------------------->");
		this.showNamedMessage("Total file size: " + this.totalFileSize + " bytes");
		this.showNamedMessage("Transfer duration: " + (this.duration * 1e-6) + " milliseconds"); 
		this.showNamedMessage("Average transferspeed: " 
				+ (this.totalFileSize / (this.duration * 1e-9)) + " bytes/second"); 
		this.showNamedMessage("Number of resend packets: " + this.totalResendPackets);
		this.showNamedMessage("--------------------------------<");
	}
	
	/**
	 * Shutdown this uploadHelper (displays warning if upload not complete).
	 */
	public void shutdown() {
		if (!this.complete) {
			this.showNamedError("WARNING! preliminairy shutdown: transfer not complete!");
		}
		this.showNamedMessage("Helper is shutting down.");
		this.uploadSocket.close();
	}
	
	public int getStartId() {
		return startID;
	}

	/**
	 * Show message on the textUIT with name of this uploadHelper.
	 * @param message to display
	 */
	public void showNamedMessage(String message) {
		textUI.showNamedMessage(this.name, message);
	}
	
	/**
	 * Show error on the textUIT with name of this uploadHelper.
	 * @param message to display
	 */
	public void showNamedError(String message) {
		textUI.showNamedError(this.name, message);
	}
	
	@Override 
	public String toString() {
		return this.name;
	}
	
	// private methods ----------------------------------------------------------
	
	/**
	 * Convert packet number to packet ID.
	 * @param packetNumber to convert
	 * @return int corresponding packet ID
	 */
	private int nrToId(int packetNumber) {
		return (packetNumber + this.startID) % FileTransferProtocol.MAX_ID;
	}

	/**
	 * Convert packet ID to packet number.
	 * @param packetID to convert
	 * @return corresponding packet number
	 */
	private int idToNr(int packetID) {
		int unwrappedId = -1;

		int correctedId = packetID;
		
		// determine in which range the ID is most likely coming from:
		int previousWraparoundRangeID = correctedId 
				+ (this.idWrapCounter - 1) * FileTransferProtocol.MAX_ID;
		int currentWraparoundRangeID = correctedId 
				+ (this.idWrapCounter) * FileTransferProtocol.MAX_ID;

		if (!(this.LAR > previousWraparoundRangeID)) { 
			// check if packet from previous wraparound is already ACKed (= expected earlier)
			unwrappedId = previousWraparoundRangeID;
		} else if (this.currentPacketToSend > currentWraparoundRangeID) {
			// check if packet could be sent in this wraparound (= this possible)
			unwrappedId = currentWraparoundRangeID;
		} else {
			this.showNamedError("Something weird happend while wrapping around packet IDs");
			this.shutdown();
		}
		return unwrappedId - this.startID;
	}
	
}