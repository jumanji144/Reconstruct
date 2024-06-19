package me.darknet.reconstruct.solvers.phantom;

import me.darknet.reconstruct.error.Error;
import me.darknet.reconstruct.error.Result;
import me.darknet.reconstruct.model.ClassHierarchy;
import me.darknet.reconstruct.model.phantom.PhantomContainer;
import me.darknet.reconstruct.solvers.Solver;

import java.util.ArrayList;
import java.util.List;

public class PhantomSolver implements Solver<Result<ClassHierarchy>> {

    @Override
    public Result<ClassHierarchy> solve(ClassHierarchy hierarchy) {
        List<Error> errors = new ArrayList<>();
        Result<PhantomContainer> phantomResult = new PhantomInstructionsSolver().solve(hierarchy);

        if (phantomResult.hasErr())
            errors.addAll(phantomResult.errors());

        // translate phantom classes to real classes
        hierarchy = phantomResult.get().build();
        return new Result<>(hierarchy, errors);
    }

}
