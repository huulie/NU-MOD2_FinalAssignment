package server;

import java.net.DatagramSocket;
import java.net.SocketException;
import network.TransportLayer;
import protocol.FileTransferProtocol;

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

		FileTransferServer FileTransferServer 
			= new FileTransferServer(FileTransferProtocol.SERVER_PORT); // TODO now HARDCODED
		FileTransferServer.socket.close(); // TODO make a method for this, ensure!
		
	}
	
	/**
	 * Construct a new FileTransfer server.
	 * @param socket
	 * @param port
	 */
	public FileTransferServer(int port) {
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
