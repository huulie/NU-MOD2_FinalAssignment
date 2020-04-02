package server;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import network.NetworkLayer;
import network.Packet;
import network.TransportLayer;

public class FileTransferServer {

	/**
	 * TODO 
	 */
	DatagramSocket socket;
	
	/**
	 * TODO
	 */
	int port;
	
	/**
	 * TODO
	 * @param port
	 * @throws SocketException
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		FileTransferServer helloWorldServer = new FileTransferServer(4567); // TODO for now HARDCODED
		helloWorldServer.receiveHelloWorld();
		System.out.println("DONE: responded with a Hello World! ");
		
	}
	
	/**
	 * Construct a new FileTransfer server.
	 * @param socket
	 * @param port
	 */
	public FileTransferServer(int port) {
		try {
			this.socket = TransportLayer.openNewDatagramSocket();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			System.out.println("FAILED to open socket " + e.getLocalizedMessage());
			e.printStackTrace();
		}
		
		this.port = port;
	}
	
	public void receiveHelloWorld() {		
		System.out.println("Server running.. ");

		try {
			Packet receivedPacket = TransportLayer.receivePacket(this.socket);
			
			if (receivedPacket.getPayload() == "Hello, are you there?".getBytes()) { 
				// TODO note encoding
				this.respondHelloWorld(receivedPacket.getSourceAddress());
				System.out.println("Responding to request...");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("FAILED when receiving packet " + e.getLocalizedMessage());
			e.printStackTrace();
		}
	}
		
	public void respondHelloWorld(InetAddress respondToAddress) {		
		try { // to construct and send a packet
			String respondHello = "Hello World!";

			Packet respondHelloPacket = new Packet(
					0, 
					NetworkLayer.getOwnAddress(), 
					respondToAddress, 
					respondHello.getBytes());
		
		
			TransportLayer.sendPacket(this.socket, respondHelloPacket, 4567);
			// TODO for now HARDCODED, not use same port on server as on client 

		
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			System.out.println("FAILED unknown host " + e.getLocalizedMessage());
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("FAILED when sending packet " + e.getLocalizedMessage());
			e.printStackTrace();
		}
	}
}
