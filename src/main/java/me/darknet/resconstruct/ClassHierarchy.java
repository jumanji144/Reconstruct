package me.darknet.resconstruct;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.Map;

public class ClassHierarchy {
	Map<Type, PhantomClass> phantoms = new HashMap<>();

	public boolean contains(Type type) {
		return phantoms.containsKey(type);
	}

	public PhantomClass getOrCreate(Type type) {
		return phantoms.computeIfAbsent(type, t -> {
			PhantomClass phantomClass = new PhantomClass();
			phantomClass.isCp = InheritanceUtils.isClasspathType(t);
			phantomClass.type = t;
			phantomClass.access = Opcodes.ACC_PUBLIC;
			return phantomClass;
		});
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		phantoms.forEach((type, phantomClass) -> {
			sb.append("Class ").append(type.getClassName()).append(":\n");
			phantomClass.methods.forEach((name, method) -> {
				sb.append("\t- ");
				sb.append(method.name).append(method.desc).append("\n");
			});
		});
		return sb.toString();
	}
}
