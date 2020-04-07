package network;

import java.net.InetAddress;

import exceptions.PacketException;
import protocol.FileTransferProtocol;

public class Packet {

	/**
	 * TODO
	 */
	private InetAddress sourceAddress;
	private InetAddress destinationAddress;
	
	/**
	 * TODO
	 */
	private int sourcePort;
	private int destinationPort;

	/**
	 * TODO
	 */
	private int id;
	private boolean ack;
	
	/**
	 * TODO
	 */
	private byte[] payload; // was Integer[]
	
	/**
	* TODO
	*/
	private int payloadLength;
	
	public Packet(int id, InetAddress sourceAddress, int sourcePort, InetAddress destinationAddress,
			int destinationPort, byte[] payload) throws PacketException {
		this.id = id;
		this.setSourceAddress(sourceAddress);
		this.setSourcePort(sourcePort);
		this.setDestinationAddress(destinationAddress);
		this.setDestinationPort(destinationPort);
		this.ack = false;
		
		this.setPayload(payload); 
		// note: do not assign directly, because lenght will not be set! TODO
	}
	
	public int getId() {
		return id;
	}
	
	public InetAddress getSourceAddress() {
		return sourceAddress;
	}

	public void setSourceAddress(InetAddress sourceAddress) {
		this.sourceAddress = sourceAddress;
	}

	public InetAddress getDestinationAddress() {
		return destinationAddress;
	}

	public void setDestinationAddress(InetAddress destinationAddress) {
		this.destinationAddress = destinationAddress;
	}	
	
	public int getSourcePort() {
		return sourcePort;
	}

	public void setSourcePort(int sourcePort) {
		this.sourcePort = sourcePort;
	}

	public int getDestinationPort() {
		return destinationPort;
	}

	public void setDestinationPort(int destinationPort) {
		this.destinationPort = destinationPort;
	}	
	
	public boolean isAck() {
		return ack;
	}
	public void setAck(boolean ack) {
		this.ack = ack;
	}
	public byte[] getPayload() {
		return payload;
	}
	public int getPayloadLength() {
		return payloadLength;
	}
	
	public void setPayload(byte[] payload) throws PacketException {
		int length = payload.length;
		
		if (length <= FileTransferProtocol.MAX_PAYLOAD_LENGTH) {
			this.payloadLength = length;
			this.payload = payload;
		} else {
			throw new PacketException("Cannot create packet: payload too large");
		}
		
	}
	
}
