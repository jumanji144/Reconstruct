package me.darknet.reconstruct.model.phantom.analysis;

import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.ValueMergeException;
import me.darknet.assembler.compile.analysis.ValuedLocal;
import me.darknet.assembler.compile.analysis.Values;
import me.darknet.assembler.compile.analysis.frame.FrameMergeException;
import me.darknet.assembler.compile.analysis.frame.ValuedFrame;
import me.darknet.assembler.compile.analysis.frame.ValuedFrameImpl;
import me.darknet.assembler.compiler.InheritanceChecker;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PhantomValueFrame extends ValuedFrameImpl {

    @Override
    public @NotNull ValuedFrameImpl copy() {
        PhantomValueFrame copy = new PhantomValueFrame();
        copy.getLocals().putAll(getLocals());
        copy.getStack().addAll(getStack());
        return copy;
    }

    private Value maybePhantomMerge(InheritanceChecker checker, Value a, Value b) throws ValueMergeException {
        if (a.type() == null && b.type() != null) {
            return b;
        } else if (a.type() != null && b.type() == null) {
            return a;
        }

        // check if neither are phantom but object
        if (a.equals(b) || b instanceof Value.NullValue) {
            return a;
        }

        if (a instanceof Value.ObjectValue objA && b instanceof Value.ObjectValue objB) {
            PhantomObjectValue phantom;
            if (a instanceof PhantomObjectValue p) {
                phantom = p;
            } else if (b instanceof PhantomObjectValue p) {
                phantom = p;
            } else {
                phantom = new PhantomObjectValue(objA);
            }
            return phantom.mergeWith(checker, objB);
        }

        return a.mergeWith(checker, b);
    }

    @Override
    public boolean merge(@NotNull InheritanceChecker checker, @NotNull ValuedFrame other) throws FrameMergeException {
        boolean changed = false;
        for (Map.Entry<Integer, ValuedLocal> entry : other.getLocals().entrySet()) {
            int index = entry.getKey();
            ValuedLocal local = getLocal(index);
            ValuedLocal otherLocal = entry.getValue();

            // If we don't have the local, copy it from the other frame.
            if (local == null) {
                // We don't set 'changed' since expanding local variable scope is not going to change
                // behavior of frames that previously passed analysis.
                setLocal(index, otherLocal);
                continue;
            }

            try {
                Value localVal = local.value();
                Value otherVal = otherLocal.value();

                Value merged = maybePhantomMerge(checker, localVal, otherVal);

                ValuedLocal mergedLocal = new ValuedLocal(local.index(), local.name(), merged);

                if (!Objects.equals(local, mergedLocal)) {
                    setLocal(index, mergedLocal);
                    changed = true;
                }
            } catch (Exception e) {
                throw new FrameMergeException(this, other, e.getMessage());
            }
        }

        Deque<Value> otherStack = other.getStack();
        Deque<Value> thisStack = getStack();

        if (thisStack.size() != otherStack.size()) {
            throw new FrameMergeException(this, other, "Stack size mismatch, " + thisStack.size() + " != " + otherStack.size());
        }

        Deque<Value> newStack = new ArrayDeque<>();

        // zip the two stacks together
        Iterator<Value> it1 = thisStack.iterator();
        Iterator<Value> it2 = otherStack.iterator();

        while (it1.hasNext() && it2.hasNext()) {
            Value value1 = it1.next();
            Value value2 = it2.next();
            if (value1 == value2) {
                newStack.add(value1);
                continue;
            } else if (value1 == Values.VOID_VALUE || value2 == Values.VOID_VALUE) {
                newStack.add(Values.VOID_VALUE);
                continue;
            }
            Value merged;
            try {
                merged = maybePhantomMerge(checker, value1, value2);
            } catch (ValueMergeException ex) {
                throw new FrameMergeException(this, other, ex.getMessage());
            }
            if (!Objects.equals(merged, value1)) {
                changed = true;
                it1.remove();
                newStack.add(merged);
            } else {
                newStack.add(value1);
            }
        }

        thisStack.clear();
        thisStack.addAll(newStack);

        return changed;
    }

}
