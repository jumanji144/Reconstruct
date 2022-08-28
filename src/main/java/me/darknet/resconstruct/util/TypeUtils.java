package me.darknet.resconstruct.util;

import me.coley.analysis.TypeResolver;
import me.coley.analysis.util.TypeUtil;
import org.objectweb.asm.Type;

/**
 * Various type utils.
 */
public class TypeUtils {
	public static final Type ANNO_TYPE = Type.getObjectType("java/lang/annotation/Annotation");

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

	/**
	 * @param desc
	 * 		Method descriptor.
	 *
	 * @return Number of arguments.
	 */
	public static int getArgumentsCount(String desc) {
		Type methodType = Type.getMethodType(desc);
		return methodType.getArgumentTypes().length;
	}

	public static Type computeBestType(Type currentType, Type frameType, TypeResolver typeResolver) {
		Type commonType = (currentType.equals(frameType)) ? currentType :
				typeResolver.common(currentType, frameType);
		if (TypeUtil.OBJECT_TYPE.equals(commonType)) {
			// One of the types involved is not known to SimAnalyzer.
			// In this case we will trust the StackMapTable entry.
			return frameType;
		} else if (currentType.equals(commonType)) {
			// The current type is the common type, no decision needed.
			// Both are the same.
			return frameType;
		} else {
			// The common type is NOT the current type.
			// But it is also not "Object" so SimAnalyzer is aware of both involved types.
			// The "currentType" is likely more specific than "frameType" so we will use it.
			return currentType;
		}
	}
}
