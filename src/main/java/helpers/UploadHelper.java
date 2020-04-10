package helpers;

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
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


/**
 * TODO maybe generalise to downloadHelper (both client and server)??
 * @author huub.lievestro
 *
 */
public class UploadHelper implements Runnable, util.ITimeoutEventHandler { 

	/**
	 * Coonected process TODO
	 */
	private Object parent;

	/**
	 * Socket used for download TODO
	 */
	private DatagramSocket uploadSocket;

	/**
	 * Address used by uploader TODO
	 */
	private InetAddress downloaderAddress;

	/**
	 * Port used by uploader TODO
	 */
	private int downloaderPort;

	/**
	 * Indicate running on client side: need to initiate (uploader will wait)
	 */
	private boolean waitForInitiate;

	/**
	 * TODO: send only once, not every packet? 
	 */
	private int totalFileSize;

	/**
	 * TODO
	 */
	File fileToRead;

	/**
	 * Indicating download complete TODO
	 */
	private boolean complete;

	/** 
	 * TODO
	 */
	private List<Packet> packetList;

	/** 
	 * TODO
	 */
	private int LAR;
	private int SWS;

	/**
	 * keep track of where we are in the data TODO
	 */
	private int filePointer;
	
	/**
	 * TODO
	 */
	private byte[] fileContents;
	
	/** 
	 * Number of packets to send
	 */
	private int totalPackets;
	
	/**
	 * TODO
	 */
	private int totalAckPackets;
	private int currentPacketToSend;
	
	private String name;
	private UI.TUI TUI;

	/**
	 * TODO
	 */
	private int totalResendPackets;
	
	/**
	 * TODO threshold of fraction of resend packets relative to total packets to transmit, in percentage
	 * If above ... 
	 */
	private int thresholdResend;
	

	/** TODO
	 * @param parent
	 * @param downloadSocket
	 * @param uploaderAddress
	 * @param uploaderPort
	 * @param initiate
	 */
	public UploadHelper(Object parent, DatagramSocket uploadSocket, InetAddress downloaderAddress,
			int downloaderPort, int totalFileSize, File fileToRead) {
		super();
		this.parent = parent;
		this.uploadSocket = uploadSocket;
		this.downloaderAddress = downloaderAddress;
		this.downloaderPort = downloaderPort;
		
		//this.waitForInitiate = waitForInitiate;
		if (parent instanceof FileTransferClient) { // TODO or just input manually?
			this.waitForInitiate = false;
			this.name = ((FileTransferClient) parent).getName() + "_Downloader-" + fileToRead.getName();
		} else if (parent instanceof FileTransferClientHandler) {
			this.waitForInitiate = true;
			this.name = ((FileTransferClientHandler) parent).getName() + "_Downloader-" + fileToRead.getName();
		} else {
			this.name = "Uploader-" + fileToRead.getName();
			this.showNamedError("Unknown parent object type!");
			// TODO set wait for initialise or not? 
		}
		
		this.totalFileSize = totalFileSize;
		this.fileToRead = fileToRead;
		this.complete = false;
		
		this.TUI = new UI.TUI();
		

		this.packetList = new ArrayList<Packet>();

		LAR = -1;
		SWS = 1;

		this.totalResendPackets = 0;
		this.thresholdResend = 25; // TODO explain in report!
		
		this.filePointer = 0;
		
	}

	@Override
	public void run() {
		this.showNamedMessage("Starting upload helper...");
		this.readFile();
		
		this.waitForInitiate();

		totalAckPackets = 0;
		currentPacketToSend = 0;

		
		this.showNamedMessage("Starting byte transfer...");
		this.transferBytes();
		
	}

