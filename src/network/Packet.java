package network;

import java.net.DatagramPacket;
import java.net.InetAddress;

public class Packet {

	/**
	 * TODO
	 */
	private InetAddress sourceAddress;
	private InetAddress destinationAddress;

	/**
	 * TODO
	 */
	private int id;
	private boolean ack;
	
	/**
	 * TODO
	 */
	private byte[] payload; // was Integer[]
	
	public Packet(int id, InetAddress sourceAddress, InetAddress destinationAddress,
			byte[] payload) {
		this.id = id;
		this.setSourceAddress(sourceAddress);
		this.setDestinationAddress(destinationAddress);
		this.ack = false;
		this.payload = payload;
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
	
	public boolean isAck() {
		return ack;
	}
	public void setAck(boolean ack) {
		this.ack = ack;
	}
	public byte[] getPayload() {
		return payload;
	}
	public void setPayload(byte[] payload) {
		this.payload = payload;
	}
	
	
	


	
	
	
}
