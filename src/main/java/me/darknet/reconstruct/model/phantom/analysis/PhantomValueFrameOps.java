package me.darknet.reconstruct.model.phantom.analysis;

import me.darknet.assembler.compile.analysis.frame.ValuedFrame;
import me.darknet.assembler.compile.analysis.frame.ValuedFrameOps;
import org.jetbrains.annotations.NotNull;

public class PhantomValueFrameOps extends ValuedFrameOps {

    @Override
    public @NotNull ValuedFrame newEmptyFrame() {
        return new PhantomValueFrame();
    }

}
