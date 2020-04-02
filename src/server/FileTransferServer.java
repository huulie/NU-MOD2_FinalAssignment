package server;

import java.net.DatagramSocket;

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
	public FileTransferServer(int port) throws SocketException {
        socket = new DatagramSocket(port);
        //random = new Random();
    }
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
