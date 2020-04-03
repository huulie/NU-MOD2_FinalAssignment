package exceptions;

/**
 * Exception to indicate a protocol violation.
 * @author huub.lievestro
 *
 */
public class ProtocolException extends Exception {

	private static final long serialVersionUID = 5574774762493692470L;

	public ProtocolException(String msg) {
		super(msg);
	}

}
