package me.darknet.resconstruct.util;

import me.coley.analysis.util.InheritanceGraph;
import org.objectweb.asm.Type;

import java.io.IOException;

/**
 * Various inheritance utils.
 */
public class InheritanceUtils {
	private static final InheritanceGraph GRAPH_CP = new InheritanceGraph();

	/**
	 * @return Inheritance graph holding information about classpath classes.
	 */
	public static InheritanceGraph getClasspathGraph() {
		return GRAPH_CP;
	}

	/**
	 * @param type
	 * 		Type to check.
	 *
	 * @return {@code true} if the given type is loaded in the {@link #getClasspathGraph() current classpath}.
	 */
	public static boolean isClasspathType(Type type) {
		// Not an object? Must be JVM type.
		if (type.getSort() != Type.OBJECT)
			return true;
		// Object check
		String internalName = type.getInternalName();
		if (internalName.equals("java/lang/Object"))
			return true;
		// If the graph has lookups for the type, it belongs to the classpath
		return GRAPH_CP.hasChildrenLookup(internalName) || GRAPH_CP.hasParentLookup(internalName);
	}

	static {
		// Handle the standard classpath. This shouldn't fail.
		try {
			GRAPH_CP.addClasspath();
		} catch (IOException ex) {
			throw new IllegalStateException("Failed to generate inheritance graph from classpath", ex);
		}
		// Handle adding the runtime.
		// For Java 8 and below, this is "rt.jar".
		// For Java 9+, this is the contents of the modules.
		try {
			// Attempt RT.jar, will throw IOException if the file is not found.
			GRAPH_CP.addRtJar();
		} catch (IOException ignored) {
			// Likely on Java 9+, so try the module path instead.
			GRAPH_CP.addModulePath();
		}
	}
}
