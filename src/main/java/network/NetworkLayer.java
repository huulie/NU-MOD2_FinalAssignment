package network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * This class provides network layer functionalities.
 * @author huub.lievestro
 *
 */
public class NetworkLayer {

	/**
	 * Get network address by host name.
	 * @param hostname to lookup
	 * @return InetAddress belonging to that hostname
	 * @throws UnknownHostException
	 */
	public static InetAddress getAdressByName(String hostname) throws UnknownHostException {
		InetAddress address = InetAddress.getByName(hostname);
		return address;
	}
	
	/**
	 * Get network address of localhost.
	 * @return InetAddress belonging to localhost
	 * @throws UnknownHostException
	 */
	public static InetAddress getOwnAddress() throws UnknownHostException  {
		InetAddress address = InetAddress.getLocalHost();
		return address;
	}
	
	/**
	 * Discover network address of localhost,
	 * using a more advanced method, to find the preferred network interface.
	 * @return InetAddress belonging to localhost
	 */
	public static InetAddress discoverLocalAddress() {
		InetAddress localIP = null;
		Socket discoverLocalIP = new Socket(); 
		// this ways, it returns the preferred outbound IP:
		try {
			try {
				discoverLocalIP.connect(new InetSocketAddress("google.com", 80));
				localIP = discoverLocalIP.getLocalAddress();
				System.out.println("Discovering local IP address: " 
						+ discoverLocalIP.getLocalAddress());
				discoverLocalIP.close();
			} catch (UnknownHostException eUnknownHost) {
				System.out.println("No internet access, trying locally to reach 192.168.1.1");
				Socket discoverLocalIPonLAN = new Socket(); 
				discoverLocalIPonLAN.connect(new InetSocketAddress("192.168.1.1", 80));
				localIP = discoverLocalIPonLAN.getLocalAddress();
				System.out.println("Discovering local IP address: " 
						+ discoverLocalIPonLAN.getLocalAddress());
				discoverLocalIPonLAN.close();
			}
		} catch (IOException e1) {
			System.out.println("IO Exception while Discovering local IP address: " 
					+ e1.getLocalizedMessage());
			e1.printStackTrace();
		}
		return localIP;
	}
	
}
