package me.darknet.resconstruct;

import me.coley.analysis.SimAnalyzer;
import me.coley.analysis.value.AbstractValue;
import me.darknet.resconstruct.instructions.InstructionSolver;
import me.darknet.resconstruct.instructions.MethodInstruction;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.HashMap;
import java.util.Map;

public class InstructionsSolver implements Solver, Opcodes {
	public static final Map<Class<? extends AbstractInsnNode>, InstructionSolver<? extends AbstractInsnNode>> instructionSolvers = new HashMap<>();
	private final Reconstruct reconstruct;

	static {
		instructionSolvers.put(MethodInsnNode.class, new MethodInstruction());
	}

	public InstructionsSolver(Reconstruct reconstruct) {
		this.reconstruct = reconstruct;
	}

	@Override
	public void solve(ClassHierarchy classHierarchy, ClassNode classNode) {
		SimAnalyzer analyzer = reconstruct.getAnalyzer();
		for (MethodNode method : classNode.methods) {
			try {
				InsnList instructions = method.instructions;
				Frame<AbstractValue>[] frames = analyzer.analyze(classNode.name, method);
				for (int i = 0; i < instructions.size(); i++) {
					AbstractInsnNode instruction = instructions.get(i);
					Frame<AbstractValue> frame = frames[i];
					InstructionSolver solver = instructionSolvers.get(instruction.getClass());
					if (solver != null) {
						solver.solve(instruction, frame, classHierarchy);
					}
				}
			} catch (AnalyzerException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
