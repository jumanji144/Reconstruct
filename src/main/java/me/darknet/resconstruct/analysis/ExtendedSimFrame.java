package me.darknet.resconstruct.analysis;

import me.coley.analysis.SimFrame;
import me.coley.analysis.SimInterpreter;
import me.coley.analysis.util.InsnUtil;
import me.coley.analysis.value.AbstractValue;
import me.coley.analysis.value.PrimitiveValue;
import me.coley.analysis.value.VirtualValue;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;

import java.util.NavigableMap;

public class ExtendedSimFrame extends SimFrame {

	private final NavigableMap<Integer, FrameNode> stackFrames;
	private boolean isFirst = true;

	public ExtendedSimFrame(NavigableMap<Integer, FrameNode> stackFrames, int numLocals, int numStack) {
		super(numLocals, numStack);
		this.stackFrames = stackFrames;
	}

	public ExtendedSimFrame(NavigableMap<Integer, FrameNode> stackFrames, SimFrame frame) {
		super(frame);
		this.stackFrames = stackFrames;
	}

	@Override
	public boolean merge(Frame<? extends AbstractValue> frame, boolean[] localsUsed) {
		isFirst = false;
		return super.merge(frame, localsUsed);
	}

	@Override
	public Frame<AbstractValue> init(Frame<? extends AbstractValue> frame) {
		isFirst = true;
		return super.init(frame);
	}

	@Override
	public void execute(AbstractInsnNode insn, Interpreter<AbstractValue> interpreter) throws AnalyzerException {
		super.execute(insn, interpreter);
		if(isFirst) {
			SimInterpreter simInterpreter = (SimInterpreter) interpreter;
			isFirst = false;
			if(stackFrames.isEmpty()) return;
			var entry  = stackFrames.floorEntry(InsnUtil.index(insn));
			if(entry == null) return;
			FrameNode frame = entry.getValue();
			if(frame.type != Opcodes.F_NEW) throw new AnalyzerException(insn, "Invalid stack map frame, expected: F_NEW");
			for(int i = 0; i < frame.local.size(); i++) {
				AbstractValue v = getLocal(i);
				if(v instanceof VirtualValue) {
					VirtualValue vv = (VirtualValue) v;
					Type t = Type.getType(frame.local.get(i).getClass());
					setLocal(i, VirtualValue.ofVirtual(vv.getInsns(), simInterpreter.getTypeChecker(), t));
				}
			}
		}
	}
}
