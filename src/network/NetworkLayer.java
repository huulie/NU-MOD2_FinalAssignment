package network;

import java.net.InetAddress;
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
	
}
