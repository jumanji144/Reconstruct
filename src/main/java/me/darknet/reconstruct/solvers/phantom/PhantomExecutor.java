package me.darknet.reconstruct.solvers.phantom;

import dev.xdark.blw.classfile.AccessFlag;
import dev.xdark.blw.code.CodeElement;
import dev.xdark.blw.code.Instruction;
import dev.xdark.blw.code.JavaOpcodes;
import dev.xdark.blw.code.Label;
import dev.xdark.blw.code.instruction.*;
import dev.xdark.blw.constant.Constant;
import dev.xdark.blw.constant.OfDynamic;
import dev.xdark.blw.simulation.ExecutionEngine;
import dev.xdark.blw.type.*;
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
    public void execute(VarInstruction instruction) {}

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

        if (instruction.opcode() == JavaOpcodes.INVOKEINTERFACE) {
            owner.access(owner.access() | AccessFlag.ACC_INTERFACE);
        }

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
        Value stack = currentFrame.pop();

        if (!(stack instanceof Value.ObjectValue)) return;

        PhantomUnit stackOwnerClass = container.getOrCreateClass(currentFrame.pop());
        if (stackOwnerClass == owner) return;

        stackOwnerClass.addTypeHint(owner);
    }

    private void inferDynamicArguments(MethodType mt, List<Constant> constants) {
        // now we iterate through the constant arguments
        for (int i = 0; i < constants.size(); i++) {
            if (constants.get(i) instanceof OfDynamic dynamic) {
                ConstantDynamic dynamicConstant = dynamic.value();
                if (dynamicConstant.type() instanceof ObjectType objectType) {
                    PhantomUnit unit = container.getOrCreateClass(objectType);
                    PhantomUnit bsmParameter = container.getOrCreateClass(mt.parameterTypes().get(i));

                    if (unit == bsmParameter) continue;

                    unit.addTypeHint(bsmParameter);
                }

                // condys themselves can also have dynamic constants

                // check if the bsm has a method type
                MethodHandle bsm = dynamicConstant.methodHandle();

                if (bsm.type() instanceof MethodType bsmType) {
                    inferDynamicArguments(bsmType, dynamicConstant.args());
                }
            }
        }
    }

    @Override
    public void execute(InvokeDynamicInstruction instruction) {
        // indys have the same deduction as method instructions,
        // except that if one of the constant arguments is a constant dynamic constant
        // then there is a possibility we can extract some information from it, example:
        // invokedynamic foo ()V { invokestatic, bar.baz, (LMHL;LString;LMethodType;LA;)LCallSite; }
        // and then as arguments
        // { { condy, LB; { bar.biz, (LMHL;LString;LMethodType;)LB; } } }

        // since the result of the condy gets passed in as argument into the BSM then we can infer if both are objects
        // here the condy will evaluate to a type of B and be passed into A, therefore B is a child of A

        // first do normal deduction
        if (instruction.type() instanceof MethodType mt) {
            // we set it to true so skip the `this` deduction
            inferArgumentTypes(null, true, mt);
        }

        // do the special deduction

        MethodHandle bsm = instruction.bootstrapHandle();

        // we assume we are dealing with T_INVOKESTATIC handle
        inferDynamicArguments((MethodType) bsm.type(), instruction.args());
    }

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
