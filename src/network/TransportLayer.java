package network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import javax.swing.text.Utilities;

import exceptions.PacketException;
import exceptions.UtilByteException;
import protocol.FileTransferProtocol;

public class TransportLayer {

	public static DatagramSocket openNewDatagramSocket(int port) throws SocketException {
        DatagramSocket socket = new DatagramSocket(port);
        return socket;

	}
	
	public static void sendPacket(DatagramSocket socket, Packet packet, int destinationPort) 
			throws IOException, PacketException, UtilByteException {
		
		// write file size into the header byte 
		byte[] payloadSizeBytes = util.Bytes.int2ByteArray(packet.getPayloadLength());
		
		byte[] header = payloadSizeBytes;
		byte[] data = util.Bytes.concatArray(header, packet.getPayload());
		
		if (data.length <= FileTransferProtocol.MAX_PACKET_SIZE) {
			DatagramPacket datagram = new DatagramPacket(data, 
					data.length, packet.getDestinationAddress(), destinationPort);
			socket.send(datagram);
		} else {
			throw new PacketException("Cannot send datagram: packet larger than maximum size");
		}
	}
		
	
	public static Packet receivePacket(DatagramSocket socket) throws IOException, PacketException {
		byte[] buffer = new byte[FileTransferProtocol.MAX_PACKET_SIZE]; // TODO don't know how large, so prepare for maximum
        DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);
        socket.receive(datagram);
        
        // TODO
        // getLength()
        // Returns the length of the data to be sent or the length of the data received.
        
        byte[] data = datagram.getData();

        byte[] payload = null;// TODO find more elegant solution?
        try {
        	int payloadLength = util.Bytes.byteArray2int(util.Bytes.subArray(data,
        			FileTransferProtocol.HEADER_PAYLOAD_LENGTH_START,
        			FileTransferProtocol.HEADER_PAYLOAD_LENGTH_LAST));


        	payload = util.Bytes.subArray(data, 
        			FileTransferProtocol.PAYLOAD_START,
        			FileTransferProtocol.PAYLOAD_START + payloadLength-1);

        } catch (UtilByteException e) {
        	// TODO Auto-generated catch block
        	System.out.println("FAIL: " + e.getLocalizedMessage()); // TODO or throw higher? 
        	e.printStackTrace();
        } 

        Packet packet = new Packet(
        		0, 
        		TransportLayer.getDatagramSourceAddress(datagram), 
        		null, // TODO: cannot be retrieved, assume own address? (note: lookup takes time!) 
        		payload// TODO: remove any padding?! Based on payload length field
        		); // TODO: id, null should be own address / from datagram?

        return packet;
	}
	
	/**
	 * TODO
	 * @param receivedDatagram NOTE: only use on received datagrams, at sendingDatagrams it will be destinationAddress 
	 * @return
	 */
	public static InetAddress getDatagramSourceAddress(DatagramPacket receivedDatagram) {
		return receivedDatagram.getAddress();
	}
	
	
	// TODO: deprecate? 
	
	public static void sendRequest(DatagramSocket socket, InetAddress address, int port) 
			throws IOException {
		DatagramPacket request = new DatagramPacket(new byte[1], 1, address, port);
        socket.send(request);
	}
	
	public static String receiveResponse(DatagramSocket socket) throws IOException {
		byte[] buffer = new byte[512];
        DatagramPacket response = new DatagramPacket(buffer, buffer.length);
        socket.receive(response);

        String responseString = new String(buffer, 0, response.getLength());
        return responseString;
	}
	
	
}
