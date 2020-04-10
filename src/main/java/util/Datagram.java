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
 * TODO because datagramPacket is a final class and cannot be extended
 * @author huub.lievestro
 *
 */
public class Datagram {

	public static int getHeaderId(byte[] datagram) throws UtilDatagramException {
		int id = -1;
		
		try {
			id = util.Bytes.byteArray2int(util.Bytes.subArray(datagram,
					FileTransferProtocol.HEADER_ID_START,
					FileTransferProtocol.HEADER_ID_LAST));
		} catch (UtilByteException e) {
			// TODO Auto-generated catch block
			throw new UtilDatagramException(e.getLocalizedMessage());
		}
		
		return id;
	}
	
	public static int getHeaderHeaderLength(byte[] datagram) throws UtilDatagramException {
		int headerLength = -1;
		
		try {
			headerLength = util.Bytes.byteArray2int(util.Bytes.subArray(datagram,
					FileTransferProtocol.HEADER_HEADER_LENGTH_START,
					FileTransferProtocol.HEADER_HEADER_LENGTH_LAST));
		} catch (UtilByteException e) {
			// TODO Auto-generated catch block
			throw new UtilDatagramException(e.getLocalizedMessage());
		}
		
		return headerLength;
	}
	
	public static int getHeaderByteOffset(byte[] datagram) throws UtilDatagramException {
		int byteOffset = -1;
		
		try {
			byteOffset = util.Bytes.byteArray2int(util.Bytes.subArray(datagram,
					FileTransferProtocol.HEADER_BYTE_OFFSET_START,
					FileTransferProtocol.HEADER_BYTE_OFFSET_LAST));
		} catch (UtilByteException e) {
			// TODO Auto-generated catch block
			throw new UtilDatagramException(e.getLocalizedMessage());
		}
		
		return byteOffset;
	}
	
	public static byte[] getPayload(byte[] datagram, int payloadLength) throws UtilDatagramException {
		byte[] payload = null; 

		try {
			payload = util.Bytes.subArray(datagram, 
					FileTransferProtocol.PAYLOAD_START,
					FileTransferProtocol.PAYLOAD_START + payloadLength - 1);
		} catch (UtilByteException e) {
			// TODO Auto-generated catch block
			throw new UtilDatagramException(e.getLocalizedMessage());
		}

		return payload;
	}
	
	/**
	 * TODO
	 * @param receivedDatagram NOTE: only use on received datagrams, at sendingDatagrams it will be destinationAddress 
	 * @return
	 */
	public static InetAddress getDatagramSourceAddress(DatagramPacket receivedDatagram) {
		return receivedDatagram.getAddress();
	}
	
	/**
	 * TODO note only works for received datagrams! 
	 * @param datagram
	 * @param localSocket
	 * @return
	 * @throws PacketException
	 * @throws UtilDatagramException 
	 */
	public static Packet createPacketFromDatagram(DatagramPacket datagram, DatagramSocket receivingSocket) 
			throws PacketException, UtilDatagramException {
		
		// TODO
        // getLength()
        // Returns the length of the data to be sent or the length of the data received.
        
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
				receivingSocket.getLocalAddress(), // TODO: assume own address? 
				receivingSocket.getPort(), // TODO: retains binding to port number, or LOCALport?
				payload,// TODO: remove any padding?! Based on payload length field
				byteOffset
				); // TODO: id, null should be own address / from datagram?
		
		return packet;
	}
	
	public static DatagramPacket buildDatagram(Packet packet, int destinationPort) throws UtilDatagramException {
		
		byte[] header = buildHeader(packet);
		byte[] data = util.Bytes.concatArray(header, packet.getPayload());
		
		DatagramPacket datagram;
		if (data.length <= FileTransferProtocol.MAX_PACKET_SIZE) {
			datagram = new DatagramPacket(data, 
					data.length, packet.getDestinationAddress(), destinationPort);
		} else {
			throw new UtilDatagramException("Cannot build datagram: packet larger than maximum size");
		}
		
		return datagram;
	}
	
	public static byte[] buildHeader(Packet packet) throws UtilDatagramException {
		// write file size into the header byte 
		byte[] header;
		try {
			byte[] idBytes = util.Bytes.int2ByteArray(packet.getId());
			byte[] headerSizeBytes = util.Bytes.int2ByteArray(FileTransferProtocol.TOTAL_HEADER_SIZE);
					//TODO util.Bytes.int2ByteArray(packet.getPayloadLength());
			byte[] byteOffsetBytes = util.Bytes.int2ByteArray(packet.getByteOffset());
		
			header = util.Bytes.concatArray(idBytes, headerSizeBytes, byteOffsetBytes);
		} catch (UtilByteException e) {
			// TODO Auto-generated catch block
			throw new UtilDatagramException(e.getLocalizedMessage());
		}

		return header;
	}
	
}
