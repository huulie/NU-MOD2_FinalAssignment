package exceptions;

/**
 * Exception to indicate response from remote was empty/null.
 * @author huub.lievestro
 *
 */
public class EmptyResponseException extends Exception {

	private static final long serialVersionUID = 8913705158729812409L;

	public EmptyResponseException(String msg) {
		super(msg);
	}

}
