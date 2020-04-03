package client;

import java.net.DatagramSocket;
import java.net.SocketException;
import network.TransportLayer;
import protocol.FileTransferProtocol;

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

		FileTransferClient fileTranferClient = 
					new FileTransferClient(FileTransferProtocol.CLIENT_PORT); // TODO now HARDCODED
		fileTranferClient.socket.close(); // TODO make a method for this, ensure!

		
	}
	
	/**
	 * Construct a new FileTransfer client.
	 * @param socket
	 * @param port
	 */
	public FileTransferClient(int port) {
		try {
			this.socket = TransportLayer.openNewDatagramSocket(port);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			System.out.println("FAILED to open socket " + e.getLocalizedMessage());
			e.printStackTrace();
		}
		
		this.port = port;
	}

}
