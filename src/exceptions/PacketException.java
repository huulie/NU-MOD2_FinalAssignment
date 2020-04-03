package exceptions;

/**
 * Exception to indicate a protocol violation.
 * @author huub.lievestro
 *
 */
public class PacketException extends Exception {

	private static final long serialVersionUID = 1771407584722448897L;

	public PacketException(String msg) {
		super(msg);
	}

}