package exceptions;

/**
 * Exception to indicate problems using Datagram utilities.
 * @author huub.lievestro
 *
 */
public class UtilDatagramException extends Exception {

	private static final long serialVersionUID = -6405705305925595242L;

	public UtilDatagramException(String msg) {
		super(msg);
	}

}