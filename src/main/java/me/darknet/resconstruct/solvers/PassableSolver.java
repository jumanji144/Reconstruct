package me.darknet.resconstruct.solvers;

import me.darknet.resconstruct.ClassHierarchy;
import org.objectweb.asm.tree.ClassNode;

public class PassableSolver implements Solver {

	private final int passes;

	public PassableSolver(int passes) {
		this.passes = passes;
	}

	@Override
	public void solve(ClassHierarchy classHierarchy, ClassNode classNode) {

	}
}
