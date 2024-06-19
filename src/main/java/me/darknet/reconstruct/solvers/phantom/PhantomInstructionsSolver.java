package me.darknet.reconstruct.solvers.phantom;

import dev.xdark.blw.classfile.AccessFlag;
import dev.xdark.blw.classfile.ClassBuilder;
import dev.xdark.blw.classfile.Method;
import dev.xdark.blw.classfile.MethodBuilder;
import dev.xdark.blw.simulation.SimulationException;
import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.Types;
import dev.xdark.blw.util.Reflectable;
import me.darknet.assembler.compile.analysis.AnalysisException;
import me.darknet.assembler.compile.analysis.Local;
import me.darknet.assembler.compile.analysis.frame.Frame;
import me.darknet.assembler.compile.analysis.frame.TypedFrame;
import me.darknet.assembler.compile.analysis.jvm.AnalysisSimulation;
import me.darknet.assembler.compile.analysis.jvm.JvmAnalysisEngine;
import me.darknet.assembler.compile.analysis.jvm.TypedJvmAnalysisEngine;
import me.darknet.reconstruct.error.Error;
import me.darknet.reconstruct.error.Result;
import me.darknet.reconstruct.model.ClassHierarchy;
import me.darknet.reconstruct.model.phantom.PhantomContainer;
import me.darknet.reconstruct.model.phantom.analysis.PhantomValueFrame;
import me.darknet.reconstruct.model.phantom.analysis.PhantomValuedJvmAnalysisEngine;
import me.darknet.reconstruct.solvers.Solver;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;

public class PhantomInstructionsSolver implements Solver<Result<PhantomContainer>> {
    @Override
    public Result<PhantomContainer> solve(ClassHierarchy hierarchy) {
        List<Error> errors = new ArrayList<>();
        PhantomContainer container = PhantomContainer.create(hierarchy);
        for (ClassBuilder<?, ?> value : hierarchy.primary().values()) { // analyze primary resources
            // first analyze the method bytecode
            for (Reflectable<Method> method : value.getMethods()) {
                MethodBuilder<?, ?> builder = (MethodBuilder<?, ?>) method;

                // analyze types
                PhantomValuedJvmAnalysisEngine engine = new PhantomValuedJvmAnalysisEngine(index -> "");
                AnalysisSimulation simulation = new AnalysisSimulation(engine.newFrameOps());

                List<Local> parameters = new ArrayList<>();
                // build parameters
                boolean isStatic = (builder.accessFlags() & AccessFlag.ACC_STATIC) != 0;
                int localIndex = 0;

                // if not static, add 'this' as a parameter
                if (!isStatic) {
                    parameters.add(new Local(localIndex++, "this", value.type()));
                }

                // add the rest of the parameters
                for (ClassType parameterType : builder.type().parameterTypes()) {
                    parameters.add(new Local(localIndex++, "arg" + localIndex, parameterType));
                    if (parameterType == Types.LONG || parameterType == Types.DOUBLE) {
                        localIndex++;
                        parameters.add(null);
                    }
                }

                var code = builder.code().child();
                var elements = code.codeList().child().getElements();

                AnalysisSimulation.Info info = new AnalysisSimulation.Info(hierarchy.inheritanceChecker(),
                        parameters, elements, code.tryCatchBlocks());

                try {
                    simulation.execute((JvmAnalysisEngine<Frame>) (JvmAnalysisEngine<?>) engine, info);
                } catch (AnalysisException e) {
                    throw new RuntimeException(e);
                }

                PhantomSimulation.Info info2 = new PhantomSimulation.Info(
                        (NavigableMap<Integer, PhantomValueFrame>) (NavigableMap<Integer, ?>)engine.frames(), elements);

                PhantomSimulation simulation2 = new PhantomSimulation();
                PhantomExecutor executor = new PhantomExecutor(container);

                try {
                    simulation2.execute(executor, info2);
                } catch (SimulationException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return new Result<>(container, errors);
    }
}
