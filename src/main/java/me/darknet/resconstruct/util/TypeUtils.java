package me.darknet.resconstruct.util;

import org.objectweb.asm.Type;

/**
 * Various type utils.
 */
public class TypeUtils {
	/**
	 * @param desc
	 * 		Method descriptor.
	 *
	 * @return Stack size usage of all parameters combined.
	 */
	public static int getArgumentsSize(String desc) {
		int size = 0;
		Type methodType = Type.getMethodType(desc);
		for (Type argType : methodType.getArgumentTypes()) {
			size += argType.getSize();
		}
		return size;
	}
}
