package me.darknet.resconstruct.instructions;

import me.coley.analysis.SimFrame;
import me.coley.analysis.value.AbstractValue;
import me.coley.analysis.value.UninitializedValue;
import me.darknet.resconstruct.ClassHierarchy;
import me.darknet.resconstruct.PhantomClass;
import me.darknet.resconstruct.util.TypeUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;

public class MethodInstructionSolver implements InstructionSolver<MethodInsnNode> {

	@Override
	public void solve(MethodInsnNode instruction, SimFrame frame, ClassHierarchy hierarchy) {
		// Infer argument types based on stack analysis
		inferArgumentTypes(instruction, frame, hierarchy);
		// Infer owner type based on stack analysis if there is method context on the stack
		if (instruction.getOpcode() != INVOKESTATIC)
			inferOwnerType(instruction, frame, hierarchy);
	}

	private void inferArgumentTypes(MethodInsnNode instruction, SimFrame frame, ClassHierarchy hierarchy) {
		int offset = 1;
		Type methodType = Type.getMethodType(instruction.desc);
		Type[] argTypes = methodType.getArgumentTypes();
		for (int i = argTypes.length - 1; i >= 0; i--) {
			Type argType = argTypes[i];
			AbstractValue argValue = frame.getStack(frame.getStackSize() - offset);
			Type stackType = argValue.getType();
			PhantomClass phantomActual = hierarchy.getOrCreate(argType);
			PhantomClass phantomStack = hierarchy.getOrCreate(stackType);
			if (!argType.equals(phantomStack.getType())) {
				// If the stack type is not the same as the argument type that means that stack type inherits the argument type.
				// Though at this point we do not know if this is directly or not.
				phantomStack.addImplementCandidate(phantomActual);
			}
			offset += argType.getSize();
		}
	}

	private void inferOwnerType(MethodInsnNode instruction, SimFrame frame, ClassHierarchy hierarchy) {
		// ASM's analysis framework does not handle 'wide' values, so we don't need to
		// consider the width of values. Therefore, just count the number of arguments.
		int offset = TypeUtils.getArgumentsCount(instruction.desc) + 1;
		AbstractValue ownerValue = frame.getStack(frame.getStackSize() - offset);
		if (ownerValue == UninitializedValue.UNINITIALIZED_VALUE) {
			return;
		}
		Type ownerType = Type.getObjectType(instruction.owner);
		Type stackType = ownerValue.getType();
		PhantomClass phantomActual = hierarchy.getOrCreate(ownerType);
		PhantomClass phantomStack = hierarchy.getOrCreate(stackType);
		if (!ownerType.equals(phantomStack.getType())) {
			// If the stack type is not the same as the owner type that means that stack type inherits the owner type.
			// Though at this point we do not know if this is directly or not.
			phantomStack.addImplementCandidate(phantomActual);
		}
	}
}
