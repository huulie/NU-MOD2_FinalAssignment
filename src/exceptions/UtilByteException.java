package exceptions;

/**
 * Exception to indicate problems using Byte utilities.
 * @author huub.lievestro
 *
 */
public class UtilByteException extends Exception {

	private static final long serialVersionUID = 7411844387716841239L;

	public UtilByteException(String msg) {
		super(msg);
	}

}