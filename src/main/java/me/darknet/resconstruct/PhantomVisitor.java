package me.darknet.resconstruct;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Generates basic phantom outlines for referenced types from field and invoke instructions.
 */
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
			// Get/create phantom
			PhantomClass phantomClass = hierarchy.getOrCreate(type);
			// Add method to phantom
			phantomClass.addMethodUsage(opcode, name, descriptor);
			// Continue visitor chain
			super.visitMethodInsn(opcode, owner, name, descriptor, itf);
		}

		@Override
		public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
			ClassHierarchy hierarchy = reconstruct.getHierarchy();
			Type type = Type.getObjectType(owner);
			// Get/create phantom
			PhantomClass phantomClass = hierarchy.getOrCreate(type);
			// Add field to phantom
			phantomClass.addFieldUsage(opcode, name, descriptor);
			// Continue visitor chain
			super.visitFieldInsn(opcode, owner, name, descriptor);
		}
	}
}
