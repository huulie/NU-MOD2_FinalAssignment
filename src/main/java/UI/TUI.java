package UI;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

import exceptions.ExitProgram;

/** 
 * General TUI (view) for the GO game.
 * This class handles all reading and writing, and leaves implementation of command handling to a more specific TUI
 * Input could be sent to a Local Player or, via Client <-> ClientHandler, to a Remote Player
 * 
 * @author huub.lievestro
 *
 */
public class TUI  { // TODO implements Runnable ?
	
	/**
	 * Starts TUI (thread): asks for user input continuously and handles communication accordingly using
	 * the {@link #handleUserInput(String input)} method.
	 * 
	 * If an ExitProgram exception is thrown, stop asking for input, send an exit message to the controller
	 * 
	 */
	public void start() {
		String input;
		try {
			input = this.getString("Input your command or " + TUICommands.HELP + " for help:");
			while (input != null) {
				handleUserInput(input);
				input = this.getString(" ");
			}
		} catch (ExitProgram eExit) {
			// To implement: controller."exit"
			this.showMessage("User requested exit: TUI quit");
		}
	}

	/**
	 * Split the user input on a space and handle it accordingly. 
	 * - If the input is valid, take the corresponding action via the controller
	 * - If the input is invalid, show a message to the user and print the help menu.
	 * 
	 * @param input The user input.
	 * @throws ExitProgram When the user has indicated to exit the program.
	 */
	public void handleUserInput(String input) throws ExitProgram {
		// To be implemented by a specific TUI, here only HELP and EXIT implemented as an example
		
		String [] split = input.split("\\s+");

		char command = split[0].charAt(0);
		
		String param = null; // To be implemented: one or more parameters may be needed
		if (split.length > 1) {
			param = split[1];
		}

		switch (command) {

		case TUICommands.HELP:
			this.showMessage(" - Help for the GO game - ");
			printHelpMenu();
			break;
			
			case 'x': //TUICommands.EXIT: TODO make this string/char compatible
    			String confirmation = null;

    			while (confirmation == null) {
    				confirmation = this.getString("Are you sure? [Y]es / [N]o ");

    				if (confirmation.equalsIgnoreCase("Y")) {
    					this.showMessage("Bye bye! ");
    					throw new ExitProgram("User requested EXIT");
    				} else if (confirmation.equalsIgnoreCase("N")) {
    					this.showMessage("Okay, continue to run client ");
    				} else {
    					confirmation = null; // all other inputs are ignored
    				}
    			}
    			break;
		
    		default:
    			System.out.println("I don't understand this command, try again");
    			System.out.println("Maybe this helps:");
    			printHelpMenu();
		}
	}

	/**
	 * Writes the given message to system output.
	 * 
	 * @param msg the message to write to the system output.
	 */
	public void showMessage(String message) {
		System.out.println(message);
	}
	
	/**
	 * Writes the given message with error prefix to system output.
	 * 
	 * @param msg the message to write to the system output.
	 */
	public void showError(String message) {
		showMessage(">ERROR: " + message);
	}

	/**
	 * Ask the user to input a valid IP. If it is not valid, show a message and ask again.
	 * 
	 * @param question the question shown to the user, asking for input
	 * @return a valid IP
	 */
	public InetAddress getIp(String question) {
		String answer = null;
		InetAddress answerIP = null;
		Boolean answerValid = false;

		while (!answerValid) {
			this.showMessage(question); 
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
				answer = in.readLine();
				answerIP = InetAddress.getByName(answer);
				answerValid = true;
			} catch (UnknownHostException eFormat) {
				this.showMessage("ERROR> " + answer +  " is not found (" 
						+ eFormat.getLocalizedMessage() + ") try again!");
			} catch (IOException e) {
				this.showMessage("IO Exception occurred");
			}
		}
		return answerIP;
	}

	/**
	 * Prints the question and asks the user to input a String.
	 * 
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
	 * 
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
	 * 
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
	 * Prints the question and asks the user to input an Move (int, with -1 for PASS).
	 * 
	 * @param question the question shown to the user, asking for input
	 * @return The written Integer, or -1 if PASS.
	 */
	public int getMove(String question) {
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
//				if (answer.equalsIgnoreCase(String.valueOf(TUICommands.PASS))) {
//					return GoGameConstants.PASSint;
//				} else {
					this.showMessage("ERROR> " + answer +  " is not an integer nor a PASS (" 
							+ eFormat.getLocalizedMessage() + ") try again!");
//				}
			} catch (IOException e) {
				this.showMessage("IO Exception occurred");
			}
		}
        return answerInt;
	}

	/**
	 * Prints the help menu with available input options.
	 */
	public void printHelpMenu() {
		// To be implemented by a specific TUI, here only HELP and EXIT implemented as an example
		this.showMessage("Commands:");
		this.showMessage(TUICommands.HELP + " ................ help (this menu)");
		System.out.println(TUICommands.EXIT + " ................ exit program");
	}
	
}
