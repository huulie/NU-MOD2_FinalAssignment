package util;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import exceptions.PacketException;
import exceptions.UtilByteException;
import exceptions.UtilDatagramException;
import network.Packet;
import protocol.FileTransferProtocol;

/**
 * Utilities to work with Datagram(Packet)s.
 * Note: the DatagramPacket class could not be extended, because it is a final class
 * @author huub.lievestro
 *
 */
public class Datagram {

	/**
	 * Get id from header.
	 * @param datagram containing the header with the id field
	 * @return id 
	 * @throws UtilDatagramException
	 */
	public static int getHeaderId(byte[] datagram) throws UtilDatagramException {
		int id = -1;
		
		try {
			id = util.Bytes.byteArray2int(util.Bytes.subArray(datagram,
					FileTransferProtocol.HEADER_ID_START,
					FileTransferProtocol.HEADER_ID_LAST));
		} catch (UtilByteException e) {
			throw new UtilDatagramException(e.getLocalizedMessage());
		}
		
		if (id > FileTransferProtocol.MAX_ID) {
			throw new UtilDatagramException("ID cannot be larger than MAX_ID");
		}
		return id;
	}
	
	/**
	 * Get headerLength from header.
	 * @param datagram containing the header with the headerLength field
	 * @return headerLength 
	 * @throws UtilDatagramException
	 */
	public static int getHeaderHeaderLength(byte[] datagram) throws UtilDatagramException {
		int headerLength = -1;
		
		try {
			headerLength = util.Bytes.byteArray2int(util.Bytes.subArray(datagram,
					FileTransferProtocol.HEADER_HEADER_LENGTH_START,
					FileTransferProtocol.HEADER_HEADER_LENGTH_LAST));
		} catch (UtilByteException e) {
			throw new UtilDatagramException(e.getLocalizedMessage());
		}
		return headerLength;
	}
	
	/**
	 * Get byteOffset from header.
	 * @param datagram containing the header with the byteOffset field
	 * @return byteOffset 
	 * @throws UtilDatagramException
	 */
	public static int getHeaderByteOffset(byte[] datagram) throws UtilDatagramException {
		int byteOffset = -1;
		
		try {
			byteOffset = util.Bytes.byteArray2int(util.Bytes.subArray(datagram,
					FileTransferProtocol.HEADER_BYTE_OFFSET_START,
					FileTransferProtocol.HEADER_BYTE_OFFSET_LAST));
		} catch (UtilByteException e) {
			throw new UtilDatagramException(e.getLocalizedMessage());
		}
		return byteOffset;
	}
	
	/**
	 * Get payload from the datagramPacket.
	 * @param datagram to extract payload from
	 * @param payloadLength in bytes
	 * @return byte array containing the payload
	 * @throws UtilDatagramException
	 */
	public static byte[] getPayload(byte[] datagram, int payloadLength) 
			throws UtilDatagramException {
		byte[] payload = null; 

		try {
			payload = util.Bytes.subArray(datagram, 
					FileTransferProtocol.PAYLOAD_START,
					FileTransferProtocol.PAYLOAD_START + payloadLength - 1);
		} catch (UtilByteException e) {
			throw new UtilDatagramException(e.getLocalizedMessage());
		}
		return payload;
	}
	
	/**
	 * Get source address from received datagram.
	 * NOTE: only use on received datagrams, at sendingDatagrams it will be destinationAddress 
	 * @param receivedDatagram to extract source addres from
	 * @return source address as InetAddress
	 */
	public static InetAddress getDatagramSourceAddress(DatagramPacket receivedDatagram) {
		return receivedDatagram.getAddress();
	}
	
	/**
	 * Create a Packet object from a received DatagramPacket
	 * Note: only works for received datagrams! 
	 * @param datagram received, to create Packet from
	 * @param localSocket on which the datagram was received
	 * @return Packet object, created from the datagram
	 * @throws PacketException
	 * @throws UtilDatagramException 
	 */
	public static Packet createPacketFromDatagram(DatagramPacket datagram, 
			DatagramSocket receivingSocket) throws PacketException, UtilDatagramException {

        byte[] data = datagram.getData();

        int id = util.Datagram.getHeaderId(data);
        int headerLength = util.Datagram.getHeaderHeaderLength(data);
        int byteOffset = util.Datagram.getHeaderByteOffset(data);
        
        int payloadLength = datagram.getLength() - headerLength;
        byte[] payload = util.Datagram.getPayload(data, payloadLength);
		
		Packet packet = new Packet(
				id, 
				util.Datagram.getDatagramSourceAddress(datagram),
				datagram.getPort(),
				receivingSocket.getLocalAddress(),
				receivingSocket.getPort(), 
				payload, 
				byteOffset
				); 
		return packet;
	}
	
	/**
	 * Create a DatagramPacket from a Packet.
	 * @param packet to create the datagram
	 * @param destinationPort to send the datagram to
	 * @return datagramPacket, created from the Packet
	 * @throws UtilDatagramException
	 */
	public static DatagramPacket buildDatagram(Packet packet, int destinationPort) 
			throws UtilDatagramException {
		
		byte[] header = buildHeader(packet);
		byte[] data = util.Bytes.concatArray(header, packet.getPayload());
		
		DatagramPacket datagram;
		if (data.length <= FileTransferProtocol.MAX_PACKET_SIZE) {
			datagram = new DatagramPacket(data, 
					data.length, packet.getDestinationAddress(), destinationPort);
		} else {
			throw new UtilDatagramException("Cannot build datagram:"
					+ " packet larger than maximum size");
		}
		return datagram;
	}
	
	/**
	 * Build header for datagram, based on Packet.
	 * @param packet to create header from
	 * @return byte array containing the header
	 * @throws UtilDatagramException
	 */
	public static byte[] buildHeader(Packet packet) throws UtilDatagramException {
		if (packet.getId() > FileTransferProtocol.MAX_ID) {
			throw new UtilDatagramException("ID cannot be larger than MAX_ID");
		}
		
		byte[] header;
		try {
			byte[] idBytes = util.Bytes.int2ByteArray(packet.getId());
			byte[] headerSizeBytes 
				= util.Bytes.int2ByteArray(FileTransferProtocol.TOTAL_HEADER_SIZE);
			byte[] byteOffsetBytes = util.Bytes.int2ByteArray(packet.getByteOffset());
		
			header = util.Bytes.concatArray(idBytes, headerSizeBytes, byteOffsetBytes);
		} catch (UtilByteException e) {
			throw new UtilDatagramException(e.getLocalizedMessage());
		}
		return header;
	}
	
}
