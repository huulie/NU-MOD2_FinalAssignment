package network;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class NetworkLayer {

	public InetAddress getAdressByName(String hostname) throws UnknownHostException {
		InetAddress address = InetAddress.getByName(hostname);
		return address;
	}
	
}
