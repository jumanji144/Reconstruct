package me.darknet.reconstruct.model.phantom;

import dev.xdark.blw.classfile.ClassBuilder;
import dev.xdark.blw.classfile.Field;
import dev.xdark.blw.classfile.Method;
import dev.xdark.blw.classfile.generic.GenericClassBuilder;
import dev.xdark.blw.type.ArrayType;
import dev.xdark.blw.type.ClassType;
import dev.xdark.blw.type.InstanceType;
import dev.xdark.blw.type.ObjectType;
import dev.xdark.blw.util.Reflectable;
import me.darknet.assembler.compile.analysis.Value;
import me.darknet.reconstruct.model.ClassHierarchy;
import me.darknet.reconstruct.model.phantom.analysis.PhantomObjectValue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PhantomContainer {

    private final Map<String, PhantomClass> classes = new HashMap<>();

    public PhantomClass addClass(InstanceType type) {
        ClassBuilder<?, ?> builder = new GenericClassBuilder();
        builder.type(type);

        return new PhantomClass(builder);
    }

    public PhantomClass getOrCreateClass(InstanceType type) {
        return classes.computeIfAbsent(type.internalName(), name -> addClass(type));
    }

    public PhantomUnit getOrCreateClass(Value value) {
        if (value instanceof PhantomObjectValue phantomObjectValue) {
            return getOrCreateClass(phantomObjectValue);
        }
        return getOrCreateClass(value.type());
    }

    public PhantomUnit getOrCreateClass(PhantomObjectValue value) {
        Set<PhantomUnit> classes = new HashSet<>(value.possible().size());
        for (Value possible : value.possible()) {
            classes.add(getOrCreateClass(possible));
        }

        return new MultiPhantomUnit(classes, (InstanceType) value.type());
    }

    public PhantomClass getOrCreateClass(ObjectType type) {
        if (type instanceof ArrayType arrayType) {
            // go down to the base type
            while (arrayType.componentType() instanceof ArrayType) {
                arrayType = (ArrayType) arrayType.componentType();
            }
            return getOrCreateClass(arrayType.componentType());
        }
        return getOrCreateClass((InstanceType) type);
    }

    public PhantomClass getOrCreateClass(ClassType type) {
        if (type instanceof ObjectType) {
            return getOrCreateClass((ObjectType) type);
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    public static PhantomContainer create(ClassHierarchy hierarchy) {
        PhantomContainer container = new PhantomContainer();
        hierarchy.builders().forEach((name, builder) -> {
            PhantomClass phantom = new PhantomClass(builder);
            phantom.concrete(true);
            container.classes.put(name, phantom);
        });
        return container;
    }

    public ClassHierarchy build() {
        Map<String, ClassBuilder<?, ?>> builders = new HashMap<>();
        for (Map.Entry<String, PhantomClass> entry : classes.entrySet()) {
            builders.put(entry.getKey(), entry.getValue().builder());
        }
        return new ClassHierarchy(builders);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (var entry : classes.entrySet()) {
            result.append(entry.getKey()).append(" (")
                    .append(entry.getValue().concrete() ? "concrete)" : "phantom):").append("\n");
            if (entry.getValue().concrete()) continue;
            var builder = entry.getValue().builder();
            for (Reflectable<Method> methodReflectable : builder.getMethods()) {
                Method method = methodReflectable.reflectAs();
                result.append("- ").append(method.name()).append(method.type()).append("\n");
            }
            for (Reflectable<Field> field : builder.getFields()) {
                Field f = field.reflectAs();
                result.append("- ").append(f.name()).append(" ").append(f.type()).append("\n");
            }
            for (PhantomUnit parentCandidate : entry.getValue().parentCandidates()) {
                if (parentCandidate instanceof PhantomClass phantomClass) {
                    result.append("- ").append("Parent: ")
                            .append(phantomClass.builder().type().internalName()).append("\n");
                }
            }
            for (PhantomUnit childrenCandidate : entry.getValue().childrenCandidates()) {
                if (childrenCandidate instanceof PhantomClass phantomClass) {
                    result.append("- ").append("Child: ")
                            .append(phantomClass.builder().type().internalName()).append("\n");
                }
            }
        }
        return result.toString();
    }
}
