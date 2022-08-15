package me.darknet.resconstruct;

/**
 * Exception type to handle various issues arising in phantom generation.
 */
public class GenerateException extends RuntimeException {
	/**
	 * @param cause
	 * 		Root problem.
	 */
	public GenerateException(Exception cause) {
		super(cause);
	}

	/**
	 * @param cause
	 * 		Root problem.
	 * @param message
	 * 		Description of the problem.
	 */
	public GenerateException(Exception cause, String message) {
		super(message, cause);
	}

	/**
	 * @param message
	 * 		Description of the problem.
	 */
	public GenerateException(String message) {
		super(message);
	}
}
