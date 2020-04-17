package helpers;

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import client.FileTransferClient;
import exceptions.PacketException;
import exceptions.UtilByteException;
import exceptions.UtilDatagramException;
import me.tongfei.progressbar.*;
import network.Packet;
import network.TransportLayer;
import protocol.FileTransferProtocol;
import server.FileTransferClientHandler;


/**
 * DownloadHelper to download a file.
 *  * @author huub.lievestro
 *
 */
public class DownloadHelper implements Helper, Runnable, util.ITimeoutEventHandler {

	/**
	 * Connected process, which started this helper.
	 */
	private Object parent;

	/**
	 * Socket used for download.
	 */
	private DatagramSocket downloadSocket;

	/**
	 * Address used by uploader, to download from.
	 */
	private InetAddress uploaderAddress;

	/**
	 * Port used by uploader, to download from.
	 */
	private int uploaderPort;

	/**
	 * Indicate running on client side: need to initiate (uploader will wait)
	 * Note: this may be needed to let downloader open a way through Firewall(s) first.
	 */
	private boolean initiate;

	/**
	 * Total file size to download.
	 */
	private long totalFileSize;
	
	/**
	 * Number of packets to receive in total.
	 */
	private int totalPackets;
	
	/**
	 * File object to download and write.
	 */
	File fileToWrite;
	
	/**
	 * Indicating download complete.
	 */
	private boolean complete;
	
	/**
	 * Indicate if paused or not.
	 */
	private boolean paused;
	
	/**
	 * (re)start time of download.
	 */
	long startTime;
	
	
	/**
	 * Duration of transfer, in nanoseconds.
	 */
	long duration;

	/**
	 * List of received packets (expected packets are included as null.
	 */
	private List<Packet> receivedPacketList;

	/**
	 * Last Frame Received, from uploader. 
	 */
	private int LFR;
	
	/**
	 * Receive Window Size,
	 * represent how many out-of-order packets this receiver is willing to buffer.
	 * Note: makes no sense to set >SWS: impossible for >SWS packets to arrive out of order
	 */
	private int RWS;
	
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
	 * Number of packets currently dropped, 
	 * because they were out of receive window (not because security or other reasons).
	 */
	private int droppedPackets;
	
	/**
	 * List of Packets needing an acknowledgement, 
	 * because packet loss is not covered by retry from uploader.
	 */
	private List<Packet> packetNeedAck;
	
	/**
	 * Index of packets needing Ack, being acknowledged by receiving any further packet.
	 */
	private int indexAcked;;
	
	/**
	 * Number of packets currently resend.
	 * (including duplicate resends)
	 */
	private int totalResendPackets;
	
	/**
	 * Threshold of fraction of resend packets relative to total packets to transmit, in percentage
	 * If above this threshold, the download will be aborted due to too unstable network. 
	 */
	private int thresholdResend;
	
	/**
	 * Contents of the file, received from the uploader and to write on disk.
	 */
	private byte[] fileContents;
	
	/**
	 * Name of this downloadHelper (mainly for printing named messages).
	 */
	private String name;
	
	/**
	 * TUI, to provide status messages to user.
	 */
	private userInterface.TUI textUI;

	/**
	 * Create a new UploadHelper, and initialise instance variables.
	 * @param parent creating this DownloadHelper
	 * @param downloadSocket to use
	 * @param uploaderAddress to download from
	 * @param uploaderPort to download from
	 * @param totalFileSize to download
	 * @param fileToWrite to receive and write
	 * @param startID where te uploader will start
	 */
	public DownloadHelper(Object parent, DatagramSocket downloadSocket,
			InetAddress uploaderAddress, int uploaderPort,
			long totalFileSize, File fileToWrite, int startID) {
		this.parent = parent;
		this.downloadSocket = downloadSocket;
		this.uploaderAddress = uploaderAddress;
		this.uploaderPort = uploaderPort;
		
		if (parent instanceof FileTransferClient) { 
			this.initiate = true;
			this.name = ((FileTransferClient) parent).getName() 
					+ "_Downloader-" + fileToWrite.getName();
		} else if (parent instanceof FileTransferClientHandler) {
			this.initiate = false;
			this.name = ((FileTransferClientHandler) parent).getName() 
					+ "_Downloader-" + fileToWrite.getName();
		} else {
			this.name = "Downloader-" + fileToWrite.getName();
			this.showNamedError("Unknown parent object type!");
			this.showNamedError("Will iniitalise, to be sure... ");
			this.initiate = true;  
		}
		
		this.totalFileSize = totalFileSize;
		this.fileToWrite = fileToWrite;
		this.complete = false;
		
		this.paused = false; 
		this.startTime = System.nanoTime();
		this.duration = 0;
		
		this.textUI = new userInterface.TUI();

		this.LFR = -1;
		this.RWS = 10; 
		//makes no sense to set >SWS: impossible for >SWS packets to arrive out of order
		
		this.startID = startID;
		if (this.startID < (FileTransferProtocol.MAX_ID - this.RWS)) {
			this.idWrapCounter = 0;
		} else { // already one wraparound in IDs
			this.idWrapCounter = 1;
		}

		this.droppedPackets = 0;
		
		this.totalResendPackets = 0;
		this.thresholdResend = 25;
		this.packetNeedAck = new ArrayList<Packet>();
		this.indexAcked = -1; // no packet acked
		
		fileContents = new byte[0]; 
		// Note: totalSize is known, but this array only keeps actually arrived bytes
	}

