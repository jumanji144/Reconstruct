package me.darknet.resconstruct.util;

import me.coley.analysis.util.InheritanceGraph;
import org.objectweb.asm.Type;

import java.io.IOException;

public class InheritanceUtils {
	private static final InheritanceGraph GRAPH_CP = new InheritanceGraph();

	public static InheritanceGraph getClasspathGraph() {
		return GRAPH_CP;
	}

	public static boolean isClasspathType(Type type) {
		// Not an object? Must be JVM type.
		if (type.getSort() != Type.OBJECT)
			return true;
		// Object check
		String internalName = type.getInternalName();
		if (internalName.equals("java/lang/Object"))
			return true;
		// Any non-object type will have at least Object as a parent.
		// If there is no record of it in the graph, then it will return an empty set.
		return !GRAPH_CP.getParents(internalName).isEmpty();
	}

	static {
		try {
			GRAPH_CP.addClasspath();
		} catch (IOException ex) {
			throw new IllegalStateException("xdark fix this later", ex);
		}
	}
}
