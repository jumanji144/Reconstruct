package me.darknet.resconstruct;

import me.darknet.resconstruct.util.InheritanceUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ClassHierarchy {
	private final Map<String, PhantomClass> phantoms = new HashMap<>();
	private final Set<String> inputPhantoms = new HashSet<>();

	public boolean contains(Type type) {
		return contains(type.getInternalName());
	}

	public boolean contains(String type) {
		return phantoms.containsKey(type);
	}

	public void createInputPhantom(ClassReader cr) {
		Type type = Type.getObjectType(cr.getClassName());
		String typeName = type.getInternalName();
		PhantomClass phantom = new PhantomClass(type);
		phantom.setSuperType(cr.getSuperName());
		for (String itf : cr.getInterfaces())
			phantom.addInterface(itf);
		phantom.setAccess(cr.getAccess());
		phantoms.put(typeName, phantom);
		inputPhantoms.add(typeName);
	}

	public PhantomClass getOrCreate(Type type) {
		return phantoms.computeIfAbsent(type.getInternalName(), t -> {
			boolean isCp = InheritanceUtils.isClasspathType(type);
			PhantomClass phantom = new PhantomClass(type) {
				@Override
				public boolean isCp() {
					return isCp;
				}
			};
			phantom.setAccess(Opcodes.ACC_PUBLIC);
			return phantom;
		});
	}

	public boolean isPhantomOnClasspath(PhantomClass phantom) {
		return phantom.isCp();
	}

	public boolean isPhantomFromInput(PhantomClass phantom) {
		return inputPhantoms.contains(phantom.getTypeName());
	}

	public Map<String, byte[]> export() {
		return phantoms.entrySet().stream()
				.filter(e -> !isPhantomOnClasspath(e.getValue()) && !isPhantomFromInput(e.getValue()))
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						e -> e.getValue().generate(Opcodes.V1_8)
				));
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		phantoms.forEach((typeName, phantomClass) -> {
			if (phantomClass.isCp() || inputPhantoms.contains(typeName))
				return;
			sb.append("Class ").append(typeName.replace('/', '.')).append(":\n");
			phantomClass.getMethods().forEach((name, method) -> {
				sb.append("\t- ");
				sb.append(method.name).append(method.desc).append("\n");
			});
		});
		return sb.toString();
	}
}
