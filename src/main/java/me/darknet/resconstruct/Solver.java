package me.darknet.resconstruct;

import me.darknet.resconstruct.ClassHierarchy;
import org.objectweb.asm.tree.ClassNode;

public interface Solver {
	void solve(ClassHierarchy classHierarchy, ClassNode classNode);
}
