package client;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;

import exceptions.PacketException;
import exceptions.UtilByteException;
import exceptions.UtilDatagramException;
import network.NetworkLayer;
import network.Packet;
import network.TransportLayer;
import protocol.FileTransferProtocol;

public class HelloWorldClient {

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

		HelloWorldClient helloWorldClient = 
					new HelloWorldClient(FileTransferProtocol.DEFAULT_CLIENT_PORT); // TODO now HARDCODED
		helloWorldClient.requestHelloWorld();
		System.out.println("DONE: requested a Hello World and got an answer!");
		helloWorldClient.socket.close(); // TODO make a method for this, ensure!

		
	}
	
	/**
	 * Construct a new FileTransfer client.
	 * @param socket
	 * @param port
	 */
	public HelloWorldClient(int port) {
		try {
			this.socket = TransportLayer.openNewDatagramSocket(port);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			System.out.println("FAILED to open socket " + e.getLocalizedMessage());
			e.printStackTrace();
		}
		
		this.port = port;
	}
	
	public void requestHelloWorld() {
		System.out.println("Client running.. ");
		try { // to construct and send a packet
			String requestHello = "Hello, are you there?";

			Packet requestHelloPacket = new Packet(
					0, 
					NetworkLayer.getOwnAddress(),
					FileTransferProtocol.DEFAULT_CLIENT_PORT,
					NetworkLayer.getAdressByName("nvc4122.nedap.local"), 
					//("nu-pi-huub"), // TODO not hardcode, put let user provide input
					FileTransferProtocol.DEFAULT_SERVER_PORT,
					requestHello.getBytes());
		
			TransportLayer.sendPacket(
					this.socket,
					requestHelloPacket,
					FileTransferProtocol.DEFAULT_SERVER_PORT
			); 
			
			System.out.println("Datagram send... ");

		
//		} catch (UnknownHostException e) {
//			// TODO Auto-generated catch block
//			System.out.println("FAILED unknown host " + e.getLocalizedMessage());
//			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("FAILED when sending packet " + e.getLocalizedMessage());
			e.printStackTrace();
		} catch (PacketException e) {
			// TODO Auto-generated catch block
			System.out.println("FAILED on packet " + e.getLocalizedMessage());
			e.printStackTrace();
		} catch (UtilByteException e) {
			// TODO Auto-generated catch block
			System.out.println("FAILED  " + e.getLocalizedMessage());
			e.printStackTrace();
		} catch (UtilDatagramException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		try {
			Packet receivedPacket = TransportLayer.receivePacket(this.socket);
			
			byte[] responseBytes = receivedPacket.getPayload(); 
			
			String responseString = new String(responseBytes, 0, responseBytes.length);
			System.out.println("Response received: " + responseString);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("FAILED when receiving packet " + e.getLocalizedMessage());
			e.printStackTrace();
		} catch (PacketException e) {
			// TODO Auto-generated catch block
			System.out.println("FAILED on packet " + e.getLocalizedMessage());
			e.printStackTrace();
		} catch (UtilDatagramException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
