package me.darknet.reconstruct.solvers.phantom;

import dev.xdark.blw.code.CodeElement;
import dev.xdark.blw.code.Instruction;
import dev.xdark.blw.simulation.ExecutionEngines;
import dev.xdark.blw.simulation.Simulation;
import dev.xdark.blw.simulation.SimulationException;
import me.darknet.assembler.compile.analysis.frame.TypedFrame;
import me.darknet.reconstruct.model.phantom.analysis.PhantomValueFrame;

import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

public class PhantomSimulation implements Simulation<PhantomExecutor, PhantomSimulation.Info> {
    @Override
    public void execute(PhantomExecutor engine, Info method) throws SimulationException {
        for (int i = 0; i < method.elements.size(); i++) {
            CodeElement element = method.elements.get(i);
            var currentEntry = method.frames.floorEntry(i);
            var nextEntry = method.frames.higherEntry(i);
            if (currentEntry == null)
                continue;

            PhantomValueFrame currentFrame = currentEntry.getValue();
            PhantomValueFrame nextFrame = nextEntry == null ? null : nextEntry.getValue();

            engine.setInfo(i, currentFrame, nextFrame);

            if (element instanceof Instruction instruction)
                ExecutionEngines.execute(engine, instruction);
        }
    }

    public record Info(NavigableMap<Integer, PhantomValueFrame> frames, List<CodeElement> elements) {}
}
