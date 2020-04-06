package util;

import network.Packet;

/**
 * TODO because these are utitilies, not part of mdoel of packet (having bytes as payload)
 * @author huub.lievestro
 *
 */
public class PacketUtil {


	/**
	 * TODO
	 * @param 
	 * @return
	 */
	public static String convertPayloadtoString(Packet packet) {
		byte[] packetBytes = packet.getPayload(); 
		String packetString = new String(packetBytes);
		
		return packetString;
	}
	
}
