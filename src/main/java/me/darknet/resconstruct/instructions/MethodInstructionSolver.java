package me.darknet.resconstruct.instructions;

import me.coley.analysis.value.AbstractValue;
import me.darknet.resconstruct.ClassHierarchy;
import me.darknet.resconstruct.info.MethodMember;
import me.darknet.resconstruct.PhantomClass;
import me.darknet.resconstruct.util.TypeUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.analysis.Frame;

public class MethodInstructionSolver implements InstructionSolver<MethodInsnNode> {

	@Override
	public void solve(MethodInsnNode instruction, Frame<AbstractValue> frame, ClassHierarchy hierarchy) {
		// Infer argument types based on stack analysis
		inferArgumentTypes(instruction, frame, hierarchy);

		// Infer owner type based on stack analysis if there is method context on the stack
		if (instruction.getOpcode() != INVOKESTATIC)
			inferOwnerType(instruction, frame, hierarchy);
	}

	private void inferArgumentTypes(MethodInsnNode instruction, Frame<AbstractValue> frame, ClassHierarchy hierarchy) {
		// TODO: Do this
	}

	private void inferOwnerType(MethodInsnNode instruction, Frame<AbstractValue> frame, ClassHierarchy hierarchy) {
		int arguments = TypeUtils.getArgumentsSize(instruction.desc) + 1;
		AbstractValue ownerValue = frame.getStack(frame.getStackSize() - arguments);
		Type ownerType = Type.getObjectType(instruction.owner);
		Type wtf = ownerValue.getType();
		if (wtf == null)
			throw new IllegalStateException();
		PhantomClass phantomActual = hierarchy.getOrCreate(ownerType);
		PhantomClass phantomStack = hierarchy.getOrCreate(wtf);
		if (phantomActual != null) {
			// Method call -> method must exist
			phantomActual.methods.put(instruction.name, new MethodMember(ACC_PUBLIC, instruction.name, instruction.desc));
		}
		if (ownerType.equals(phantomStack.type)) {
			// If the stack type is not the same as the owner type that means that stack type inherits the owner type.
			// Though at this point we do not know if this is directly or not.
			phantomStack.inheritors.add(ownerType);
		}
	}
}
