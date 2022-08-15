package me.darknet.resconstruct;

/**
 * Exception type to handle various issues arising in phantom analysis.
 */
public class SolveException extends RuntimeException {
	/**
	 * @param cause
	 * 		Root problem.
	 */
	public SolveException(Exception cause) {
		super(cause);
	}

	/**
	 * @param cause
	 * 		Root problem.
	 * @param message
	 * 		Description of the problem.
	 */
	public SolveException(Exception cause, String message) {
		super(message, cause);
	}

	/**
	 * @param message
	 * 		Description of the problem.
	 */
	public SolveException(String message) {
		super(message);
	}
}
