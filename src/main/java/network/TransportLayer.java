package network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import exceptions.PacketException;
import exceptions.UtilByteException;
import exceptions.UtilDatagramException;
import protocol.FileTransferProtocol;

public class TransportLayer {

	public static DatagramSocket openNewDatagramSocket() throws SocketException {
        DatagramSocket socket = new DatagramSocket();
        return socket;
	}
	
	public static DatagramSocket openNewDatagramSocket(int port) throws SocketException {
        DatagramSocket socket = new DatagramSocket(port);
        return socket;
	}
	
	public static void sendPacket(DatagramSocket socket, Packet packet, int destinationPort) 
			throws IOException, UtilByteException, UtilDatagramException {
		
		DatagramPacket datagram = util.Datagram.buildDatagram(packet, destinationPort);
		socket.send(datagram);	
	}
	
	public static Packet receivePacket(DatagramSocket socket) 
			throws IOException, PacketException, UtilDatagramException {
		
		byte[] buffer = new byte[FileTransferProtocol.MAX_PACKET_SIZE];
        DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);
        socket.receive(datagram);
        
        return util.Datagram.createPacketFromDatagram(datagram,socket);
	}
	
}
