package me.darknet.resconstruct.instructions;

import me.coley.analysis.value.AbstractValue;
import me.darknet.resconstruct.ClassHierarchy;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

public interface InstructionSolver<T extends AbstractInsnNode> extends Opcodes {

	void solve(T instruction, Frame<AbstractValue> frame, ClassHierarchy hierarchy);

}
