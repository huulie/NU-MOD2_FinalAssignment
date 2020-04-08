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

public class Bytes {

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
	 * TODO
	 * @param i
	 * @return
	 * @throws UtilByteException
	 */
	public static byte[] int2ByteArray(int i) throws UtilByteException {
		if (i < 0) {
			throw new UtilByteException("This conversion does NOT support negative integers");
		}
		
//		int numberOfBytesNeeded;
//		if (i == 0) {
//			numberOfBytesNeeded = 1;
//		} else {
//			numberOfBytesNeeded = (int) Math.ceil(Math.log(i) / Math.log(2) / 8);
//		}
		
		ByteBuffer b = ByteBuffer.allocate(4); // TODO: always puts in block of 4 bytes
			// TODO: check with number of bytes!
		b.putInt(i); // using Big Endian! 
		return b.array();
	}
	
	/*
	 * TODO
	 */
	public static int byteArray2int(byte[] byteArray)  {
		return ByteBuffer.wrap(byteArray).getInt(); // assuming Big-endian!
	}
	
	/**
	 * TODO
	 * last is inclusive (note: copyOfRange: exclusive)
	 * @throws UtilByteException 
	 */
	public static byte[] subArray(byte[] array, int start, int last) throws UtilByteException {
		if (start < 0 || last < 0 || last + 1 > array.length) {
			throw new UtilByteException("Indices cannot be negative!");
		}
		
		return Arrays.copyOfRange(array, start, last + 1);
	}
	
	public static byte[] serialiseObjectToByteArray(Object object) throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ObjectOutputStream objectOutputStream =
		    new ObjectOutputStream(byteArrayOutputStream);
		objectOutputStream.writeObject(object);
		objectOutputStream.flush();
		objectOutputStream.close();

		return byteArrayOutputStream.toByteArray();
	}
	
	public static File[] deserialiseByteArrayToFileArray(byte[] byteArray) throws ClassNotFoundException, IOException {
		// TODO https://stackoverflow.com/questions/14669820/how-to-convert-a-string-array-to-a-byte-array-java
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
		ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);

		File[] fileArray = (File[]) objectInputStream.readObject(); // instead of String

		objectInputStream.close();

		return fileArray;
	}
	
	public static File deserialiseByteArrayToFile(byte[] byteArray) throws ClassNotFoundException, IOException {
		// TODO https://stackoverflow.com/questions/14669820/how-to-convert-a-string-array-to-a-byte-array-java
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
		ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);

		File fileArray = (File) objectInputStream.readObject(); // instead of String

		objectInputStream.close();

		return fileArray;
	}
}
