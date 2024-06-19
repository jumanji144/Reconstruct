package me.darknet.reconstruct.model.phantom;

import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.InstanceType;
import dev.xdark.blw.type.MethodType;

import java.util.HashSet;
import java.util.Set;

public class MultiPhantomUnit implements PhantomUnit {

    private final Set<PhantomUnit> classes;
    private final InstanceType common;

    private final Set<PhantomUnit> parentCandidates;
    private final Set<PhantomUnit> childrenCandidates;

    public MultiPhantomUnit(Set<PhantomUnit> classes, InstanceType common) {
        this.classes = classes;
        this.common = common;

        parentCandidates = new HashSet<>();
        childrenCandidates = new HashSet<>();
        for (PhantomUnit phantom : classes) {
            parentCandidates.addAll(phantom.parentCandidates());
            childrenCandidates.addAll(phantom.childrenCandidates());
        }
    }

    @Override
    public void addTypeHint(PhantomUnit parent) {
        for (PhantomUnit phantom : classes) {
            phantom.addTypeHint(parent);
        }
        parentCandidates.add(parent);
    }

    @Override
    public InstanceType type() {
        return common;
    }

    @Override
    public Set<PhantomUnit> parentCandidates() {
        return parentCandidates;
    }

    @Override
    public Set<PhantomUnit> childrenCandidates() {
        return childrenCandidates;
    }

    @Override
    public void putMethod(int access, String name, MethodType type) {
        for (PhantomUnit phantom : classes) {
            phantom.putMethod(access, name, type);
        }
    }

    @Override
    public void putField(int access, String name, ClassType type) {
        for (PhantomUnit phantom : classes) {
            phantom.putField(access, name, type);
        }
    }

    @Override
    public boolean concrete() {
        for (PhantomUnit phantom : classes) {
            if (!phantom.concrete()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int access() {
        // return access flags that all classes share
        int access = 0xFFFFFFFF;
        for (PhantomUnit phantom : classes) {
            access &= phantom.access();
        }
        return access;
    }

    @Override
    public void access(int access) {
        for (PhantomUnit phantom : classes) {
            phantom.access(access);
        }
    }
}
