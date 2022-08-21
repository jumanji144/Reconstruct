package me.darknet.resconstruct.instructions;

import me.coley.analysis.SimFrame;
import me.coley.analysis.TypeResolver;
import me.coley.analysis.util.TypeUtil;
import me.coley.analysis.value.AbstractValue;
import me.darknet.resconstruct.ClassHierarchy;
import me.darknet.resconstruct.PhantomClass;
import me.darknet.resconstruct.util.TypeUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.VarInsnNode;

public class VarInstructionSolver implements InstructionSolver<VarInsnNode> {

	private final TypeResolver typeResolver;

	public VarInstructionSolver(TypeResolver resolver) {
		this.typeResolver = resolver;
	}

	@Override
	public void solve(VarInsnNode instruction, SimFrame frame, ClassHierarchy hierarchy) {
		if (instruction.getOpcode() == Opcodes.ASTORE) {
			solveStore(instruction, frame, hierarchy);
		}
	}

	public void solveStore(VarInsnNode insnNode, SimFrame frame, ClassHierarchy hierarchy) {
		SimFrame next = frame.getFlowOutputs().iterator().next();
		AbstractValue localValue = next.getLocal(insnNode.var);
		AbstractValue stackValue = frame.getStack(frame.getStackSize() - 1);
		Type stackType = stackValue.getType();
		Type localType = localValue.getType();
		PhantomClass phantomActual = hierarchy.getOrCreate(stackType);
		PhantomClass phantomLocal = hierarchy.getOrCreate(localType);
		Type commonType = TypeUtils.computeBestType(localType, stackType, typeResolver);
		if(commonType.equals(TypeUtil.OBJECT_TYPE) && !stackType.equals(TypeUtil.OBJECT_TYPE)) {
			phantomLocal.addInheritor(commonType);
		}
	}

}
