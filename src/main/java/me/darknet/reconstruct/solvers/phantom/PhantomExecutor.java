package me.darknet.reconstruct.solvers.phantom;

import dev.xdark.blw.classfile.AccessFlag;
import dev.xdark.blw.code.CodeElement;
import dev.xdark.blw.code.Instruction;
import dev.xdark.blw.code.JavaOpcodes;
import dev.xdark.blw.code.Label;
import dev.xdark.blw.code.instruction.*;
import dev.xdark.blw.simulation.ExecutionEngine;
import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.InstanceType;
import dev.xdark.blw.type.MethodType;
import dev.xdark.blw.type.ObjectType;
import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.frame.Frame;
import me.darknet.assembler.compile.analysis.frame.TypedFrame;
import me.darknet.reconstruct.model.phantom.PhantomClass;
import me.darknet.reconstruct.model.phantom.PhantomContainer;
import me.darknet.reconstruct.model.phantom.PhantomUnit;
import me.darknet.reconstruct.model.phantom.analysis.PhantomObjectValue;
import me.darknet.reconstruct.model.phantom.analysis.PhantomValueFrame;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PhantomExecutor implements ExecutionEngine {

    private final PhantomContainer container;
    private int currentIndex = 0;
    private PhantomValueFrame currentFrame;
    private PhantomValueFrame nextFrame;

    public PhantomExecutor(PhantomContainer container) {
        this.container = container;
    }

    public void setInfo(int currentIndex, PhantomValueFrame currentFrame, PhantomValueFrame nextFrame) {
        this.currentIndex = currentIndex;
        this.currentFrame = currentFrame;
        this.nextFrame = nextFrame;
    }

    @Override
    public void label(Label label) {}

    @Override
    public void execute(SimpleInstruction instruction) {}

    @Override
    public void execute(ConstantInstruction<?> instruction) {}

    @Override
    public void execute(VarInstruction instruction) {

    }

    @Override
    public void execute(LookupSwitchInstruction instruction) {}

    @Override
    public void execute(TableSwitchInstruction instruction) {}

    @Override
    public void execute(InstanceofInstruction instruction) {
        PhantomUnit in = container.getOrCreateClass(currentFrame.pop());
        PhantomUnit out = container.getOrCreateClass(instruction.type());

        if (in == out) return;

        in.addTypeHint(out); // in can become out, so out is a parent of in
    }

    @Override
    public void execute(CheckCastInstruction instruction) {
        PhantomUnit in = container.getOrCreateClass(currentFrame.pop());
        PhantomUnit out = container.getOrCreateClass(instruction.type());

        if (in == out) return;

        in.addTypeHint(out); // in can become out, so out is a parent of in
    }

    @Override
    public void execute(AllocateInstruction instruction) {}

    @Override
    public void execute(AllocateMultiDimArrayInstruction allocateMultiDimArrayInstruction) {
        PhantomClass arrayClass = container.getOrCreateClass(allocateMultiDimArrayInstruction.type());

        for (int i = 0; i < allocateMultiDimArrayInstruction.dimensions(); i++) {
            PhantomUnit stack = container.getOrCreateClass(currentFrame.pop());
            if (stack == arrayClass) continue;

            stack.addTypeHint(arrayClass);
        }
    }

    private void inferArgumentTypes(PhantomClass owner, boolean isStatic, MethodType type) {

        for (ClassType parameterType : type.parameterTypes()) {
            Value stack = currentFrame.pop(parameterType);

            if (!(parameterType instanceof ObjectType)) continue;

            PhantomUnit stackClass = container.getOrCreateClass(stack);
            PhantomUnit parameterClass = container.getOrCreateClass((ObjectType) parameterType);

            if (stackClass == parameterClass) continue;

            stackClass.addTypeHint(parameterClass);

        }

        if(!isStatic) {
            PhantomUnit stackThisClass = container.getOrCreateClass(currentFrame.pop());

            if (stackThisClass == owner) return; // nothing to infer

            stackThisClass.addTypeHint(owner);
        }

    }

    @Override
    public void execute(MethodInstruction instruction) {
        PhantomClass owner = container.getOrCreateClass(instruction.owner());

        boolean isStatic = instruction.opcode() == JavaOpcodes.INVOKESTATIC;

        inferArgumentTypes(owner, isStatic, instruction.type());

        if(!owner.concrete()) {
            int access = AccessFlag.ACC_PUBLIC | (isStatic ? AccessFlag.ACC_STATIC : 0);

            owner.putMethod(access, instruction.name(), instruction.type());
        }
    }

    @Override
    public void execute(FieldInstruction instruction) {
        PhantomUnit owner = container.getOrCreateClass(instruction.owner());

        boolean isStatic = instruction.opcode() == JavaOpcodes.GETSTATIC || instruction.opcode() == JavaOpcodes.PUTSTATIC;

        if(!owner.concrete()) {
            int access = AccessFlag.ACC_PUBLIC | (isStatic ? AccessFlag.ACC_STATIC : 0);

            owner.putField(access, instruction.name(), instruction.type());
        }

        if(isStatic) return;

        PhantomUnit stackOwnerClass = container.getOrCreateClass(currentFrame.pop());
        if (stackOwnerClass == owner) return;

        stackOwnerClass.addTypeHint(owner);
    }

    @Override
    public void execute(InvokeDynamicInstruction instruction) {}

    @Override
    public void execute(ImmediateJumpInstruction instruction) {}

    @Override
    public void execute(ConditionalJumpInstruction instruction) {}

    @Override
    public void execute(VariableIncrementInstruction instruction) {}

    @Override
    public void execute(PrimitiveConversionInstruction instruction) {}

    @Override
    public void execute(Instruction instruction) {}
}
