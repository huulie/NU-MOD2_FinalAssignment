package network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class TransportLayer {

	public static DatagramSocket openNewDatagramSocket() throws SocketException {
        DatagramSocket socket = new DatagramSocket();
        return socket;

	}
	
	public static void sendPacket(DatagramSocket socket, Packet packet, int port) 
			throws IOException {
		
		DatagramPacket datagram = new DatagramPacket(packet.getPayload(), 
				packet.getPayload().length, packet.getDestinationAddress(), port);
        
		socket.send(datagram);
	}
		
	
	public static Packet receivePacket(DatagramSocket socket) throws IOException {
		byte[] buffer = new byte[512];
        DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);
        socket.receive(datagram);

        Packet packet = new Packet(
        		0, 
        		TransportLayer.getDatagramSourceAddress(datagram), 
        		null, 
        		datagram.getData()
        		); // TODO: id, null should be own address / from datagram?
        
        return packet;
	}
	
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
