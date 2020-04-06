package network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class NetworkLayer {

	public static InetAddress getAdressByName(String hostname) throws UnknownHostException {
		InetAddress address = InetAddress.getByName(hostname);
		return address;
	}
	
	public static InetAddress getOwnAddress() throws UnknownHostException  {
		InetAddress address = InetAddress.getLocalHost();
		return address;
		// TODO maybe replace with more extensive search as in project GO
	}
	
	public static InetAddress discoverLocalAddress() { // TODO remove?!
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
