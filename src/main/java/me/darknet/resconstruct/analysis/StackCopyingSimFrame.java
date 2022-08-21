package me.darknet.resconstruct.analysis;

import me.coley.analysis.SimFrame;
import me.coley.analysis.SimInterpreter;
import me.coley.analysis.TypeChecker;
import me.coley.analysis.TypeResolver;
import me.coley.analysis.util.InsnUtil;
import me.coley.analysis.util.TypeUtil;
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
		// There are not stack-frames per each instruction, but per "blocks" split up by labels.
		// The stack-frame entry is at the beginning of each block. So for any instruction in the block,
		// we can get the associated frame by doing a floor lookup of the current instruction index.
		int insnIndex = InsnUtil.index(insn);
		Map.Entry<Integer, FrameNode> entry = stackFrames.floorEntry(insnIndex);
		if (entry == null) {
			// It is possible that a frame does not yet exist, likely with code that doesn't have any need
			// to be enriched anyways, so it is ok to stop here.
			super.execute(insn, interpreter);
			return;
		}
		// From here on, we have stack-map information.
		int frameIndex = entry.getKey();
		// Sanity check frame type, we use 'ClassReader.EXPAND_FRAMES' so the frames should always
		// be in expanded form, which effectively makes them operate like a FULL frame, even if the
		// frame type in the bytecode is something like APPEND.
		// This makes look-ups like 'locals.get(i)' simple since they "just work".
		FrameNode frame = entry.getValue();
		if (frame.type != Opcodes.F_NEW)
			throw new AnalyzerException(insn, "Invalid stack map frame, expected: F_NEW");
		SimInterpreter simInterpreter = (SimInterpreter) interpreter;
		TypeResolver typeResolver = simInterpreter.getTypeResolver();
		TypeChecker typeChecker = simInterpreter.getTypeChecker();
		// Update stack information. We can only do this for the first executed instruction in a block.
		// The frame entry in the StackMapTable only tells us about the stack's state in the beginning.
		// Though, the interpreter should propagate information we provide here.
		if (insnIndex - 1 == frameIndex) {
			// We can only work up to the maximum size defined by the stack entry.
			int stackMax = Math.min(getMaxStackSize(), frame.stack.size());
			for (int i = 0; i < stackMax; i++) {
				AbstractValue value = getStack(i);
				// We only care about object/virtual values
				if (value instanceof VirtualValue) {
					// Get the type in the stack-frame at the current index.
					// The 'local' value should be the internal name of the type.
					Type frameType = Type.getObjectType((String) frame.stack.get(i));
					// Get the type value as recognized by SimAnalyzer for the stack value.
					VirtualValue virtual = (VirtualValue) value;
					Type currentType = virtual.getType();
					// Compute which type is the 'best' and use that.
					// If it is the same as the current type, we do not need to do anything.
					Type targetType = computeBestType(currentType, frameType, typeResolver);
					if (targetType.equals(currentType))
						return;
					// Create a copy value but with the new type.
					VirtualValue newValue =
							VirtualValue.ofVirtual(virtual.getInsns(), typeChecker, targetType, virtual.getValue());
					// Updating 'this' frame will update what the interpreter operates off of since this is the frame
					// the interpreter is using as its 'executing frame'.
					setStack(i, newValue);
					// Updating the last initialized frame updates the information that will be returned by the
					// SimAnalyzer's "analyze(...)" method.
					lastInitted.setStack(i, newValue);
				}
			}
		}
		// Now that we have updated the type information of things on the stack, we can continue execution
		// using the updated values.
		super.execute(insn, interpreter);
		// Update local variable information
		for (int i = 0; i < frame.local.size(); i++) {
			AbstractValue value = getLocal(i);
			// We only care about object/virtual values
			if (value instanceof VirtualValue) {
				// Get the type in the stack-frame at the current index.
				// The 'local' value should be the internal name of the type.
				Type frameType = Type.getObjectType((String) frame.local.get(i));
				// Get the type value as recognized by SimAnalyzer for the local variable.
				VirtualValue virtual = (VirtualValue) value;
				Type currentType = virtual.getType();
				// Compute which type is the 'best' and use that.
				// If it is the same as the current type, we do not need to do anything.
				Type targetType = computeBestType(currentType, frameType, typeResolver);
				if (targetType.equals(currentType))
					return;
				// Create a copy value but with the new type.
				VirtualValue newValue =
						VirtualValue.ofVirtual(virtual.getInsns(), typeChecker, targetType, virtual.getValue());
				// Updating 'this' frame (the executing frame) ensures things like 'ALOAD list' will properly
				// get the type even of things SimAnalyzer isn't aware of, so long as it is present in the
				// StackMapTable.
				setLocal(i, newValue);
				// Updating the last initialized frame updates the information that will be returned by the
				// SimAnalyzer's "analyze(...)" method.
				lastInitted.setLocal(i, newValue);
			}
		}
	}

	private static Type computeBestType(Type currentType, Type frameType, TypeResolver typeResolver) {
		Type commonType = (currentType.equals(frameType)) ? currentType :
				typeResolver.common(currentType, frameType);
		if (TypeUtil.OBJECT_TYPE.equals(commonType)) {
			// One of the types involved is not known to SimAnalyzer.
			// In this case we will trust the StackMapTable entry.
			return frameType;
		} else if (currentType.equals(commonType)) {
			// The current type is the common type, no decision needed.
			// Both are the same.
			return frameType;
		} else {
			// The common type is NOT the current type.
			// But it is also not "Object" so SimAnalyzer is aware of both involved types.
			// The "currentType" is likely more specific than "frameType" so we will use it.
			return currentType;
		}
	}
}
