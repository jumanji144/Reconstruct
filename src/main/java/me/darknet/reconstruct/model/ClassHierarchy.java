package me.darknet.reconstruct.model;

import dev.xdark.blw.BytecodeLibrary;
import dev.xdark.blw.asm.AsmBytecodeLibrary;
import dev.xdark.blw.asm.ClassWriterProvider;
import dev.xdark.blw.classfile.AccessFlag;
import dev.xdark.blw.classfile.ClassBuilder;
import dev.xdark.blw.classfile.ClassFileView;
import dev.xdark.blw.classfile.generic.GenericClassBuilder;
import dev.xdark.blw.type.InstanceType;
import me.darknet.assembler.compile.JvmClassWriter;
import me.darknet.assembler.compiler.InheritanceChecker;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ClassHierarchy {

    private final BytecodeLibrary library = new AsmBytecodeLibrary(new ClassHierarchyClassWriterProvider());
    private final ClassHierarchyInheritanceChecker checker = new ClassHierarchyInheritanceChecker();
    private final Map<String, ClassBuilder<?, ?>> classes;
    private final Map<String, ClassBuilder<?, ?>> primary;
    private final Map<String, ClassBuilder<?, ?>> secondary;

    public ClassHierarchy(Map<String, ClassBuilder<?, ?>> classes) {
        this.classes = classes;
        this.primary = classes;
        this.secondary = new HashMap<>();
    }

    public ClassHierarchy(Map<String, ClassBuilder<?, ?>> primary, Map<String, ClassBuilder<?, ?>> secondary) {
        // classes should be merge of primary and secondary
        this.classes = new HashMap<>();
        this.classes.putAll(primary);
        this.classes.putAll(secondary);

        this.primary = primary;
        this.secondary = secondary;
    }

    public ClassHierarchy merge(ClassHierarchy other) {
        return new ClassHierarchy(primary, other.classes);
    }

    public Map<String, ClassBuilder<?, ?>> primary() {
        return primary;
    }

    public Map<String, ClassBuilder<?, ?>> secondary() {
        return secondary;
    }

    public Map<String, byte[]> build() throws IOException {
        Map<String, byte[]> data = new HashMap<>();
        for (Map.Entry<String, ClassBuilder<?, ?>> entry : primary.entrySet()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ClassFileView view = entry.getValue().build();
            library.write(view, out);
            data.put(entry.getKey(), out.toByteArray());
        }
        return data;
    }

    public Map<String, ClassBuilder<?, ?>> builders() {
        return classes;
    }

    public InheritanceChecker inheritanceChecker() {
        return checker;
    }

    public static ClassHierarchy of(Map<String, byte[]> primary, Map<String, byte[]> secondary) throws IOException {
        Map<String, ClassBuilder<?, ?>> primaryClasses = new HashMap<>();
        Map<String, ClassBuilder<?, ?>> secondaryClasses = new HashMap<>();

        BytecodeLibrary library = new AsmBytecodeLibrary(ClassWriterProvider.flags(0));
        readClasses(primary, primaryClasses, library);
        readClasses(secondary, secondaryClasses, library);

        return new ClassHierarchy(primaryClasses, secondaryClasses);
    }

    private static void readClasses(Map<String, byte[]> classes, Map<String, ClassBuilder<?, ?>> map,
                                    BytecodeLibrary library) throws IOException {
        for (Map.Entry<String, byte[]> entry : classes.entrySet()) {
            ByteArrayInputStream in = new ByteArrayInputStream(entry.getValue());
            ClassBuilder<?, ?> builder = new GenericClassBuilder();
            library.read(in, builder);
            map.put(entry.getKey(), builder);
        }
    }

    public static ClassHierarchy merge(ClassHierarchy... hierarchies) {
        Map<String, ClassBuilder<?, ?>> classes = new HashMap<>();
        for (ClassHierarchy hierarchy : hierarchies) {
            classes.putAll(hierarchy.classes);
        }

        return new ClassHierarchy(classes);
    }

    private class ClassHierarchyClassWriterProvider implements ClassWriterProvider {

        @Override
        public ClassWriter newClassWriterFor(ClassReader classReader, ClassFileView classFileView) {
            return new JvmClassWriter(classReader, 0, checker);
        }

        @Override
        public ClassWriter newClassWriterFor(ClassFileView classFileView) {
            return new JvmClassWriter(0, checker);
        }

    }

    private class ClassHierarchyInheritanceChecker implements InheritanceChecker {

        @Override
        public boolean isSubclassOf(String child, String parent) {
            var childClass = classes.get(child);
            var parentClass = classes.get(parent);

            if (childClass == null || parentClass == null)
                return false;

            if (childClass.equals(parentClass))
                return true;

            InstanceType superType = childClass.getSuperClass();
            if(superType == null)
                return false;

            return isSubclassOf(superType.externalName(), parent);
        }

        @Override
        public String getCommonSuperclass(String type1, String type2) {
            if (isSubclassOf(type1, type2))
                return type2;

            if (isSubclassOf(type2, type1))
                return type1;

            var type1Class = classes.get(type1);
            var type2Class = classes.get(type2);

            if (type1Class == null || type2Class == null)
                return type1;

            if ((type1Class.accessFlags() & AccessFlag.ACC_INTERFACE) != 0
                    || (type2Class.accessFlags() & AccessFlag.ACC_INTERFACE) != 0) {
                return "java/lang/Object";
            }

            do {
                InstanceType type1Super = type1Class.getSuperClass();
                if (type1Super == null)
                    return "java/lang/Object";

                type1 = type1Super.externalName();
                type1Class = classes.get(type1);
            } while (!isSubclassOf(type2, type1));
            return type1;
        }
    }

}
