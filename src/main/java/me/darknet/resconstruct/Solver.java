package me.darknet.resconstruct;

import org.objectweb.asm.tree.ClassNode;

public interface Solver {
	void solve(ClassHierarchy classHierarchy, ClassNode classNode);
}
