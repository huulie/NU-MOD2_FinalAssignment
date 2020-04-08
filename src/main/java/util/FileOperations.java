package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class FileOperations {

	
	/**
     * Gets the contents of the specified file.
     * @param id the file ID
     * @return the array of integers, representing the contents of the file to transmit
     */
    public static byte[] getFileContents(File fileToRead) {
//        File fileToTransmit = new File(String.format("rdtcInput%d.png", id));
        try (FileInputStream fileStream = new FileInputStream(fileToRead)) {
            byte[] fileContents = new byte[(int) fileToRead.length()];

            for (int i = 0; i < fileContents.length; i++) {
                byte nextByte = (byte) fileStream.read(); // TODO check!
                if (nextByte == -1) {
                    throw new Exception("File size is smaller than reported");
                }

                fileContents[i] = nextByte;
            }
            return fileContents;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.err.println(e.getStackTrace());
            return null;
        }
    }

    /**
     * Writes the contents of the fileContents array to the specified file. TODO update
     * @param fileContents the contents to write
     * @param id the file ID
     */
    public static void setFileContents(byte[] fileContents, int id, long timestamp) {
        File fileToWrite = new File(String.format("rdtcOutput%d.%d.png", id, timestamp));
        try (FileOutputStream fileStream = new FileOutputStream(fileToWrite)) {
            for (byte fileContent : fileContents) {
                fileStream.write(fileContent);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.err.println(e.getStackTrace());
        }
    }
	
}
