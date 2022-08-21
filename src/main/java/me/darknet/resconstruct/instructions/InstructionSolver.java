package me.darknet.resconstruct.instructions;

import me.coley.analysis.SimFrame;
import me.darknet.resconstruct.ClassHierarchy;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

public interface InstructionSolver<T extends AbstractInsnNode> extends Opcodes {

	void solve(T instruction, SimFrame frame, ClassHierarchy hierarchy);

}
