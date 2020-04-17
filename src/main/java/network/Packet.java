package network;

import java.net.InetAddress;
import java.util.Arrays;

import exceptions.PacketException;
import protocol.FileTransferProtocol;

/**
 * Model of a Packet, 
 * (as extension of a DatagramPacket, which is a final class).
 * @author huub.lievestro
 *
 */
public class Packet {

	/**
	 * Address of source of Packet.
	 */
	private InetAddress sourceAddress;
	
	/**
	 * Address of destination of Packet.
	 */
	private InetAddress destinationAddress;
	
	/**
	 * Port number of source of Packet.
	 */
	private int sourcePort;
	
	/**
	 * Prot number of destination of Packet.
	 */
	private int destinationPort;

	/**
	 * ID of Packet
	 * Note: should remain below MAX_ID from protocol.
	 */
	private int id;
	
	/**
	 * Indicating if Packet is acknowledged (=true) or not.
	 */
	private boolean ack;
	
	/**
	 * Payload of the Packet,
	 * consisting of String followed by byte[] part.
	 */
	private byte[] payload; 
	
	/**
	* Length of payload in bytes.
	*/
	private int payloadLength;
	
	/**
	 * Index of byte where byte payload starts.
	 * Non-zero when some String payload is preceding the bytes
	 */
	private int byteOffset;
	
	/**
	 * Create a Packet.
	 * @param id of new Packet
	 * @param sourceAddress of new Packet
	 * @param sourcePort of new Packet
	 * @param destinationAddress of new Packet
	 * @param destinationPort of new Packet
	 * @param payload of new Packet
	 * @param byteOffset of new Packet
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
		
		this.byteOffset = byteOffset;
	}
	
	/**
	 * Create a Packet, containing only byte[].
	 * @param id of new Packet
	 * @param sourceAddress of new Packet
	 * @param sourcePort of new Packet
	 * @param destinationAddress of new Packet
	 * @param destinationPort of new Packet
	 * @param payload of new Packet
	 * @throws PacketException
	 */
	public Packet(int id, InetAddress sourceAddress, int sourcePort, InetAddress destinationAddress,
			int destinationPort, byte[] payload) throws PacketException {
		this (id,  sourceAddress,  sourcePort,  destinationAddress, destinationPort, payload, 0);
	}
	
	/**
	 * Set the payload of Packet
	 * Note: do NOT set payload field directly: payload length will no be updated!
	 * @param payload to put in Packet
	 * @throws PacketException
	 */
	public void setPayload(byte[] payload) throws PacketException {
		int length = payload.length;
		
		if (length <= FileTransferProtocol.MAX_PAYLOAD_LENGTH) {
			this.payloadLength = length;
			this.payload = payload;
		} else {
			throw new PacketException("Cannot create packet: payload too large");
		}
	}

	/**
	 * Get String payload of Packet (ignoring byte[] part).
	 * @return String part of payload
	 */
	public String getPayloadString() {
		if (this.byteOffset == 0) {
			return "";
		} else {
			byte[] stringBytes = Arrays.copyOfRange(this.getPayload(), 0, this.byteOffset);
			return new String(stringBytes);
		}
	}
	
	/**
	 * Get byte[] payload of Packet (ignoring String part).
	 * @return String part of payload
	 */
	public byte[] getPayloadBytes() {
		if (this.byteOffset > this.payloadLength) {
			return null;
		} else {
			return Arrays.copyOfRange(this.getPayload(), this.byteOffset, this.payloadLength); 
		}
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
	
	public int getByteOffset() {
		return byteOffset;
	}
	
}
