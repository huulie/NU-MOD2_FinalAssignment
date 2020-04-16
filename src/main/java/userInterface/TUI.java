package userInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

import exceptions.ExitProgram;

/** 
 * General TUI for the FileTransfer server and client.
 * This class handles all reading and writing, 
 * and could leave implementation of command handling to a more specific TUI
 * 
 * @author huub.lievestro
 *
 */
public class TUI  {

	/**
	 * Writes the given message to system output.
	 * @param msg the message to write to the system output.
	 */
	public void showMessage(String message) {
		System.out.println(message);
	}
	
	/**
	 * Writes the given message to system output, including the name.
	 * @param name to identify caller
	 * @param message the message to write to the system output.
	 */
	public void showNamedMessage(String name, String message) {
		this.showMessage("[" + name + "]: " + message);
	}
	
	/**
	 * Writes the given message with error prefix to system output.
	 * @param msg the message to write to the system output.
	 */
	public void showError(String message) {
		System.err.println(">ERROR: " + message);
	}
	
	/**
	 * Writes the given error to system output, including the name.
	 * @param name to identify caller
	 * @param message the error to write to the system output.
	 */
	public void showNamedError(String name, String message) {
		System.err.println("[" + name + "]>ERROR: " + message);
	}

	/**
	 * Prints the question and asks the user to input a String.
	 * @param question the question shown to the user, asking for input
	 * @return The user input as a String
	 */
	public String getString(String question) {
		this.showMessage(question); // manual new line, for better layout (no extra white lines)
        String antw = null;
        try {
        	BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            antw = in.readLine();
        } catch (IOException e) {
        	this.showMessage("IO exception: " + e.getLocalizedMessage());
        }
        return (antw == null) ? "" : antw;
	}

	/**
	 * Prints the question and asks the user to input an Integer.
	 * @param question the question shown to the user, asking for input
	 * @return The written Integer.
	 */
	public int getInt(String question) {
		String answer = null;
		int answerInt = 0;
		Boolean answerValid = false;

		while (!answerValid) {
			this.showMessage(question); 
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
				answer = in.readLine();

				answerInt = Integer.parseInt(answer);
				answerValid = true;
			} catch (NumberFormatException eFormat) {
				this.showMessage("ERROR> " + answer +  " is not an integer (" 
						+ eFormat.getLocalizedMessage() + ") try again!");
			} catch (IOException e) {
				this.showMessage("IO Exception occurred");
			}
		}
        return answerInt;
	}

	/**
	 * Prints the question and asks the user for a yes/no answer.
	 * @param question the question shown to the user, asking for input
	 * @return The user input as boolean.
	 */
	public boolean getBoolean(String question) {
		String answer = null;
		Boolean answerBool = false;
		Boolean answerValid = false;

		while (!answerValid) {
			this.showMessage(question); 
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
				answer = in.readLine();

				// answerBool = Boolean.parseBoolean(answer); 
				// PM: use own switch, so everything not [T]/[t]rue is not automatically false
				answerValid = true;
				if (answer.equalsIgnoreCase("true") 
						|| answer.equalsIgnoreCase("yes") 
						|| answer.equalsIgnoreCase("y")) {
					answerBool = true;
				} else if (answer.equalsIgnoreCase("false") 
						|| answer.equalsIgnoreCase("no") 
						|| answer.equalsIgnoreCase("n")) {
					answerBool = false;
				} else {
					answerValid = false;
				}
			} catch (IOException e) {
				this.showMessage("IO Exception occurred");
			}
		}
        return answerBool;
	}
	
	/**
	 * Prints the help menu with available input options.
	 */
	public void printHelpMenu() {
		this.showMessage("Commands:");
		this.showMessage("- " + TUICommands.START_SESSION + " = start a session with the server");
		this.showMessage("- " + TUICommands.LIST_FILES + " = list files on the server");
		this.showMessage("- " + TUICommands.LIST_FILES_LOCAL + " = list files on this client");
		this.showMessage("- " + TUICommands.DOWNLOAD_SINGLE + " = download a single file from the server");
		this.showMessage("- " + TUICommands.UPLOAD_SINGLE + " = upload a single file to the server");
		this.showMessage("- " + TUICommands.DELETE_SINGLE + " = delete a single file from the server");
		this.showMessage("- " + TUICommands.CHECK_INTEGRITY + " = check integrity of single file"
				+ " against the server");
		this.showMessage("- " + TUICommands.DOWNLOAD_MANAGER + " = manage downloaders");
		this.showMessage("- " + TUICommands.UPLOAD_MANAGER + " = manage uploaders");
		this.showMessage("- " + TUICommands.HELP + " = help (this menu)");
		this.showMessage("- " + TUICommands.EXIT + " = exit client");
	}
	
}
