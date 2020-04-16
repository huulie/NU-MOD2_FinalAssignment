package util;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.DatagramPacket;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import exceptions.PacketException;
import exceptions.UtilDatagramException;
import network.Packet;

/**
 * Tests for the Datagram utilities.
 * @author huub.lievestro
 *
 */
public class DatagramTest {
// TODO extend and/or refine testing
	
	@Test
	void testBuildAndInspectDataGram() {	
		int mockId = 1;
		int mockByteOffset = 5;
		int mockPayloadLength = 10;
		byte[] mockPayload = new byte[mockPayloadLength];
		
		int mockDestPort = 1234;
		
		try {
			Packet test = new Packet(mockId, null, 0, null, 0, mockPayload, mockByteOffset);
			
			DatagramPacket testDatagram = util.Datagram.buildDatagram(test, mockDestPort);
			
			byte[] testPayload = util.Datagram.getPayload(testDatagram.getData(),
					mockPayloadLength);
			assertTrue(Arrays.equals(testPayload, mockPayload));
					
			int testId = util.Datagram.getHeaderId(testDatagram.getData());
			assertTrue(testId == mockId);

			int testByteOffset = util.Datagram.getHeaderByteOffset(testDatagram.getData());
			assertTrue(testByteOffset == mockByteOffset);

		} catch (PacketException | UtilDatagramException e) {
			fail();
			e.printStackTrace();
		}
	}

}
