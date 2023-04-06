package me.darknet.resconstruct.solvers;

import me.darknet.resconstruct.ClassHierarchy;
import me.darknet.resconstruct.util.TriConsumer;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * A solver allowing for multiple passes over the class hierarchy.
 */
public class PassableSolver implements Solver {

	private final List<TriConsumer<ClassHierarchy, ClassNode, Integer>> passes = new ArrayList<>();

	protected void addPass(BiConsumer<ClassHierarchy, ClassNode> pass) {
		passes.add((classHierarchy, classNode, passNumber) -> pass.accept(classHierarchy, classNode));
	}

	@Override
	public void solve(ClassHierarchy classHierarchy, ClassNode classNode) {
		for (int i = 0; i < passes.size(); i++) {
			passes.get(i).accept(classHierarchy, classNode, i);
		}
	}
}
