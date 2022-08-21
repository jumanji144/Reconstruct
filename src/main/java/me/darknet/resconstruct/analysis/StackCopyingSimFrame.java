package me.darknet.resconstruct.analysis;

import me.coley.analysis.SimFrame;
import me.coley.analysis.SimInterpreter;
import me.coley.analysis.util.InsnUtil;
import me.coley.analysis.value.AbstractValue;
import me.coley.analysis.value.VirtualValue;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;

import java.util.Map;
import java.util.NavigableMap;

/**
 * Frame implementation that enriches local variable type analysis from {@code StackMapTable}.
 */
public class StackCopyingSimFrame extends SimFrame {
	private final NavigableMap<Integer, FrameNode> stackFrames;
	private StackCopyingSimFrame lastInitted;

	/**
	 * @param stackFrames
	 * 		Map of stack frames supplied by {@code StackMapTable}.
	 * 		Only keyframes included, so we use {@link NavigableMap} to do floor lookups.
	 * @param numLocals
	 * 		Maximum number of local variables of the frame.
	 * @param numStack
	 * 		Maximum stack size of the frame.
	 */
	public StackCopyingSimFrame(NavigableMap<Integer, FrameNode> stackFrames, int numLocals, int numStack) {
		super(numLocals, numStack);
		this.stackFrames = stackFrames;
	}

	/**
	 * @param stackFrames
	 * 		Map of stack frames supplied by {@code StackMapTable}.
	 * 		Only keyframes included, so we use {@link NavigableMap} to do floor lookups.
	 * @param frame
	 * 		Old frame.
	 */
	public StackCopyingSimFrame(NavigableMap<Integer, FrameNode> stackFrames, SimFrame frame) {
		super(frame);
		this.stackFrames = stackFrames;
	}

	@Override
	public Frame<AbstractValue> init(Frame<? extends AbstractValue> frame) {
		// The 'this' frame is the value that will be put into the results of `SimAnalyzer.analyze(...)`.
		// But the interpreter is operating off of a single frame which gets constantly updated.
		// That frame is passed to us, so we call it the 'executing' frame.
		StackCopyingSimFrame executingFrame = (StackCopyingSimFrame) frame;
		// We need to tell the executing frame to enrich data in 'this' frame,
		// so we pass it a reference to ourselves. It will be used in 'execute' below.
		executingFrame.lastInitted = this;
		return super.init(frame);
	}

	@Override
	public void execute(AbstractInsnNode insn, Interpreter<AbstractValue> interpreter) throws AnalyzerException {
		super.execute(insn, interpreter);
		// There are not stack-frames per each instruction, but per "blocks" split up by labels.
		// The stack-frame entry is at the beginning of each block. So for any instruction in the block,
		// we can get the associated frame by doing a floor lookup of the current instruction index.
		int insnIndex = InsnUtil.index(insn);
		Map.Entry<Integer, FrameNode> entry = stackFrames.floorEntry(insnIndex);
		if (entry == null) {
			// It is possible that a frame does not yet exist, likely with code that doesn't have any need
			// to be enriched anyways, so it is ok to stop here.
			return;
		}
		// Sanity check frame type, we use 'ClassReader.EXPAND_FRAMES' so the frames should always
		// be in expanded form, which effectively makes them operate like a FULL frame, even if the
		// frame type in the bytecode is something like APPEND.
		// This makes look-ups like 'locals.get(i)' simple since they "just work".
		FrameNode frame = entry.getValue();
		if (frame.type != Opcodes.F_NEW)
			throw new AnalyzerException(insn, "Invalid stack map frame, expected: F_NEW");
		SimInterpreter simInterpreter = (SimInterpreter) interpreter;
		for (int i = 0; i < frame.local.size(); i++) {
			AbstractValue value = getLocal(i);
			// We only care about object/virtual values
			if (value instanceof VirtualValue) {
				VirtualValue virtual = (VirtualValue) value;
				// Get the type in the stack-frame at the current index.
				// The 'local' value should be the internal name of the type.
				Type frameType = Type.getObjectType((String) frame.local.get(i));
				// Create a copy value but with the new type.
				// This will lose the 'value' content, but we don't care about that since we just want to
				// extract type information.
				VirtualValue newValue =
						VirtualValue.ofVirtual(virtual.getInsns(), simInterpreter.getTypeChecker(), frameType);
				// Updating 'this' frame (the executing frame) ensures things like 'ALOAD list' will properly
				// get the type even of things SimAnalyzer isn't aware of, so long as it is present in the
				// StackMapTable.
				setLocal(i, newValue);
				// Updating the last initialized frame updates the information that will be returned by the
				// SimAnalyzer's "analyze(...)" method
				lastInitted.setLocal(i, newValue);
			}
		}
	}
}
