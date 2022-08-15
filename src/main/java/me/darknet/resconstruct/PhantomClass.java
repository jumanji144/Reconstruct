package me.darknet.resconstruct;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PhantomClass {

	public Type type;
	public int access;
	public boolean isCp;
	public Map<String, MethodMember> methods = new HashMap<>();
	public Map<String, FieldMember> fields = new HashMap<>();
	public String superType;
	public List<String> interfaces = new ArrayList<>();
	public List<Type> inheritors = new ArrayList<>();

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
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}

		for (FieldMember field : fields.values()) {
			cw.visitField(field.access, field.name, field.desc, null, null);
		}

		cw.visitEnd();

		return cw.toByteArray();
	}

	public void addMethodUsage(int opcode, String name, String descriptor) {
		String key = name + descriptor;
		if (!methods.containsKey(key)) {
			MethodMember method = new MethodMember(Opcodes.ACC_PUBLIC, name, descriptor);
			methods.put(key, method);
		}
	}
}
