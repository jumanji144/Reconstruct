package me.darknet.resconstruct.util;

import static org.objectweb.asm.Opcodes.ACC_INTERFACE;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

/**
 * Utility calls for checking access flags.
 */
public class AccessUtils {
	/**
	 * @param access
	 * 		Flags.
	 *
	 * @return {@code true} if flags contains {@code static}.
	 */
	public static boolean isStatic(int access) {
		return is(access, ACC_STATIC);
	}

	/**
	 * @param access
	 * 		Flags.
	 *
	 * @return {@code true} if flags contains {@code interface}.
	 */
	public static boolean isInterface(int access) {
		return is(access, ACC_INTERFACE);
	}

	/**
	 * @param access
	 * 		Flags.
	 * @param flag
	 * 		Flag(s) to check for.
	 *
	 * @return {@code true} if flags contains {@code flag} mask.
	 */
	public static boolean is(int access, int flag) {
		return (access & flag) > 0;
	}
}
