package me.darknet.resconstruct;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class PhantomVisitor extends ClassVisitor {
	private final Reconstruct reconstruct;
	private final int api;

	protected PhantomVisitor(int api, ClassVisitor classVisitor, Reconstruct reconstruct) {
		super(api, classVisitor);
		this.reconstruct = reconstruct;
		this.api = api;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		return new PhantomMethodVisitor(super.visitMethod(access, name, descriptor, signature, exceptions));
	}

	public class PhantomMethodVisitor extends MethodVisitor {
		public PhantomMethodVisitor(MethodVisitor methodVisitor) {
			super(PhantomVisitor.this.api, methodVisitor);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean itf) {
			ClassHierarchy hierarchy = reconstruct.getHierarchy();
			Type type = Type.getObjectType(owner);
			PhantomClass phantomClass;

			if (!hierarchy.contains(type)) {
				phantomClass = new PhantomClass();
				phantomClass.type = type;
				hierarchy.addPhantom(phantomClass);
			} else {
				phantomClass = hierarchy.get(type);
			}

			if (!phantomClass.methods.containsKey(name + descriptor)) {
				MethodMember method = new MethodMember(Opcodes.ACC_PUBLIC, name, descriptor);
				phantomClass.methods.put(name + descriptor, method);
			}

			super.visitMethodInsn(opcode, owner, name, descriptor, itf);
		}
	}
}
