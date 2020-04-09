package network;

import java.net.InetAddress;
import java.util.Arrays;

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
	
	/**
	 * TODO Indicating when some String is preceding the bytes
	 */
	private int byteOffset;
	
	/**
	 * TODO
	 * @param id
	 * @param sourceAddress
	 * @param sourcePort
	 * @param destinationAddress
	 * @param destinationPort
	 * @param payload
	 * @throws PacketException
	 */
	
	public Packet(int id, InetAddress sourceAddress, int sourcePort, InetAddress destinationAddress,
			int destinationPort, byte[] payload, int byteOffset) throws PacketException {
		this.id = id;
		this.setSourceAddress(sourceAddress);
		this.setSourcePort(sourcePort);
		this.setDestinationAddress(destinationAddress);
		this.setDestinationPort(destinationPort);
		this.ack = false;
		
		this.setPayload(payload); 
		// note: do not assign directly, because lenght will not be set! TODO
		
		this.byteOffset = byteOffset;
	}
	
	/**
	 * TODO create Packet witt only bytes
	 * @param id
	 * @param sourceAddress
	 * @param sourcePort
	 * @param destinationAddress
	 * @param destinationPort
	 * @param payload
	 * @throws PacketException
	 */
	public Packet(int id, InetAddress sourceAddress, int sourcePort, InetAddress destinationAddress,
			int destinationPort, byte[] payload) throws PacketException {
		this (id,  sourceAddress,  sourcePort,  destinationAddress, destinationPort, payload, 0);
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
	
	public int getByteOffset() {
		return byteOffset;
	}

	/**
	 * TODO as a sort of "toString", still concerning one packet and placing it here makes naming much easier
	 * @param 
	 * @return
	 */
	public String getPayloadString() {
		if (this.byteOffset == 0) {
			return "";
		} else {
			byte[] stringBytes = Arrays.copyOfRange(this.getPayload(), 0, this.byteOffset);
			// TODO: not minus one, because to is exclusive
			return new String(stringBytes);
		}
	}
	
	/**
	 * TODO as a sort of "toString", still concerning one packet and placing it here makes naming much easier
	 * @param 
	 * @return
	 */
	public byte[] getPayloadBytes() {
		if (this.byteOffset > this.payloadLength) {
			return null;
		} else {
			return Arrays.copyOfRange(this.getPayload(), this.byteOffset, this.payloadLength + 1);
			// TODO note to is exclusive
		}
	}
	
}
