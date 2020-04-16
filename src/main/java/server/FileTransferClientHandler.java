package server;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import exceptions.NotEnoughFreeSpaceException;
import exceptions.PacketException;
import exceptions.ServerFailureException;
import exceptions.UtilByteException;
import exceptions.UtilDatagramException;
import helpers.DownloadHelper;
import helpers.Helper;
import helpers.UploadHelper;
import network.Packet;
import network.TransportLayer;
import protocol.FileTransferProtocol;

public class FileTransferClientHandler implements Runnable {

	/** 
	 * The socket of this FileTranfer ClientHandler.
	 */
	DatagramSocket socket;

	/** 
	 * Network address of the ClientHandler. 
	 */
	InetAddress ownAddress;

	/** 
	 * Network address of the ClientHandler. 
	 */
	int ownPort;

	/** 
	 * Network address of the corresponding client. 
	 */
	InetAddress clientAddress;
	
	/** 
	 * Network port of the corresponding client. 
	 */
	int clientPort;

	/**
	 * Server which started this ClientHandler.
	 */
	FileTransferServer server;
	
	/**
	 * Local filesystem storage location of this ClientHandler.
	 */
	Path fileStorage;

	/**
	 * List of downloads, one for each connected downloadHelper. 
	 */
	private List<Helper> downloads;

	/**
	 * List of uploads, one for each connected uploadHelper. 
	 */
	private List<Helper> uploads;

	/**
	 *  The TUI of this FileTransferServer.
	 */
	private userInterface.TUI textUI; 

	/**
	 * The name of the corresponding client.
	 */
	private String name;

	/**
	 * Indicates if ClientHandler is running.
	 */
	boolean running;

	/**
	 * Construct a new FileTransfer client handler.
	 * @param socket to use for receiving/sending 
	 * @param initPacket send by the client 
	 * @param server which started this client handler
	 * @param name of the client sending the initPacket
	 */
	public FileTransferClientHandler(DatagramSocket socket, Packet initPacket,
			FileTransferServer server, String name) { 
		this.textUI = new userInterface.TUI();

		this.socket = socket;
		this.ownPort = socket.getLocalPort();
		this.ownAddress = socket.getLocalAddress();

		this.name = name;

		this.server = server;
		this.fileStorage = server.getFileStorage(this.name);

		this.downloads = new ArrayList<>();
		this.uploads = new ArrayList<>();

		this.running = true;
		this.setClient(initPacket);
	}

	/**
	 * Set client network information, based on received init packet.
	 * @param initPacket containing the needed information
	 */
	public void setClient(Packet initPacket) {
		this.clientAddress = initPacket.getSourceAddress();
		this.clientPort = initPacket.getSourcePort();

		this.showNamedMessage("Set client information in handler: done ");
	}

	/**
	 * Receive a packet, preprocess it and pass it on to the corresponding method.
	 */
	public void run() { 
		while (running) { // keep listening
			Packet receivedPacket = null;

			this.showNamedMessage("Listening for client requests...");

			try {
				receivedPacket = TransportLayer.receivePacket(this.socket);
			} catch (IOException | PacketException | UtilDatagramException e) {
				this.showNamedError("Someting went wrong while recieving a packet: " 
						+ e.getLocalizedMessage());
				this.showNamedError("Not going to proceed with it: trying to receive a new packet");
			}

			if (receivedPacket == null) {
				this.showNamedError("Someting went wrong with recieving a packet: is null!");
				this.showNamedError("Not going to process it: trying to receive a new packet");
			} else {
				this.showNamedMessage("Received a packet: going to process it...");
				this.showNamedMessage("Packet payload: " + new String(receivedPacket.getPayload()));

				if (!(receivedPacket.getSourceAddress().equals(this.clientAddress)
						&& receivedPacket.getSourcePort() == this.clientPort)) { 
					this.showNamedError("SECURITY WARNING: this response is NOT"
							+ " coming for known client > dropping it");
					continue;
				}

				String receivedString = receivedPacket.getPayloadString();
				this.showNamedMessage("Received String: " + receivedString);
				byte[] receivedBytes = receivedPacket.getPayloadBytes();
				this.showNamedMessage("Received bytes: " + receivedString);

				this.processRequest(receivedString, receivedBytes);
			}
		}
	}

