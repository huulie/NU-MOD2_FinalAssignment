package exceptions;

/**
 * Exception to indicate server has failed.
 * @author huub.lievestro
 *
 */
public class ServerFailureException extends Exception {

	private static final long serialVersionUID = -2058685679682839546L;

	public ServerFailureException(String msg) {
		super(msg);
	}

}
