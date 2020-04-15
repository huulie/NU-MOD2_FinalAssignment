package util;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import exceptions.UtilByteException;

/**
 * Tests for the Byte utilities.
 * @author huub.lievestro
 *
 */
public class BytesTest {

	@Test
	void testArrayConcatenation() {	
		byte[] array1 = new byte[2];
		array1[0] = 0;
		array1[1] = 1;
		byte[] array2 = new byte[2];
		array2[0] = 2;
		array2[1] = 3;

		byte[] concat = util.Bytes.concatArray(array1, array2);

		assertTrue(concat[0] == 0);
		assertTrue(concat[1] == 1);
		assertTrue(concat[2] == 2);
		assertTrue(concat[3] == 3);

	}
	
	@Test
	void testSubArray() {	
		byte[] testArray = new byte[4];
		testArray[0] = 0;
		testArray[1] = 1;
		testArray[2] = 2;
		testArray[3] = 3;


		try {
			byte[] subBegin = util.Bytes.subArray(testArray, 0, 2);
			assertTrue(subBegin[0] == 0);
			assertTrue(subBegin[1] == 1);
			assertTrue(subBegin[2] == 2);


			byte[] subMiddle = util.Bytes.subArray(testArray, 1, 2);
			assertTrue(subMiddle[0] == 1);
			assertTrue(subMiddle[1] == 2);

			byte[] subEnd = util.Bytes.subArray(testArray, 2, 3);
			assertTrue(subEnd[0] == 2);
			assertTrue(subEnd[1] == 3);

			byte[] subFull = util.Bytes.subArray(testArray, 0, 3);
			assertTrue(subFull[0] == 0);
			assertTrue(subFull[1] == 1);
			assertTrue(subFull[2] == 2);
			assertTrue(subFull[3] == 3);
		} catch (UtilByteException e) {
			fail();
			e.printStackTrace();
		}
	}
	
	@Test
	void testInt2ByteArray() {
		try {
			byte[] test = util.Bytes.int2ByteArray(1);
			assertTrue(test[0] == 0);
			assertTrue(test[1] == 0);
			assertTrue(test[2] == 0);
			assertTrue(test[3] == 1);
			
		} catch (UtilByteException e) {
			fail();
			e.printStackTrace();
		}
	}
	
	@Test
	void testByteArray2Int() {
		byte[] test = new byte[4];
		test[0] = 0;
		test[1] = 0;
		test[2] = 0;
		test[3] = 1;

		int testInt = util.Bytes.byteArray2int(test);
		assertTrue(testInt == 1);
	}
	
	@Test
	void testFileSerialisation() {
		File mockFile = new File("");
		
		try {
			byte[] testBytes = util.Bytes.serialiseObjectToByteArray(mockFile);
			File testFile = util.Bytes.deserialiseByteArrayToFile(testBytes);

			assertTrue(testFile.equals(mockFile));
		} catch (IOException | ClassNotFoundException e) {
			fail();
			e.printStackTrace();
		}
	}
	
	@Test
	void testFileArraySerialisation() {
		File[] mockFiles = new File[2];
		mockFiles[0] = new File("");
		mockFiles[1] = new File("");

		try {
			byte[] testBytes = util.Bytes.serialiseObjectToByteArray(mockFiles);
			File[] testFiles = util.Bytes.deserialiseByteArrayToFileArray(testBytes);

			assertTrue(Arrays.equals(testFiles, mockFiles));
		} catch (IOException | ClassNotFoundException e) {
			fail();
			e.printStackTrace();
		}
	}

}
