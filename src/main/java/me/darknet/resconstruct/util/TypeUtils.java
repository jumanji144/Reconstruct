package me.darknet.resconstruct.util;

import org.objectweb.asm.Type;

public class TypeUtils {
    public static int getArgumentsSize(String desc) {
        int size = 0;
        Type methodType = Type.getMethodType(desc);
        for (Type argType : methodType.getArgumentTypes()) {
            size += argType.getSize();
        }
        return size;
    }
}