	/**
	 * Take input from the received packet and call corresponding method,
	 *  with some relevant feedback to the user (via TUI) and client (over network).
	 * @param requestString part of the request
	 * @param requestBytes part of the request
	 */
	public void processRequest(String requestString, byte[] requestBytes) {

		String[] request = this.getArguments(requestString); 
		this.showNamedMessage("Received request: " + Arrays.toString(request));

		String command = request[0]; 

		try {
			switch (command) {
				case FileTransferProtocol.LIST_FILES:
					this.showNamedMessage("Client requested list of files...");
					this.sendBytesToClient(this.listFiles(), 0);
					break;

				case FileTransferProtocol.DOWNLOAD:
					this.showNamedMessage("Client requested download of single file...");
					try {
						File fileToUpload = 
								util.Bytes.deserialiseByteArrayToFile(requestBytes);
						this.showNamedMessage("File: " + fileToUpload.getAbsolutePath());

						int downloaderPort = Integer.parseInt(request[1]);
						this.showNamedMessage("To downloader on port: " + downloaderPort);

						this.downloadSingle(fileToUpload, downloaderPort);
					} catch (NumberFormatException | ClassNotFoundException | IOException e) {
						throw new ServerFailureException(e.getLocalizedMessage());
					}
					break;

				case FileTransferProtocol.UPLOAD:
					this.showNamedMessage("Client announced upload of single file...");
					try {
						File fileAnnounced = util.Bytes.deserialiseByteArrayToFile(requestBytes); 

						File fileToDownload = new File(this.fileStorage.toString() +
								File.separator + fileAnnounced.getName());
						this.showNamedMessage("File: " + fileToDownload.getAbsolutePath());

						int uploaderPort = Integer.parseInt(request[1]);
						this.showNamedMessage("From uploader on port: " + uploaderPort);

						long totalFileSize = Long.parseLong(request[2]);
						this.showNamedMessage("Uploader reports total file size = " 
								+ totalFileSize + " bytes");
						
						int startID = Integer.parseInt(request[3]);
						this.showNamedMessage("Uploader starts at ID " + startID);

						this.uploadSingle(fileToDownload, uploaderPort, totalFileSize, startID);
					} catch (NumberFormatException | ClassNotFoundException 
							| IOException | NotEnoughFreeSpaceException e) {
						throw new ServerFailureException(e.getLocalizedMessage());
					}
					break;

				case FileTransferProtocol.DELETE:
					this.showNamedMessage("Client requested deletion of single file...");
					try {
						File fileToDelete = util.Bytes.deserialiseByteArrayToFile(requestBytes); 
						this.deleteSingle(fileToDelete);
					} catch (ClassNotFoundException | IOException e) {
						throw new ServerFailureException(e.getLocalizedMessage());
					}
					break;

				case FileTransferProtocol.HASH:
					this.showNamedMessage("Client requested to check hash of a file...");
					try {
						File fileToCheck = util.Bytes.deserialiseByteArrayToFile(requestBytes); 
						String hashOnClient = request[1];

						this.checkFile(fileToCheck, hashOnClient);
					} catch (ClassNotFoundException | IOException e) {
						throw new ServerFailureException(e.getLocalizedMessage());
					}
					break;

				default:
					this.showNamedError("Unknow command received: ignoring it!");
			}
			this.showNamedMessage("... request done!");
		} catch (ServerFailureException e) {
			byte[] failure = (FileTransferProtocol.FAILED + FileTransferProtocol.DELIMITER 
					+ "Handeling " + command + " failed: " + e.getLocalizedMessage()).getBytes();
			this.sendBytesToClient(failure,	failure.length); 
			this.showNamedError("Client notified of failure: " + e.getLocalizedMessage());
		}
	}

	/**
	 * Split the requestString in command and optional arguments.
	 * @param requestString
	 * @return String[] with command and optional arguments
	 */
	public String[] getArguments(String requestString) {
		if (requestString == null) {
			this.showNamedError("cannot get arguments from a NULL string");
			return null;
		}
		return requestString.split(FileTransferProtocol.DELIMITER);
	}

