package es.satec.rundeck.exceptions;

@SuppressWarnings("serial")
public class SessionException extends Exception {

	public SessionException() {
		super();
	}

	public SessionException(String message) {
		super(message);
	}

	public SessionException(String message, Throwable cause) {
		super(message, cause);
	}

	public SessionException(Throwable cause) {
		super(cause);
	}
}
