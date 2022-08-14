package me.darknet.resconstruct.instructions;

import me.coley.analysis.value.AbstractValue;
import me.darknet.resconstruct.ClassHierarchy;
import me.darknet.resconstruct.MethodMember;
import me.darknet.resconstruct.PhantomClass;
import me.darknet.resconstruct.Poggers;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.Objects;

public class MethodInstruction implements InstructionSolver<MethodInsnNode> {

	@Override
	public void solve(MethodInsnNode instruction, Frame<AbstractValue> frame, ClassHierarchy hierarchy) {
		if (instruction.getOpcode() == INVOKESTATIC) {
			// TODO: Can only infer argument types based on stack analysis
		}
        // Infer owner type
		int arguments = Poggers.getArgumentsSize(instruction.desc);
		if (instruction.getOpcode() != INVOKESTATIC) {
			arguments++;
		}
		AbstractValue value = frame.getStack(frame.getStackSize() - arguments);
		Type ownerType = Type.getObjectType(instruction.owner);
		PhantomClass clazz = hierarchy.get(ownerType); // should always be an object
		if (clazz != null) { // basic implications of a method call -> method must exist
			clazz.methods.put(instruction.name, new MethodMember(ACC_PUBLIC, instruction.name, instruction.desc));
		}
		PhantomClass actual = hierarchy.get(value.getType()); // get the actual class of the method value
		if (!Objects.equals(actual.type, ownerType)) { // if the actual class is not the same as the owner
			// that means that actual inherits owner somewhere down the chain
			// add it to possible inheritors of owner
			actual.inheritors.add(ownerType);
		}
	}
}