	/**
	 * List all files in corresponding file storage.
	 * Note: for now, only searching in current directory,
	 * and hiding hidden and non-file objects.
	 * @return byte[] of File[] containing list of files
	 * @throws ServerFailureException
	 */
	public byte[] listFiles() throws ServerFailureException {
		this.showNamedMessage("Creating list of files in current directory..");

		byte[] filesByteArray = null;
		File[] filesArray;
		
		try {
			filesArray = new File(this.fileStorage.toString()).listFiles(
					new FileFilter() {
						@Override
						public boolean accept(File file) {
							return !file.isHidden() && file.isFile();
						}
					});
		} catch (NullPointerException e) {
			this.showNamedError("Storage directory may not exist or does not contains any files:"
					+ " returning empty array");
			filesArray = new File[0];
		}
		
		try {
			filesByteArray = util.Bytes.serialiseObjectToByteArray(filesArray);
		} catch (IOException e) {
			throw new ServerFailureException(e.getLocalizedMessage());
		} 

		return filesByteArray;
	}

	/**
	 * Download a single file from the server to the client.
	 * @param fileToUpload to the client
	 * @param downloaderPort to upload to
	 * @throws ServerFailureException
	 */
	public void downloadSingle(File fileToUpload, int downloaderPort) 
			throws ServerFailureException {
		try { // to create uploader helper with file and port from request
			DatagramSocket uploadSocket = TransportLayer.openNewDatagramSocket();
			int fileSizeToUpload = (int) fileToUpload.length(); // TODO casting long to int!
			UploadHelper uploadHelper = new UploadHelper(this, uploadSocket, 
					this.clientAddress, downloaderPort, fileSizeToUpload, fileToUpload);
			this.uploads.add(uploadHelper);

			// start upload helper
			new Thread(uploadHelper).start();

			// let downloadHelper know about uploader
			byte[] singleFileResponse = (FileTransferProtocol.UPLOAD +
					FileTransferProtocol.DELIMITER +
					uploadSocket.getLocalPort() + 
					FileTransferProtocol.DELIMITER + 
					fileSizeToUpload +
					FileTransferProtocol.DELIMITER + 
					uploadHelper.getStartId()).getBytes();
			byte[] fileToUploadBytes = util.Bytes.serialiseObjectToByteArray(fileToUpload);
			this.sendBytesToClient(util.Bytes.concatArray(singleFileResponse, fileToUploadBytes),
					singleFileResponse.length); 
		} catch (IOException e) {
			throw new ServerFailureException(e.getLocalizedMessage());
		} 
	}

	/**
	 * Upload a single file to the server from the client.
	 * @param fileToDownload from the client
	 * @param uploaderPort to download from
	 * @param totalFileSize to download
	 * @param startID to start downloading with
	 * @throws ServerFailureException
	 * @throws NotEnoughFreeSpaceException
	 */
	public void uploadSingle(File fileToDownload, int uploaderPort, long totalFileSize, int startID) 
			throws ServerFailureException, NotEnoughFreeSpaceException {
		
		if (this.checkFreeSpace(totalFileSize)) {

			try { // to create uploader helper with file and port from request
				DatagramSocket downloadSocket = TransportLayer.openNewDatagramSocket();
				DownloadHelper downloadHelper = new DownloadHelper(this, downloadSocket, 
						this.clientAddress, uploaderPort, totalFileSize, fileToDownload, startID);
				this.downloads.add(downloadHelper);

				// start upload helper
				new Thread(downloadHelper).start();

				// let uploadHelper know about downloader
				byte[] singleFileResponse = (FileTransferProtocol.DOWNLOAD +
						FileTransferProtocol.DELIMITER +
						downloadSocket.getLocalPort()).getBytes(); 
				byte[] fileToDownloadBytes = util.Bytes.serialiseObjectToByteArray(fileToDownload);
				this.sendBytesToClient(util.Bytes.concatArray(singleFileResponse,
						fileToDownloadBytes), singleFileResponse.length);
			} catch (IOException e) {
				throw new ServerFailureException(e.getLocalizedMessage());
			}
		}
	}

