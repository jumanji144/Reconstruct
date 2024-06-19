package me.darknet.reconstruct.model.phantom.analysis;

import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.InstanceType;
import dev.xdark.blw.type.ObjectType;
import dev.xdark.blw.type.Types;
import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.ValueMergeException;
import me.darknet.assembler.compiler.InheritanceChecker;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static me.darknet.assembler.compile.analysis.Value.ObjectValue;

public class PhantomObjectValue implements ObjectValue {

    private final Set<ObjectValue> possible;
    private ObjectType commonType;

    public PhantomObjectValue(Set<ObjectValue> possible) {
        this(possible, Types.OBJECT);
    }

    public PhantomObjectValue(ObjectValue... possible) {
        this(new HashSet<>(Arrays.asList(possible)));
    }

    public PhantomObjectValue(Set<ObjectValue> possible, ObjectType commonType) {
        this.possible = possible;
        this.commonType = commonType;
    }

    public PhantomObjectValue() {
        this(new HashSet<>());
    }

    @Override
    public @NotNull ObjectType type() {
        // the type of this is the common type of all the values
        return commonType;
    }

    public Set<ObjectValue> possible() {
        return possible;
    }

    @Override
    public @NotNull Value mergeWith(@NotNull InheritanceChecker checker, @NotNull Value other) throws ValueMergeException {
        if (equals(other) || other instanceof NullValue)
            return this;

        if (other instanceof PhantomObjectValue phantom) {
            // compute the common of the two superpositions
            String superCommon = checker.getCommonSuperclass(
                    type().internalName(), phantom.type().internalName());
            this.commonType = Types.instanceTypeFromInternalName(superCommon);

            // merge the two sets
            this.possible.addAll(phantom.possible);

            return this;
        }

        if (other instanceof ObjectValue obj) {
            // compute common
            String commonSuperclass = checker
                    .getCommonSuperclass(type().internalName(), obj.type().internalName());
            this.commonType = Types.instanceTypeFromInternalName(commonSuperclass);

            // merge the two sets
            possible.add(obj);

            return this;
        }

        throw new ValueMergeException("Invalid merge of object and non-object value");
    }

    @Override
    public int hashCode() {
        return possible.hashCode() * 31 + commonType.hashCode();
    }

    @Override
    public String toString() {
        return "PhantomObjectValue{" +
                "possible=" + possible +
                ", commonType=" + commonType +
                '}';
    }
}
