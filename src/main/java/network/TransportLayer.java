package network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import exceptions.PacketException;
import exceptions.UtilByteException;
import exceptions.UtilDatagramException;
import protocol.FileTransferProtocol;

/**
 * This class provides transport layer functionalities.
 * @author huub.lievestro
 *
 */
public class TransportLayer {

	/**
	 * Open a new DatagramSocket.
	 * @return new DatagramSocket
	 * @throws SocketException
	 */
	public static DatagramSocket openNewDatagramSocket() throws SocketException {
        DatagramSocket socket = new DatagramSocket();
        return socket;
	}
	
	/**
	 * Open a new DatagramSocket, bound to a port.
	 * @param port to bind
	 * @return new DatagramSocket, bound to port
	 * @throws SocketException
	 */
	public static DatagramSocket openNewDatagramSocket(int port) throws SocketException {
        DatagramSocket socket = new DatagramSocket(port);
        return socket;
	}
	
	/**
	 * Send a Packet (in a DatagramPacket) via a DatagramSocket.
	 * @param socket used to send the DatagramPacket
	 * @param packet to send
	 * @param destinationPort to send to
	 * @throws IOException
	 * @throws UtilByteException
	 * @throws UtilDatagramException
	 */
	public static void sendPacket(DatagramSocket socket, Packet packet, int destinationPort) 
			throws IOException, UtilByteException, UtilDatagramException {
		
		DatagramPacket datagram = util.Datagram.buildDatagram(packet, destinationPort);
		socket.send(datagram);	
	}
	
	/**
	 * Receive a Packet (in a DatagramPacket) via a DatagramSocket.
	 * @param socket used to receive the DatagramPacket
	 * @return Packet received
	 * @throws IOException
	 * @throws PacketException
	 * @throws UtilDatagramException
	 */
	public static Packet receivePacket(DatagramSocket socket) 
			throws IOException, PacketException, UtilDatagramException {
		
		byte[] buffer = new byte[FileTransferProtocol.MAX_PACKET_SIZE];
        DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);
        socket.receive(datagram);
        
        return util.Datagram.createPacketFromDatagram(datagram, socket);
	}
	
}
