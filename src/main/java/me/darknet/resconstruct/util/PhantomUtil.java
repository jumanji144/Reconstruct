package me.darknet.resconstruct.util;

import me.darknet.resconstruct.PhantomClass;

public class PhantomUtil {

	public static boolean isObject(PhantomClass phantomClass) {
		if(phantomClass.isObject()) return true;
		for (String name : phantomClass.getMethods().keySet()) {
			if(name.startsWith("<init>"))
				return true;
		}
		return false;
	}

}
