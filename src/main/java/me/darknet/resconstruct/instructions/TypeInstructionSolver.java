package me.darknet.resconstruct.instructions;

import me.coley.analysis.SimFrame;
import me.coley.analysis.value.AbstractValue;
import me.darknet.resconstruct.ClassHierarchy;
import me.darknet.resconstruct.PhantomClass;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.TypeInsnNode;

public class TypeInstructionSolver implements InstructionSolver<TypeInsnNode> {
	@Override
	public void solve(TypeInsnNode instruction, SimFrame frame, ClassHierarchy hierarchy) {
		if (instruction.getOpcode() == CHECKCAST) {
			AbstractValue value = frame.getStack(frame.getStackSize() - 1);
			// checkcast infers that the type is the same as the stack type (or inherits the stack type)
			PhantomClass phantomStack = hierarchy.getOrCreate(Type.getObjectType(instruction.desc));
			PhantomClass phantomValue = hierarchy.getOrCreate(value.getType());
			if (!value.getType().equals(phantomStack.getType())) {
				// adding a type hint will imply that phantomStack is a subclass of phantomValue
				phantomStack.addTypeHint(phantomValue);
			}
		}
	}
}