	/**
	 * Download file.
	 */
	@Override
	public void run() {
		this.showNamedMessage("Starting download helper...");

		if (initiate) {
			this.initiateTransfer();
		} 

		this.showNamedMessage("Total file size = " + this.totalFileSize + " bytes");
		this.totalPackets = (int)
				Math.ceil(this.totalFileSize / FileTransferProtocol.MAX_PAYLOAD_LENGTH) + 1;
		this.showNamedMessage("Number of packets to receive: " + this.totalPackets);
		this.receivedPacketList = new ArrayList<Packet>(
				Collections.nCopies(this.totalPackets, null));
		
		this.showNamedMessage("Receiving...");

		if (initiate) { // running on a client: show progress bar
			try (ProgressBar pb = new ProgressBar(this.fileToWrite.getName(), this.totalFileSize, 1, 
					System.out, ProgressBarStyle.COLORFUL_UNICODE_BLOCK, " Bytes", 1, false, null)) {
				pb.setExtraMessage("Downloading..."); 

				while (!this.complete) { 
					this.receiveBytes();
					pb.stepTo(this.fileContents.length);
				} 
				pb.setExtraMessage("Done!"); 
			}
		} else { // running on server
			while (!this.complete) {
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

	/** 
	 * Send initiation by downloader,
	 *  (may be needed to let downloader open a way through Firewall(s) first).
	 */
	public void initiateTransfer() {
		this.sendBytesToUploader(0, FileTransferProtocol.START_DOWNLOAD, true);
		// uploader will not retry (opposite to when ack is lost): so require ack 
		this.showNamedMessage("Download initiated...");
	}

	/**
	 * Transfer the byte[] of the File of the uploader, contained in Packets.
	 */
	public void receiveBytes() {
		try { //to receive a packet from the network layer

			Packet receivedPacket = TransportLayer.receivePacket(this.downloadSocket);

			if (receivedPacket != null) {
				if (this.checkSource(receivedPacket)) { // if not: do nothing = drop packet
					this.ackAllPackets();
					this.processPacket(receivedPacket);
					this.checkComplete();
				}
			} else { // wait ~10ms (or however long the OS makes us wait) before trying again
				try {
					this.showNamedError("Receiving packet was null: dropping it and trying again");
					Thread.sleep(10);
				} catch (InterruptedException e) {
					this.showNamedError("INTERRUPTED EXCEPTION occured");
				}
			}
			
		} catch (SocketTimeoutException eTO) {
			if (!initiate) { // running on server: more textual output
				this.showNamedMessage("Socket timed-out: retry receive");
			}
			this.receiveBytes();
		} catch (IOException | PacketException | UtilDatagramException e) {
			this.showNamedError("Receiving packet failed: " + e.getLocalizedMessage());
			this.showNamedError("Downloader continues, but may be something missing!");
		}
	}
	
	/**
	 * Process received Packet with byte[] from uploader.
	 * Security note: source should already be checked when receiving packet!
	 * @param packet to process
	 */
	public void processPacket(Packet receivedPacket) {

		int packetNr = this.idToNr(receivedPacket.getId());

		if (!initiate) { // running on server: more textual output
			this.showNamedMessage("Received packet with ID = " 
					+ receivedPacket.getId() + ", could be nr " + packetNr);
		}

		if (packetNr > LFR && packetNr <= LFR + RWS) { // = inside receive window
			if (!initiate) { // running on server: more textual output
				this.showNamedMessage("Processing packet " + packetNr);
			}

			this.receivedPacketList.add(packetNr, receivedPacket);

			for (int iNext = 0; packetNr + iNext < this.receivedPacketList.size(); iNext++) {
				Packet nextPacket = this.receivedPacketList.get(packetNr + iNext);
				if (nextPacket != null) { // append the packet's data to the fileContents array
					int oldlength = fileContents.length;
					int datalen = nextPacket.getPayloadLength();
					fileContents = Arrays.copyOf(fileContents, oldlength + datalen);
					System.arraycopy(nextPacket.getPayloadBytes(),
							0, fileContents, oldlength, datalen); 
				} else { // set last received to the packet before null packet (= to receive)
					LFR = packetNr + iNext - 1;
					break;
				}
			}
			this.sendAck(packetNr);
		
		} else if (packetNr < LFR) { // this packet was already ACKed, but maybe ACK got lost
			this.sendAck(packetNr); // resend ACK
		
		} else {
			if (initiate) { // running on client: add line break after progress bar
				this.showNamedMessage(" ");
			}
			this.showNamedMessage("DROPPING packet with ID = " + receivedPacket.getId());
			this.droppedPackets++;
			this.sendAck(this.LFR); // resend last ACK
		}
	}

	/**
	 * Send acknowledgement packet to uploader, and pause packet if downloader wants to pause.
	 * Note: also checks for ID wraparound (= going beyond MAX_ID and starting at zero again)
	 * @param nrToAck to uploader
	 */
	public void sendAck(int nrToAck) {
		int packetID = nrToId(nrToAck);
		
		this.sendBytesToUploader(packetID, FileTransferProtocol.ACK, false);
		if (!initiate) { // running on server: more textual output
			this.showNamedMessage("Packet " + nrToAck + " with ID = " + packetID + " ACK send");
		}
		
		// check if ID could wrap around in the new receive window:
		int maxIdToReceived = nrToId(nrToAck + RWS); 
		if (maxIdToReceived == 0) {
			this.idWrapCounter++;
			//this.showNamedMessage("packet ID wrap around"); // for debugging
		}

		if (this.paused) { // let uploader know that downloader wants to pause transfer
			this.sendBytesToUploader(packetID, FileTransferProtocol.PAUSE_DOWNLOAD, false);
		}
	}
	
	/**
	 * Check if the download is complete, by comparing actual received bytes to totalFileSize.
	 * Note: sets instance variable complete to true, doesn't return a boolean.
	 */
	public void checkComplete() {
		if (fileContents.length >= this.totalFileSize) {
			this.complete = true;
		} else {
			this.complete = false;
		}
	}

	/**
	 * Write the received bytes to File (on more permanent storage).
	 * Note: if writing fails, user is asked if helper should try again.
	 */
	public void writeFile() {
		this.showNamedMessage("Writing file contents to file...");
		try {
			util.FileOperations.setFileContents(this.fileContents, this.fileToWrite);
		} catch (IOException e) {
			this.showNamedError("Writing of file failed: " + e.getLocalizedMessage());
			if (this.textUI.getBoolean("Try again?")) {
				this.writeFile();
			} else {
				this.showNamedError("File not written, downloadHelper going to shutdown");
				this.shutdown();
			}
		}
		this.showNamedMessage("... file written to " + this.fileToWrite.getAbsolutePath());
	}
	
	/**
	 * Send byte[] to the corresponding uploadHelper, contained in a Packet,
	 * and, if required, resend this Packet if not acknowledged by any further received packets.
	 * @param id of the Packet to send.
	 * @param bytesToSend to uploader
	 */
	public void sendBytesToUploader(int id, byte[] bytesToSend, boolean requireAck) {
		try { // to construct and send a packet
			Packet packet = new Packet(
					id, 
					this.downloadSocket.getLocalAddress(),
					this.downloadSocket.getLocalPort(), 
					this.uploaderAddress, 
					this.uploaderPort,
					bytesToSend
					);

			this.sendPacketToUploader(packet);
			
			if (requireAck) { // here: Ack = receiving any packet from uploader
				util.TimeOut.setTimeOut(1000, this, packet); 
			}
			
			//this.showNamedMessage("Bytes send!"); // for debugging

		} catch (PacketException e) {
			this.showNamedError("Sending packet failed: " + e.getLocalizedMessage());
			this.showNamedError("Downloader continues, "
					+ "but something (probably an ACK) may be missing!");
		}
	}
	
	/**
	 * Send packet to the corresponding downloadHelper.
	 * @param packet to send to downloader.
	 */
	public void sendPacketToUploader(Packet packet) {
		try {
			TransportLayer.sendPacket(
					this.downloadSocket,
					packet,
					this.uploaderPort
			);
		} catch (IOException | UtilByteException | UtilDatagramException e) {
			this.showNamedError("Sending packet failed: " + e.getLocalizedMessage());
			this.showNamedError("Downloader continues, "
					+ "but something (probably an ACK) may be missing!");
		}
	}
	
	/**
	 * If time-out elapsed and packet is not acknowledged: resend packet.	
	 * @param tag Object that called timeoutElapsed	    
	 */
	@Override
	public void timeoutElapsed(Object tag) {
		Packet packet = (Packet) tag;
		if (!packet.isAck()) {
			this.showNamedMessage("TIME OUT packet with ID = " 
					+ packet.getId() + " without ACK: resend!");
			sendPacketToUploader(packet);
			
			this.restrictResend(packet);
		}
	}
	
	/**
	 * Set ack to true on all packets needing an acknowledgement.
	 * Note: receiving any further packet from uploader is used as implicit ack.
	 */
	private void ackAllPackets() {
		int nrPacketsNeedingAck = this.packetNeedAck.size();
		if (nrPacketsNeedingAck > 0 && this.indexAcked < nrPacketsNeedingAck) {
			for (int i = this.indexAcked + 1; i < nrPacketsNeedingAck; i++) {
				this.packetNeedAck.get(i).setAck(true);
			}
		} 
		// else: no packets in need of an acknoledgement
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
	 * Check if source of Packet is the corresponding uploader.
	 * @param receivedPacket to check
	 * @return true if source if the corresponding uploader, and false if not.
	 */
	public boolean checkSource(Packet receivedPacket) {
		if (!(receivedPacket.getSourceAddress().equals(this.uploaderAddress) 
				&& receivedPacket.getSourcePort() == this.uploaderPort)) { 
			this.showNamedError("SECURITY WARNING: this response is NOT"
					+ " coming for known downloader > dropping it");
			return false;
		} else {
			return true;
		}
	}
	
	public void setUploaderPort(int uploaderPort) {
		this.uploaderPort = uploaderPort;
	}
	
	public void setTotalFileSize(long totalFileSize) {
		this.totalFileSize = totalFileSize;
	}
	
	public void setStartID(int startID) {
		this.startID = startID;
	}

	/**
	 * Pause this downloader.
	 * Note: working indirectly, uploader is asked to pause transfer via a PAUSE packet
	 */
	public synchronized void pause() {
		this.paused = true;
		this.sendBytesToUploader(0, FileTransferProtocol.PAUSE_DOWNLOAD, false);
		this.duration += System.nanoTime() - this.startTime;
		
		try {
			this.downloadSocket.setSoTimeout(1000); 
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
	 * Note: working indirectly, uploader is asked to resume transfer via a RESUME packet
	 */
	public synchronized void resume() {
		this.paused = false;
		this.sendBytesToUploader(0, FileTransferProtocol.RESUME_DOWNLOAD, true); 
		// uploader will not retry (opposite to when ack is lost): require ack 
		this.startTime = System.nanoTime(); // restart times
		
		try {
			this.downloadSocket.setSoTimeout(0); /// revert socket to default operation
		} catch (SocketException e) {
			this.showNamedError("Removing socket time-out failed: " + e.getLocalizedMessage());
			this.showNamedError("Uploader continues, but socket may time out!");
		} 	
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
		return this.downloadSocket.isClosed();
	}
	
	/**
	 * Show statistic of this downloader.
	 */
	public void showStats() {
		this.showNamedMessage("Transferred file: " + fileToWrite.getName());
		this.showNamedMessage("Transfer complete: " + this.complete);
		this.showNamedMessage("-------------------------------->");
		this.showNamedMessage("Total file size: " + this.totalFileSize + " bytes");
		this.showNamedMessage("Transfer duration: " + (this.duration * 1e-6) + " milliseconds"); 
		this.showNamedMessage("Average transferspeed: " 
				+ (this.totalFileSize / (this.duration * 1e-9)) + " bytes/second"); 
		this.showNamedMessage("Number of dropped packets: " + this.droppedPackets);
		this.showNamedMessage("--------------------------------<");
	}
	
	/**
	 * Shutdown this downloadHelper (displays warning if download not complete).
	 */
	public void shutdown() {
		if (!this.complete) {
			this.showNamedError("WARNING! preliminairy shutdown: transfer not complete!");
		}
		this.showNamedMessage("Helper is shutting down.");
		this.downloadSocket.close();
	}
	
	/**
	 * Show message on the textUIT with name of this downloadHelper.
	 * @param message to display
	 */
	public void showNamedMessage(String message) {
		textUI.showNamedMessage(this.name, message);
	}
	
	/**
	 * Show error on the textUIT with name of this downloadHelper.
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
		
		if (!(this.LFR > previousWraparoundRangeID)) { 
			// check if packet from previous wraparound is already received (= expected earlier)
			unwrappedId = previousWraparoundRangeID;
		} else {
			unwrappedId = currentWraparoundRangeID;
		}
		return unwrappedId - this.startID; 
	}
	
}