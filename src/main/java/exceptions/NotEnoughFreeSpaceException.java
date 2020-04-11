package exceptions;

/**
 * Exception to indicate there is not enough disk space.
 * @author huub.lievestro
 *
 */
public class NotEnoughFreeSpaceException extends Exception {

	private static final long serialVersionUID = -5051923228571077255L;

	public NotEnoughFreeSpaceException(String msg) {
		super(msg);
	}

}
