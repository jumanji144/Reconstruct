package me.darknet.reconstruct;

import me.darknet.assembler.compile.analysis.frame.ValuedFrameImpl;
import me.darknet.reconstruct.error.Result;
import me.darknet.reconstruct.model.ClassHierarchy;
import me.darknet.reconstruct.solvers.phantom.PhantomSolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class Reconstruct {

    public static void main(String[] args) throws IOException {
        byte[] data = Files.readAllBytes(Paths.get("Application.classx"));
        byte[] objectData = Files.readAllBytes(Paths.get("Object.classx"));
        // load java/lang/Object.class from the classpath
        ClassHierarchy hierarchy = ClassHierarchy.of(Map.of("simulation/Application", data), Map.of(
                "java/lang/Object", objectData
        ));

        // do something with hierarchy
        PhantomSolver solver = new PhantomSolver();
        Result<ClassHierarchy> result = solver.solve(hierarchy);

        return;
    }

}
