package me.darknet.resconstruct.solvers;

import me.darknet.resconstruct.ClassHierarchy;
import me.darknet.resconstruct.PhantomClass;
import me.darknet.resconstruct.Solver;
import me.darknet.resconstruct.util.PhantomUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

public class InheritanceSolver implements Solver {
	@Override
	public void solve(ClassHierarchy classHierarchy, ClassNode classNode) {
		// simple solve routine: assume all giving information is already correct
		for (PhantomClass value : classHierarchy.getPhantoms().values()) {
			if(!PhantomUtil.isObject(value)) { // class is not object -> must be an interface
				value.setAccess(value.getAccess() | (Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT));
			}
			for (PhantomClass implementCandidate : value.getImplementCandidates()) {
				value.addImplements(implementCandidate.getType());
			}
		}
	}
}
