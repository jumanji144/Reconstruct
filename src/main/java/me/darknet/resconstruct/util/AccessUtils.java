package me.darknet.resconstruct.util;

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
		return (access & ACC_STATIC) > 0;
	}
}
