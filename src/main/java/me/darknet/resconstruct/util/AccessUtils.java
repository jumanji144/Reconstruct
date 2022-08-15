package me.darknet.resconstruct.util;

import static org.objectweb.asm.Opcodes.ACC_STATIC;

public class AccessUtils {
	public static boolean isStatic(int access) {
		return (access & ACC_STATIC) > 0;
	}
}
