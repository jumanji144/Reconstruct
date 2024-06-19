package me.darknet.reconstruct.model.phantom.analysis;

import me.darknet.assembler.compile.analysis.VariableNameLookup;
import me.darknet.assembler.compile.analysis.frame.FrameOps;
import me.darknet.assembler.compile.analysis.jvm.ValuedJvmAnalysisEngine;
import org.jetbrains.annotations.NotNull;

public class PhantomValuedJvmAnalysisEngine extends ValuedJvmAnalysisEngine {

    public PhantomValuedJvmAnalysisEngine(@NotNull VariableNameLookup variableNameLookup) {
        super(variableNameLookup);
    }

    @Override
    public FrameOps<?> newFrameOps() {
        return new PhantomValueFrameOps();
    }

}
