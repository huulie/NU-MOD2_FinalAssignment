package server;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;

import exceptions.PacketException;
import exceptions.UtilByteException;
import exceptions.UtilDatagramException;
import network.NetworkLayer;
import network.Packet;
import network.TransportLayer;
import protocol.FileTransferProtocol;

public class HelloWorldServer {

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

		HelloWorldServer helloWorldServer 
			= new HelloWorldServer(FileTransferProtocol.DEFAULT_SERVER_PORT); // TODO now HARDCODED
		helloWorldServer.receiveHelloWorld();
		System.out.println("DONE: responded with a Hello World! (or something else) ");
		helloWorldServer.socket.close(); // TODO make a method for this, ensure!
		
	}
	
	/**
	 * Construct a new FileTransfer server.
	 * @param socket
	 * @param port
	 */
	public HelloWorldServer(int port) {
		try {
			this.socket = TransportLayer.openNewDatagramSocket(port);
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
			
			byte[] responseBytes = receivedPacket.getPayload(); 
			String responseString = new String(responseBytes);
			
			System.out.println(Arrays.toString(responseBytes));
			System.out.println(Arrays.toString(responseString.getBytes()));
			System.out.println(Arrays.toString("Hello, are you there?".getBytes()));

			
			if (responseString.equals("Hello, are you there?")) {
				// TODO note encoding
				this.respondHelloWorld(receivedPacket.getSourceAddress());
				System.out.println("Responding to request from " 
						+ receivedPacket.getSourceAddress().toString() + "...");
			} else {
				System.out.println("Don't know what to do with payload: " + responseString);
			}
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
			System.out.println("FAILED " + e.getLocalizedMessage());
			e.printStackTrace();
		}
	}
		
	public void respondHelloWorld(InetAddress respondToAddress) {		
		try { // to construct and send a packet
			String respondHello = "Hello World!";

			Packet respondHelloPacket = new Packet(
					1, 
					NetworkLayer.getOwnAddress(),
					FileTransferProtocol.DEFAULT_SERVER_PORT,
					respondToAddress, 
					 FileTransferProtocol.DEFAULT_CLIENT_PORT,
					respondHello.getBytes());
		
		
			TransportLayer.sendPacket(
					this.socket,
					respondHelloPacket,
					FileTransferProtocol.DEFAULT_CLIENT_PORT
			);
		
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
			System.out.println("FAILED on " + e.getLocalizedMessage());
			e.printStackTrace();
		} catch (UtilDatagramException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
