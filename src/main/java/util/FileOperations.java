package util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utilities to work with Files.
 * @author huub.lievestro
 *
 */
public class FileOperations {

	/**
     * Gets the contents of the specified file.
     * @param fileToRead, represented by a File object
     * @return array of bytes, representing the contents of the file
	 * @throws IOException 
     */
    public static byte[] getFileContents(File fileToRead) throws IOException {
        return Files.readAllBytes(Paths.get(fileToRead.getAbsolutePath())); 
    }

    /**
     * Writes the contents of the fileContents array to the specified file. TODO update
     * @param fileContents the contents to write
     * @param fileToWrite the File to write to
     * @throws IOException 
     */
    public static void setFileContents(byte[] fileContents, File fileToWrite) throws IOException {
        try (FileOutputStream fileStream = new FileOutputStream(fileToWrite)) {
        	fileStream.write(fileContents); 
        }
    }
    
    /**
     * Calculate file hash.
     * @param fileToHash File object pointing to file to hash 
     * @return file hash as byte array
     * @throws NoSuchAlgorithmException 
     * @throws IOException
     */
    public static byte[] getHashBytes(File fileToHash) 
    		throws IOException, NoSuchAlgorithmException {
    	byte[] fileBytes = Files.readAllBytes(Paths.get(fileToHash.getAbsolutePath()));
    	return MessageDigest.getInstance("MD5").digest(fileBytes);
    }

    /**
     * Calculate file hash.
     * @param fileToHash File object pointing to file to hash 
     * @return file hash as HEX string
     * @throws NoSuchAlgorithmException 
     * @throws IOException
     */
    public static  String getHashHexString(File fileToHash) 
    		throws IOException, NoSuchAlgorithmException {
    	byte[] hashBytes = getHashBytes(fileToHash);
    	return util.Bytes.bytesToHex(hashBytes);
    }
}