	public void readFile() {
		// read from the input file
		//Integer[] fileContents = Utils.getFileContents(getFileID());
		try {
			this.fileContents = util.FileOperations.getFileContents(this.fileToRead);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			this.showNamedError("Reading file failed!");
			e.printStackTrace();
		}

		this.totalPackets = (int) Math.ceil(fileContents.length/FileTransferProtocol.MAX_PAYLOAD_LENGTH);
		this.showNamedMessage("Total number of packets to send: " + this.totalPackets);
		if (this.totalPackets > 256) {
			this.showNamedError("!! WARNING THIS IS NOT IMPLEMENTED YET!!");
		}
	}
	
	/** 
	 * TODO
	 */
	public void waitForInitiate() {
		if (waitForInitiate) {
			this.showNamedMessage("Waiting for initiation by downloader...");
			boolean proceed = false;

			while (!proceed) {
				try {
					Packet receivedPacket = TransportLayer.receivePacket(this.uploadSocket);

					if (!(receivedPacket.getSourceAddress().equals(this.downloaderAddress) // TODO make seperate method?
							&& receivedPacket.getSourcePort() == this.downloaderPort)) { 
						this.showNamedError("SECURITY WARNING: this response is NOT"
								+ " coming for known downloader > dropping it");
						continue;
					}
					
					System.out.println("DEBUG"); // TODO
					System.out.println(Arrays.toString(receivedPacket.getPayloadBytes()));
					System.out.println(Arrays.toString(FileTransferProtocol.START_DOWNLOAD));
					
					
					if (Arrays.equals(receivedPacket.getPayloadBytes(), 
							FileTransferProtocol.START_DOWNLOAD)) {
						proceed = true;
					} else {
						this.showNamedError("Unknown packet received: " 
								+ new String(receivedPacket.getPayload())); // TODO payload parse? 
					}
				} catch (IOException | PacketException | UtilDatagramException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			this.showNamedMessage("Download initiated!");
		}
	}
	
	public void transferBytes() {
		while (!(filePointer >= fileContents.length && totalAckPackets == totalPackets)) {
			// while not (reached end of the file AND all packets are acknowledged)

			if (currentPacketToSend <= LAR + SWS && currentPacketToSend <= totalPackets) { 
				this.sendNextPacket();
			} else {
				this.listenForAck();
			}
		}
		this.showNamedMessage("Sending completed!"); 
	}
	
	public void sendNextPacket() {
		// inside send window size = send the packet
		int packetID = currentPacketToSend;
		
		this.sendBytesToDownloader(packetID, generatePayload()); 
		// TODO reduce with instance var: fileContents, filePointer

		filePointer += Math.min(FileTransferProtocol.MAX_PAYLOAD_LENGTH,
				fileContents.length - filePointer); // datalen
		
		this.showNamedMessage("Packet " + packetID + " send..");
		currentPacketToSend++;
	}
	
	public void listenForAck() {
		try {
			this.showNamedMessage("Listening for ACK(s)...");

			boolean ackReceived = false;

			while (!ackReceived) {
				
				//if the socket does not receive anything in 1 second, 
				//it will timeout and throw a SocketTimeoutException
				//you can catch the exception if you need to log, or you can ignore it
				// this.uploadSocket.setSoTimeout(1000); // TODO
				
				Packet receivedPacket = TransportLayer.receivePacket(this.uploadSocket);
				
				if (!(receivedPacket.getSourceAddress().equals(this.downloaderAddress) // TODO make seperate method?
						&& receivedPacket.getSourcePort() == this.downloaderPort)) { 
					this.showNamedError("SECURITY WARNING: this response is NOT"
							+ " coming for known downloader > dropping it");
					continue;
				}
				
				if (receivedPacket != null && // TODO null is now bit superfluous
						Arrays.equals(receivedPacket.getPayloadBytes(), FileTransferProtocol.ACK)) {

					int receivedId = receivedPacket.getId();
					LAR = receivedId;
					this.setPacketAck(receivedId);
					
					ackReceived = true;
				} else {
					this.showNamedError("Unknown packet received: " 
							+ new String(receivedPacket.getPayload())); // TODO payload parse?
				}
			}

//		} catch (InterruptedException e) { // on TimeOut
//			// let is resend
//			// resume listening
		} catch (SocketTimeoutException e) {
			this.showNamedMessage("Socket timed-out: retry receive");
			this.listenForAck();
		} catch (IOException | PacketException | UtilDatagramException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public byte[] generatePayload() { // TOD instance var byte[] fileContents, int filePointer
		int datalen = Math.min(FileTransferProtocol.MAX_PAYLOAD_LENGTH,
				fileContents.length - filePointer);
//		Integer[] pkt = new Integer[HEADERSIZE + datalen];
		byte[] payload = new byte[datalen];

//		// write file size into the header byte TODO not include every packet? 
//		int fileSize = fileContents.length; 
//		ByteBuffer b = ByteBuffer.allocate(4);
//		b.putInt(fileSize); // using Big Endian! 
//		byte[] fileSizeBytes = b.array();
//		Integer[] fileSizeInBytes = new Integer[MAX_FILE_SIZE_BYTES];
//		for (int i = 0; i < MAX_FILE_SIZE_BYTES; i++) {
//			fileSizeInBytes[i] = Byte.valueOf(fileSizeBytes[i]).intValue();
//		}

		// copy databytes from the input file into data part of the packet, i.e., the payload
		System.arraycopy(fileContents, filePointer, payload, 0, datalen); 
		// TODO HEADERSIZE replaced by zero

		return payload;
	}

	public void setPacketAck(int idToAck) {
		boolean found = false;
		for (Packet p : packetList) { 
			if (p.getId() == idToAck) {
				p.setAck(true);
				this.showNamedMessage("Packet " + idToAck + " ACKed!");
				totalAckPackets++;
				found = true;
			}
		}
		if (!found) {
			this.showNamedError("Packet with ID = " + idToAck + " not found!");
		}
	}

	public void sendBytesToDownloader(int id, byte[] bytesToSend) { // TODO put in seperate utility?
		try { // to construct and send a packet
			Packet packet = new Packet(
					id,
					this.uploadSocket.getLocalAddress(), // TODO request once and store? pass on?
					this.uploadSocket.getLocalPort(), 
					this.downloaderAddress, 
					this.downloaderPort,
					bytesToSend
					); // TODO only sending bytes, so no byteOffset

			this.sendPacketToDownloader(packet);

			packetList.add(packet); // TODO from loop

			this.showNamedMessage("Bytes send!"); // TODO 

		} catch (PacketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void sendPacketToDownloader(Packet packet) {
		try {
			TransportLayer.sendPacket(
					this.uploadSocket,
					packet,
					this.downloaderPort
					);
		} catch (IOException | UtilByteException | UtilDatagramException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		util.TimeOut.setTimeOut(1000, this, packet); 
		// TODO let it break after certain number of resends?
	}
		
			    
	@Override
	public void TimeoutElapsed(Object tag) {
		Packet packet = (Packet) tag;
		if (!packet.isAck()) {
			//Thread.interrupt();// TODO how to get thread to resend while waiting on this ACK?
			this.showNamedMessage("TIME OUT packet " + packet.getId() + " without ACK: resend!");
			sendPacketToDownloader(packet);
			 this.restrictResend(packet);
		}
	}
	
	public synchronized void restrictResend(Packet packet) {
		this.totalResendPackets++; // TODO is this thread safe? make it sync? is it a problem here?
		
		int currentResendRatio = this.totalResendPackets / this.totalPackets * 100;
		if ( currentResendRatio > this.thresholdResend) {
			this.showNamedError("Relative packet resend ratio of " + currentResendRatio 
					+ " is above threshold (" + this.thresholdResend + "): network too unreliable = aborting transfer");
		
			this.complete = true; // TODO do something else, to make it stop
			// TODO handle this
			// TODO and let downloader know!
			
			packet.setAck(true); // TODO end timeout in other way!
		
		}
		
		
	}
		

	
	public void setDownloaderPort(int downloaderPort) {
		this.downloaderPort = downloaderPort;
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