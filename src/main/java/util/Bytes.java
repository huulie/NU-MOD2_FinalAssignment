package util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

import exceptions.UtilByteException;

/**
 * Utilities to work with bytes.
 * @author huub.lievestro
 *
 */
public class Bytes {

	/**
	 * Concatenate multiple byte arrays.
	 * @param arrays to be concatenated
	 * @return concatenated byte array, containing all input arrays
	 */
	public static byte[] concatArray(byte[]...arrays) {
	    // Determine the length of the result array
	    int totalLength = 0;
	    for (int i = 0; i < arrays.length; i++) {
	        totalLength += arrays[i].length;
	    }

	    // create the result array
	    byte[] result = new byte[totalLength];

	    // copy the source arrays into the result array
	    int currentIndex = 0;
	    for (int i = 0; i < arrays.length; i++) {
	        System.arraycopy(arrays[i], 0, result, currentIndex, arrays[i].length);
	        currentIndex += arrays[i].length;
	    }
	    return result;
	}
	
	/**
	 * Convert an int to a byte array.
	 * @param i integer to convert
	 * @return byte array, representing this integer
	 * @throws UtilByteException if a negative integer is put in
	 */
	public static byte[] int2ByteArray(int i) throws UtilByteException {
		if (i < 0) {
			throw new UtilByteException("This conversion does NOT support negative integers");
		}
		ByteBuffer b = ByteBuffer.allocate(4); 
		b.putInt(i); // using Big Endian! 
		return b.array();
	}
	
	/**
	 * Convert a byte array to an int.
	 * Note: assumes byte array big-endian!
	 * @param byteArray to convert
	 * @return int, represented by this byteArray
	 */
	public static int byteArray2int(byte[] byteArray)  {
		return ByteBuffer.wrap(byteArray).getInt(); // assuming Big-endian!
	}
	
	/**
	 * Array used to hold characters for bytes to hex conversion.
	 */
	private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
	
	/**
	 * Convert bytes to their hexadecimal representation.
	 * @param bytes to convert
	 * @return String with hexadecimal representation of the bytes
	 */
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for (int j = 0; j < bytes.length; j++) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = HEX_ARRAY[v >>> 4];
	        hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
	/**
	 * Take a part of a byte array.
	 * Note: last is inclusive, so this index will be copied (different from copyOfRange: exclusive)
	 * @param array to take this part from
	 * @param start first index to include
	 * @param last final index to include
	 * @return subarray, containing the elements from the array with indices start trough last
	 * @throws UtilByteException
	 */
	public static byte[] subArray(byte[] array, int start, int last) throws UtilByteException {
		if (start < 0 || last < 0 || last + 1 > array.length) {
			throw new UtilByteException("Indices cannot be negative!");
		}
		return Arrays.copyOfRange(array, start, last + 1);
	}
	
	/**
	 * Serialise an object to a byte array.
	 * @param object to serialize
	 * @return byte array, representing the object
	 * @throws IOException
	 */
	public static byte[] serialiseObjectToByteArray(Object object) throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ObjectOutputStream objectOutputStream =
		    new ObjectOutputStream(byteArrayOutputStream);
		objectOutputStream.writeObject(object);
		objectOutputStream.flush();
		objectOutputStream.close();

		return byteArrayOutputStream.toByteArray();
	}
	
	/**
	 * Deserialise a byte array to its corresponding File array object.
	 * @param byteArray containing the file array object
	 * @return file array
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public static File[] deserialiseByteArrayToFileArray(byte[] byteArray) 
			throws ClassNotFoundException, IOException {
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
		ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);

		File[] fileArray = (File[]) objectInputStream.readObject(); 

		objectInputStream.close();
		return fileArray;
	}
	
	/**
	 * Deserialise a byte array to its corresponding File object.
	 * @param byteArray containing the file  object
	 * @return file 
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public static File deserialiseByteArrayToFile(byte[] byteArray) 
			throws ClassNotFoundException, IOException {
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
		ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);

		File fileArray = (File) objectInputStream.readObject();

		objectInputStream.close();
		return fileArray;
	}
}
