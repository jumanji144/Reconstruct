package me.darknet.reconstruct.solvers;

import me.darknet.reconstruct.model.ClassHierarchy;

public interface Solver<T> {

    T solve(ClassHierarchy hierarchy);

}
