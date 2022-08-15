package me.darknet.resconstruct;

import me.darknet.resconstruct.info.FieldMember;
import me.darknet.resconstruct.info.MethodMember;
import me.darknet.resconstruct.util.AccessUtils;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.*;

public class PhantomClass {
	private final Map<String, MethodMember> methods = new HashMap<>();
	private final Map<String, FieldMember> fields = new HashMap<>();
	private final List<String> interfaces = new ArrayList<>();
	private final List<Type> inheritors = new ArrayList<>();
	private final Type type;
	private String superType;
	private int access;

	/**
	 * @param type The phantom wrapped type.
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

	public Map<String, MethodMember> getMethods() {
		return Collections.unmodifiableMap(methods);
	}

	public Map<String, FieldMember> getFields() {
		return Collections.unmodifiableMap(fields);
	}

	public List<String> getInterfaces() {
		return interfaces;
	}

	public void addInterface(String itf) {
		interfaces.add(itf);
	}

	public List<Type> getInheritors() {
		return inheritors;
	}

	public void addInheritor(Type type) {
		inheritors.add(type);
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

	public int getAccess() {
		return access;
	}

	public void setAccess(int access) {
		this.access = access;
	}

	public boolean isCp() {
		return false;
	}
}
