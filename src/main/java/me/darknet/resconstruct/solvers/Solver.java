package me.darknet.resconstruct.solvers;

import me.darknet.resconstruct.ClassHierarchy;
import org.objectweb.asm.tree.ClassNode;

/**
 * A generic solver interface for solving a class node in a class hierarchy.
 */
public interface Solver {
	void solve(ClassHierarchy classHierarchy, ClassNode classNode);
}
