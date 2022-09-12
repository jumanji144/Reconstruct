package me.darknet.resconstruct.analysis;

import me.coley.analysis.SimFrame;
import me.coley.analysis.SimInterpreter;
import me.coley.analysis.TypeChecker;
import me.coley.analysis.TypeResolver;
import me.coley.analysis.util.InsnUtil;
import me.coley.analysis.util.TypeUtil;
import me.coley.analysis.value.AbstractValue;
import me.coley.analysis.value.VirtualValue;
import me.darknet.resconstruct.GenerateException;
import me.darknet.resconstruct.util.TypeUtils;
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
	private static final Type TOP_TYPE = Type.VOID_TYPE;
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
					Type targetType = TypeUtils.computeBestType(currentType, frameType, typeResolver);
					if (targetType.equals(currentType))
						continue;
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
		int wideCount = 0;
		for (int i = 0; i < frame.local.size(); i++) {
			// Sanity check
			if (wideCount < 0)
				throw new AnalyzerException(frame, "Frame's StackMapTable yielded data causing estimated" +
						" wide conversion offset to go negative");
			// The index in the "locals" of an ASM stack analysis frame must account for wide types.
			// So we will adjust for it here. We do not need to do so for stack-frames since they
			// do not account for wide types and their reserved slots.
			AbstractValue value = getLocal(i + wideCount);
			// Get the type in the stack-frame at the current index.
			// The 'local' value should be the internal name of the type.
			Type frameLocalType = getFrameLocalType(frame, i);
			// We only care about object/virtual values for type solving
			if (value instanceof VirtualValue) {
				// Get the type value as recognized by SimAnalyzer for the local variable.
				VirtualValue virtual = (VirtualValue) value;
				Type currentType = virtual.getType();
				// Compute which type is the 'best' and use that.
				// If it is the same as the current type, we do not need to do anything.
				Type targetType = TypeUtils.computeBestType(currentType, frameLocalType, typeResolver);
				if (targetType.equals(currentType))
					continue;
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
			} else if (value.isWide()) {
				// Need to offset wide count differences between ASM stack analysis frame,
				// and the StackMapTable frame.
				wideCount++;
				// If the StackMapTable's frame is 'top' then we don't need to offset.
				// I don't understand why, but sometimes this data exists, sometimes it does not.
				// The spec says:
				//  >> Types of size 2 (long and double) are represented by two local variables (§2.6.1),
				//  >> with the first local variable being the type itself and the second
				//  >> local variable being top (§4.10.1.7).
				// However, it is trivial to generate a class with a StackMapTable where in one frame
				// this is satisfied, in another 'top' is missing, and in others there are multiple 'top' values
				// in a row.
				if (frameLocalType == TOP_TYPE)
					wideCount--;
			}
		}
	}

	private static Type getFrameLocalType(FrameNode frame, int index) {
		Object frameLocal = frame.local.get(index);
		Type frameLocalType;
		if (frameLocal instanceof Integer) {
			// Frame constants are not public, refer to ASM's frame class.
			int valueKey = (Integer) frameLocal;
			switch (valueKey) {
				case 0: // ITEM_TOP
					frameLocalType = TOP_TYPE;
					break;
				case 1: // ITEM_INTEGER
					frameLocalType = Type.INT_TYPE;
					break;
				case 2: // ITEM_FLOAT
					frameLocalType = Type.FLOAT_TYPE;
					break;
				case 3: // ITEM_DOUBLE
					frameLocalType = Type.DOUBLE_TYPE;
					break;
				case 4: // ITEM_LONG
					frameLocalType = Type.LONG_TYPE;
					break;
				case 5: // ITEM_NULL
				case 7: // ITEM_OBJECT
				case 8: // ITEM_UNINITIALIZED
					frameLocalType = TypeUtil.OBJECT_TYPE;
					break;
				case 6: // ITEM_UNINITIALIZED_THIS
					// TODO: Use declaring type of method instead
					frameLocalType = TypeUtil.OBJECT_TYPE;
					break;
				case 9: // ITEM_ASM_BOOLEAN
					frameLocalType = Type.BOOLEAN_TYPE;
					break;
				case 10: // ITEM_ASM_BYTE
					frameLocalType = Type.BYTE_TYPE;
					break;
				case 11: // ITEM_ASM_CHAR
					frameLocalType = Type.CHAR_TYPE;
					break;
				case 12: // ITEM_ASM_SHORT
					frameLocalType = Type.SHORT_TYPE;
					break;
				default:
					throw new GenerateException("Unknown frame type: " + valueKey);
			}
		} else {
			frameLocalType = Type.getObjectType((String) frameLocal);
		}
		return frameLocalType;
	}
}
