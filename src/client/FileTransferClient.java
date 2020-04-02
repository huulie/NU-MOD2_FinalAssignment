package client;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;

import network.NetworkLayer;
import network.Packet;
import network.TransportLayer;

public class FileTransferClient {

	/**
	 * TODO .
	 */
	DatagramSocket socket;
	
	/**
	 * TODO .
	 */
	int port;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		FileTransferClient helloWorldClient = new FileTransferClient(1234);
		helloWorldClient.requestHelloWorld();
		System.out.println("DONE: requested a Hello World... ");
		
	}
	
	/**
	 * Construct a new FileTransfer client.
	 * @param socket
	 * @param port
	 */
	public FileTransferClient(int port) {
		try {
			this.socket = TransportLayer.openNewDatagramSocket();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			System.out.println("FAILED to open socket " + e.getLocalizedMessage());
			e.printStackTrace();
		}
		
		this.port = port;
	}
	
	public void requestHelloWorld() {
		try { // to construct and send a packet
			String requestHello = "Hello, are you there?";

			Packet requestHelloPacket = new Packet(
					0, 
					NetworkLayer.getOwnAddress(), 
					NetworkLayer.getAdressByName("nu-pi-huub"), 
					requestHello.getBytes());
		
		
			TransportLayer.sendPacket(this.socket, requestHelloPacket, this.port);
		
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
