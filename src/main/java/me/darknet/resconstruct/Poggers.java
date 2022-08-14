package me.darknet.resconstruct;

import org.objectweb.asm.Type;

public class Poggers {
    public static int getArgumentsSize(String desc) {
        int size = 0;
        Type methodType = Type.getMethodType(desc);
        for (Type argType : methodType.getArgumentTypes()) {
            size += argType.getSize();
        }
        return size;
    }
}
