package me.darknet.resconstruct.instructions;

import me.coley.analysis.SimFrame;
import me.coley.analysis.value.AbstractValue;
import me.darknet.resconstruct.ClassHierarchy;
import me.darknet.resconstruct.PhantomClass;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldInsnNode;

public class FieldInstructionSolver implements InstructionSolver<FieldInsnNode> {

	@Override
	public void solve(FieldInsnNode instruction, SimFrame frame, ClassHierarchy hierarchy) {
		int opcode = instruction.getOpcode();
		if(opcode == PUTFIELD || opcode == PUTSTATIC) {
			AbstractValue stackValue = frame.getStack(frame.getStackSize() - 1); // get stack value
			PhantomClass owner = hierarchy.getOrCreate(Type.getObjectType(instruction.owner));
			owner.addFieldUsage(opcode, instruction.name, stackValue.getType().getDescriptor());
		}
	}

}
