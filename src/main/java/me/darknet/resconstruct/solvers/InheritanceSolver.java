package me.darknet.resconstruct.solvers;

import me.darknet.resconstruct.ClassHierarchy;
import me.darknet.resconstruct.PhantomClass;
import me.darknet.resconstruct.util.PhantomUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Solver that solves inheritance chains based on a given class hierarchy.
 */
public class InheritanceSolver extends PassableSolver {

	public InheritanceSolver() {
		addPass(this::pass0);
		addPass(this::pass1);
	}

	public void pass0(ClassHierarchy classHierarchy, ClassNode classNode) {
		// simple solve routine: assume all giving information is already correct
		for (PhantomClass value : classHierarchy.getPhantoms().values()) {
			if(!PhantomUtil.isObject(value)) { // class is not object -> must be an interface
				value.setAccess(value.getAccess() | (Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT));
			}
			for (PhantomClass implementCandidate : value.getImplementCandidates()) {
				value.addImplements(implementCandidate);
			}
		}
	}

	public void pass1(ClassHierarchy classHierarchy, ClassNode classNode) {
		// solve for redundant interfaces
		for (PhantomClass value : classHierarchy.getPhantoms().values()) {
			List<Type> toRemove = new ArrayList<>();
			for(PhantomClass implement : value.getImplements()) {
				// check if other parents implement this interface
				for(PhantomClass otherParent : value.getImplements()) {
					if(otherParent != implement && otherParent.implementsInterface(implement.getType())) {
						// remove the interface from the current parent
						toRemove.add(implement.getType());
						break;
					}
				}
			}
			for(Type type : toRemove) {
				value.removeImplements(type);
			}
 		}
	}
}
