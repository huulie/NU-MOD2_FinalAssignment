package helpers;

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
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


/**
 * TODO maybe generalise to downloadHelper (both client and server)??
 * @author huub.lievestro
 *
 */
public class UploadHelper implements Runnable {

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
			this.name = ((FileTransferClient) parent).getName() + "_Uploader-" + fileToRead.getName();
		} else if (parent instanceof FileTransferClientHandler) {
			this.waitForInitiate = true;
			this.name = ((FileTransferClientHandler) parent).getName() + "_Uploader-" + fileToRead.getName();
		} else {
			this.name = "Uploader-" + fileToRead.getName();
			this.showNamedError("Unknown parent object type!");
		}
		
		this.totalFileSize = totalFileSize;
		this.fileToRead = fileToRead;
		this.complete = false;
		
		this.TUI = new UI.TUI();
		

		this.packetList = new ArrayList<Packet>();

		LAR = -1;
		SWS = 2;

		this.filePointer = 0;
	}

	@Override
	public void run() {
		this.showNamedMessage("Sending...");
		this.readFile();

		totalAckPackets = 0;
		currentPacketToSend = 0;

		if (waitForInitiate) {
			boolean proceed = false;

			while (!proceed) {
				try {
					Packet receivedPacket = TransportLayer.receivePacket(this.uploadSocket);

					if (Arrays.equals(receivedPacket.getPayloadBytes(),FileTransferProtocol.START_DOWNLOAD)) {
						proceed = true;
					}
				} catch (IOException | PacketException | UtilDatagramException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		while (!(filePointer >= fileContents.length || totalAckPackets == totalPackets)) {
			// while not reached end of the file OR not all packets are acknowledged

			if(currentPacketToSend <= LAR + SWS && currentPacketToSend <= totalPackets) { // inside send window size
				// send the packet
				int packetID = currentPacketToSend;
//				Packet packet = new Packet(packetID,generateContent(packetID, fileContents, filePointer));
//				sendPacket(packet);
//				packetList.add(packet);
				
				this.sendBytesToDownloader(generateContent(packetID, fileContents, filePointer)); // TODO reduce with instance var?

				filePointer += Math.min(FileTransferProtocol.MAX_PAYLOAD_LENGTH, fileContents.length - filePointer); // datalen
				currentPacketToSend++;

			} else {
//				// listen for ACKs
//				Integer[] receivedPkt = null;
//				boolean ackReceived = false;
//				while (!ackReceived) {
//					receivedPkt = getNetworkLayer().receivePacket();
//					if (receivedPkt != null && receivedPkt.length == 1 ) {
//						LAR = receivedPkt[0];
//						for (Packet p : packetList) {
//							if (p.getId() == receivedPkt[0]) {
//								p.setAck(true);
//								this.showNamedMessage("Packet " + receivedPkt[0] + " ACKed!");
//								totalAckPackets++;
//							}
//						}
//						ackReceived = true;
//					}
//				}
//			}
				this.showNamedMessage("LISTENING FOR ACK");
		}

		this.showNamedMessage("Sending completed!"); 
		}
	}


	public byte[] generateContent(int packetID, byte[] fileContents, int filePointer) {
		int datalen = Math.min(FileTransferProtocol.MAX_PAYLOAD_LENGTH, fileContents.length - filePointer);
//		Integer[] pkt = new Integer[HEADERSIZE + datalen];
		byte[] pkt = new byte[datalen];

//		// write file size into the header byte
//		int fileSize = fileContents.length; 
//		ByteBuffer b = ByteBuffer.allocate(4);
//		b.putInt(fileSize); // using Big Endian! 
//		byte[] fileSizeBytes = b.array();
//		Integer[] fileSizeInBytes = new Integer[MAX_FILE_SIZE_BYTES];
//		for (int i = 0; i < MAX_FILE_SIZE_BYTES; i++) {
//			fileSizeInBytes[i] = Byte.valueOf(fileSizeBytes[i]).intValue();
//		}
//
//		// Assign header bytes:
//		pkt[0] = packetID;
//
//		for(int i = ID_BYTES; i < MAX_FILE_SIZE_BYTES+ID_BYTES; i++) {
//			pkt[i] = fileSizeInBytes[i-ID_BYTES];
//		}

		// copy databytes from the input file into data part of the packet, i.e., after the header
		System.arraycopy(fileContents, filePointer, pkt, 0, datalen); // HEADERSIZE replaced by zero

		return pkt;
	}


	public void sendBytesToDownloader(byte[] bytesToSend) { // TODO put in seperate utility?
		try { // to construct and send a packet
			Packet packet = new Packet(
					0, // TODO id
					this.uploadSocket.getLocalAddress(), // TODO request once and store? pass on?
					this.uploadSocket.getLocalPort(), 
					this.downloaderAddress, 
					this.downloaderPort,
					bytesToSend
					);

			TransportLayer.sendPacket(
					this.uploadSocket,
					packet,
					this.downloaderPort
					); 

			this.showNamedMessage("Bytes send!"); // TODO 

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

		// TODO framework.Utils.Timeout.SetTimeout(1000, this, packet);
		//	    
		//	    @Override
		//	    public void TimeoutElapsed(Object tag) {
		//	    	Packet packet = (Packet) tag;
		//	    	if (!packet.isAck()) {
		//	        	this.showNamedMessage("TIME OUT packet " + packet.getId() + " without ACK: resend!");
		//	    		sendPacket(packet);
		//	    	}
		//	    }
		
	}
	

	public void readFile() {
		// read from the input file
		//Integer[] fileContents = Utils.getFileContents(getFileID());
		this.fileContents = util.FileOperations.getFileContents(this.fileToRead);

		this.totalPackets = (int) Math.ceil(fileContents.length/FileTransferProtocol.MAX_PAYLOAD_LENGTH);
		if (this.totalPackets > 256) {
			this.showNamedError("!! WARNING THIS IS NOT IMPLEMENTED YET!!");
		}
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