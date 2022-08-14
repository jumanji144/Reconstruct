package me.darknet.resconstruct;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.Map;

public class ClassHierarchy {
	Map<Type, PhantomClass> phantoms = new HashMap<>();

	public void addPhantom(PhantomClass phantomClass) {
		phantoms.put(phantomClass.type, phantomClass);
	}

	public boolean contains(Type type) {
		return phantoms.containsKey(type);
	}

	public PhantomClass get(Type type) {
		if(phantoms.containsKey(type)) {
			return phantoms.get(type);
		} else {
			// create new class based off of type
			PhantomClass phantomClass = new PhantomClass();
			phantomClass.type = type;
			phantomClass.access = Opcodes.ACC_PUBLIC;
		}
		return phantoms.get(type);
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