	/**
	 * Delete a single file from the server.
	 * @param fileToDelete File to delete
	 * @throws ServerFailureException
	 */
	public void deleteSingle(File fileToDelete) throws ServerFailureException {
		try {
			boolean succes = fileToDelete.delete();

			if (succes) { // let client know about deletion 
				byte[] singleDeleteResponse = FileTransferProtocol.DELETE.getBytes();
				byte[] fileToDeleteBytes = util.Bytes.serialiseObjectToByteArray(fileToDelete);
				this.sendBytesToClient(util.Bytes.concatArray(singleDeleteResponse, 
						fileToDeleteBytes), singleDeleteResponse.length);
			} else {
				this.showNamedError("Deletion did not succeed!");
				throw new ServerFailureException("Deletion did not succeed!");
			}
		} catch (IOException e) {
			throw new ServerFailureException(e.getLocalizedMessage());
		} 
	}

	/**
	 * Check the hash of a file on the server, with matching file on client.
	 * @param fileToCheck on server
	 * @param hashOnClient as send by the client
	 * @throws ServerFailureException
	 */
	private void checkFile(File fileToCheck, String hashOnClient) throws ServerFailureException {
		try {
			String hashOnServer = util.FileOperations.getHashHexString(fileToCheck);

			byte[] checkFileResponse = (FileTransferProtocol.HASH +
					FileTransferProtocol.DELIMITER +
					hashOnServer).getBytes();
			byte[] fileToCheckBytes = util.Bytes.serialiseObjectToByteArray(fileToCheck); 
			this.sendBytesToClient(util.Bytes.concatArray(checkFileResponse, fileToCheckBytes),
					checkFileResponse.length);

			if (hashOnServer.equals(hashOnClient)) {
				this.showNamedMessage("Local and remote files have the same hash: INTEGRITY OK");
			} else {
				this.showNamedMessage("Local and remote files have the different hash:"
						+ " INTEGRITY FAILED");
			}
		} catch (NoSuchAlgorithmException | IOException e) {
			throw new ServerFailureException(e.getLocalizedMessage());
		}

	}

	/**
	 * Check if there is enough space to store the file.
	 * @param totalFileSize of the file
	 * @return true if there more than totalFileSize free space
	 * @throws NotEnoughFreeSpaceException
	 */
	public boolean checkFreeSpace(long totalFileSize) throws NotEnoughFreeSpaceException {
		long freeSpace = this.fileStorage.toFile().getUsableSpace(); 
		if (freeSpace > totalFileSize) {
			this.showNamedMessage("Free space remaining after upload: " 
					+ (freeSpace - totalFileSize) + " bytes");
			return true;
		} else {
			throw new NotEnoughFreeSpaceException(totalFileSize 
					+ "bytes don't fit in " + freeSpace + "bytes of free space");
		}
	}
	
	/**
	 * Send bytes to the client, contained in a Packet.
	 * @param bytesToSend to client
	 * @param byteOffset due to String part
	 */
	public void sendBytesToClient(byte[] bytesToSend, int byteOffset) {
		try { // to construct and send a packet
			Packet packet = new Packet(
					0,
					this.ownAddress,
					this.ownPort, 
					this.clientAddress, 
					this.clientPort,
					bytesToSend,
					byteOffset
					);

			TransportLayer.sendPacket(
					this.socket,
					packet,
					this.clientPort
			); 

			// this.showNamedMessage("Bytes send!"); // for debugging

		} catch (PacketException | IOException | UtilByteException | UtilDatagramException e) {
			this.showNamedError("Something went wrong while sending bytes: "
					+ e.getLocalizedMessage());
		}
	}

	/**
	 * Shutdown ClientHandler.
	 */
	public void shutdown() {
		this.showNamedMessage("ClientHandler is shutting down.");
		this.socket.close();

	}

	/**
	 * port of this ClientHandler.
	 * @return int port of this ClientHandler.
	 */
	public int getPort() {
		return this.ownPort;
	}

	/**
	 * Get name of client of this ClientHandler.
	 * @return String name of client of this ClientHandler.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Show message on the textUIT with name of client of this ClientHandler.
	 * @param message to display
	 */
	public void showNamedMessage(String message) {
		textUI.showNamedMessage("handler-" + this.name, message);
	}

	/**
	 * Show error on the textUIT with name of client of this ClientHandler.
	 * @param message to display
	 */
	public void showNamedError(String message) {
		textUI.showNamedError("handler-" + this.name, message);
	}

}
