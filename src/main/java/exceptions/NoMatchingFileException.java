package exceptions;

/**
 * Exception to indicate no matching file was found on the remote.
 * @author huub.lievestro
 *
 */
public class NoMatchingFileException extends Exception {

	private static final long serialVersionUID = -7842712606038242628L;

	public NoMatchingFileException(String msg) {
		super(msg);
	}

}
