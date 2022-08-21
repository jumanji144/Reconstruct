package me.darknet.resconstruct.analysis;

import me.coley.analysis.SimAnalyzer;
import me.coley.analysis.SimFrame;
import me.coley.analysis.SimInterpreter;
import me.coley.analysis.value.AbstractValue;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.NavigableMap;

public class ExtendedSimAnalyser extends SimAnalyzer {

	private final NavigableMap<Integer, FrameNode> stackFrames;

	/**
	 * Create analyzer.
	 *
	 * @param interpreter Interpreter to use.
	 */
	public ExtendedSimAnalyser(NavigableMap<Integer, FrameNode> stackFrames, SimInterpreter interpreter) {
		super(interpreter);
		this.stackFrames = stackFrames;
	}

	@Override
	protected SimFrame newFrame(Frame<? extends AbstractValue> frame) {
		return new ExtendedSimFrame(stackFrames, (SimFrame) frame);
	}

	@Override
	protected SimFrame newFrame(int numLocals, int numStack) {
		return new ExtendedSimFrame(stackFrames, numLocals, numStack);
	}
}
