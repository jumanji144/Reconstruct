package me.darknet.resconstruct;

import me.coley.analysis.util.TypeUtil;
import me.darknet.resconstruct.info.FieldMember;
import me.darknet.resconstruct.info.MethodMember;
import me.darknet.resconstruct.util.AccessUtils;
import me.darknet.resconstruct.util.TypeUtils;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.*;

public class PhantomClass {
	private final Map<String, MethodMember> methods = new HashMap<>();
	private final Map<String, FieldMember> fields = new HashMap<>();
	private final Set<String> interfaces = new HashSet<>();
	private final Set<PhantomClass> implementCandidates = new HashSet<>();
	private final Set<PhantomClass> childCandidates = new HashSet<>();
	private final Type type;
	private String superType = "java/lang/Object";
	private int access = Opcodes.ACC_PUBLIC;
	private boolean isObject;

	/**
	 * @param type
	 * 		The phantom wrapped type.
	 */
	public PhantomClass(Type type) {
		this.type = type;
	}

	public void addMethodUsage(int opcode, String name, String descriptor) {
		String key = name + descriptor;
		if (!methods.containsKey(key)) {
			int mods = Opcodes.ACC_PUBLIC;
			if (opcode == Opcodes.INVOKESTATIC) {
				mods |= Opcodes.ACC_STATIC;
			}
			MethodMember method = new MethodMember(mods, name, descriptor);
			if (opcode == Opcodes.INVOKEINTERFACE) {
				method.setInterface();
			}
			methods.put(key, method);
		}
	}

	public void addFieldUsage(int opcode, String name, String descriptor) {
		String key = name + "." + descriptor;
		if (!fields.containsKey(key)) {
			int mods = Opcodes.ACC_PUBLIC;
			if (opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC) {
				mods |= Opcodes.ACC_STATIC;
			}
			FieldMember field = new FieldMember(mods, name, descriptor);
			fields.put(key, field);
		}
	}

	public byte[] generate(int version) {
		if (isAnnotation()) {
			access |= Opcodes.ACC_ANNOTATION | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE;
		}

		ClassWriter cw = new ClassWriter(0);
		cw.visit(version, access, type.getInternalName(), null, superType, interfaces.toArray(new String[]{}));

		for (MethodMember method : methods.values()) {
			MethodVisitor mv = cw.visitMethod(method.access, method.name, method.desc, null, null);
			mv.visitCode();
			mv.visitTypeInsn(Opcodes.NEW, "java/lang/RuntimeException");
			mv.visitInsn(Opcodes.DUP);
			mv.visitLdcInsn("stub");
			mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V", false);
			mv.visitInsn(Opcodes.ATHROW);
			mv.visitMaxs(1, AccessUtils.isStatic(method.access) ? 0 : 1);
			mv.visitEnd();
		}

		for (FieldMember field : fields.values()) {
			cw.visitField(field.access, field.name, field.desc, null, null);
		}

		cw.visitEnd();
		return cw.toByteArray();
	}

	public boolean isInterface() {
		return isAnnotation() || methods.values().stream()
				.anyMatch(MethodMember::isInterface);
	}

	public boolean isAnnotation() {
		return superType.equals(TypeUtils.ANNO_TYPE.getInternalName());
	}

	public Map<String, MethodMember> getMethods() {
		return Collections.unmodifiableMap(methods);
	}

	public Map<String, FieldMember> getFields() {
		return Collections.unmodifiableMap(fields);
	}

	public Set<String> getInterfaces() {
		return interfaces;
	}

	public void addInterface(String itf) {
		interfaces.add(itf);
	}

	public Set<PhantomClass> getImplementCandidates() {
		return implementCandidates;
	}

	public Set<PhantomClass> getChildCandidates() {
		return childCandidates;
	}

	public void addImplementCandidate(PhantomClass other) {
		Type otherType = other.getType();
		if (TypeUtil.OBJECT_TYPE.equals(otherType) || getType().equals(otherType))
			return;
		implementCandidates.add(other);
		other.childCandidates.add(this);
	}

	public void addTypeHint(PhantomClass other) {
		this.addImplementCandidate(other);
		other.getChildCandidates().forEach(t -> t.addImplementCandidate(this));
	}

	public void addImplements(Type type) {
		this.interfaces.add(type.getInternalName());
	}

	public Type getType() {
		return type;
	}

	public String getTypeName() {
		return type.getInternalName();
	}

	public String getSuperType() {
		return superType;
	}

	public void setSuperType(String superType) {
		this.superType = superType;
	}

	public boolean isObject() {
		return isObject;
	}

	public void setIsObject(boolean isObject) {
		this.isObject = isObject;
	}

	public void setAccess(int access) {
		this.access = access;
	}

	public int getAccess() {
		return this.access;
	}

	public boolean isCp() {
		return false;
	}

	@Override
	public String toString() {
		return type.getInternalName();
	}
}
