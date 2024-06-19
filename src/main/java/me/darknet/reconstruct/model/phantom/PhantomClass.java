package me.darknet.reconstruct.model.phantom;

import dev.xdark.blw.classfile.AccessFlag;
import dev.xdark.blw.classfile.ClassBuilder;
import dev.xdark.blw.classfile.MemberIdentifier;
import dev.xdark.blw.code.JavaOpcodes;
import dev.xdark.blw.code.instruction.AllocateInstruction;
import dev.xdark.blw.code.instruction.ConstantInstruction;
import dev.xdark.blw.code.instruction.MethodInstruction;
import dev.xdark.blw.code.instruction.SimpleInstruction;
import dev.xdark.blw.constant.OfString;
import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.InstanceType;
import dev.xdark.blw.type.MethodType;
import dev.xdark.blw.type.Types;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PhantomClass implements PhantomUnit {

    private final ClassBuilder<?, ?> builder;
    private boolean concrete;

    private Set<PhantomUnit> parentCandidates = new HashSet<>();
    private Set<PhantomUnit> childrenCandidates = new HashSet<>();
    private Set<PhantomUnit> concreteParents = new HashSet<>();
    private Set<PhantomUnit> concreteChildren = new HashSet<>();

    public PhantomClass(ClassBuilder<?, ?> builder) {
        this.builder = builder;
    }

    public void concrete(boolean concrete) {
        this.concrete = concrete;
    }

    public boolean concrete() {
        return concrete;
    }

    @Override
    public int access() {
        return builder.accessFlags();
    }

    @Override
    public void access(int access) {
        builder.accessFlags(access);
    }

    public InstanceType type() {
        return builder.type();
    }

    public Set<PhantomUnit> parentCandidates() {
        return parentCandidates;
    }

    public Set<PhantomUnit> childrenCandidates() {
        return childrenCandidates;
    }

    public ClassBuilder<?, ?> builder() {
        return builder;
    }

    @Override
    public void addTypeHint(PhantomUnit parent) {
        if (concrete) {
            return; // no need to add type hints to concrete classes
        }
        this.parentCandidates.add(parent);
        parent.childrenCandidates().add(this);
    }

    @Override
    public void putMethod(int access, String name, MethodType type) {
        var method = builder.putMethod(access, name, type).child();

        if((access & AccessFlag.ACC_ABSTRACT) != 0 || (access & AccessFlag.ACC_INTERFACE) != 0) {
            return;
        }

        var code = method.code().child();
        var list = code.codeList().child();

        list.addInstruction(new AllocateInstruction(Types.instanceType(UnsupportedOperationException.class)))
            .addInstruction(new SimpleInstruction(JavaOpcodes.DUP))
            .addInstruction(new ConstantInstruction.String(new OfString("Not implemented")))
            .addInstruction(new MethodInstruction(JavaOpcodes.INVOKESPECIAL,
                   Types.instanceType(UnsupportedOperationException.class), "<init>",
                   Types.methodType(Types.VOID, Types.STRING), false))
            .addInstruction(new SimpleInstruction(JavaOpcodes.ATHROW));

        code.maxLocals(Types.parametersSize(type));
        code.maxStack(2);
    }

    @Override
    public void putField(int access, String name, ClassType type) {
        builder.putField(access, name, type);
    }

    @Override
    public String toString() {
        return builder.type().internalName();
    }
}
