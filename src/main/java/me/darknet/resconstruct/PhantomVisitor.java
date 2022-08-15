package me.darknet.resconstruct;

import me.darknet.resconstruct.util.TypeUtils;
import org.objectweb.asm.*;

/**
 * Generates basic phantom outlines for referenced types from field and invoke instructions.
 */
public class PhantomVisitor extends ClassVisitor {
	private final ClassHierarchy hierarchy;
	private final int api;

	protected PhantomVisitor(int api, ClassVisitor classVisitor, Reconstruct reconstruct) {
		super(api, classVisitor);
		this.hierarchy = reconstruct.getHierarchy();
		this.api = api;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		return new PhantomMethodVisitor(super.visitMethod(access, name, descriptor, signature, exceptions));
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		AnnotationVisitor av = super.visitAnnotation(descriptor, visible);
		return new PhantomAnnotationVisitor(Type.getType(descriptor), av);
	}

	@Override
	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
		AnnotationVisitor av = super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
		return new PhantomAnnotationVisitor(Type.getType(descriptor), av);
	}

	public class PhantomMethodVisitor extends MethodVisitor {
		public PhantomMethodVisitor(MethodVisitor methodVisitor) {
			super(PhantomVisitor.this.api, methodVisitor);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean itf) {
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
			Type type = Type.getObjectType(owner);
			// Get/create phantom
			PhantomClass phantomClass = hierarchy.getOrCreate(type);
			// Add field to phantom
			phantomClass.addFieldUsage(opcode, name, descriptor);
			// Continue visitor chain
			super.visitFieldInsn(opcode, owner, name, descriptor);
		}

		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			AnnotationVisitor av = super.visitAnnotation(descriptor, visible);
			return new PhantomAnnotationVisitor(Type.getType(descriptor), av);
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
			AnnotationVisitor av = super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
			return new PhantomAnnotationVisitor(Type.getType(descriptor), av);
		}

		@Override
		public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
			AnnotationVisitor av = super.visitParameterAnnotation(parameter, descriptor, visible);
			return new PhantomAnnotationVisitor(Type.getType(descriptor), av);
		}

		@Override
		public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
			AnnotationVisitor av = super.visitInsnAnnotation(typeRef, typePath, descriptor, visible);
			return new PhantomAnnotationVisitor(Type.getType(descriptor), av);
		}

		@Override
		public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
			AnnotationVisitor av = super.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible);
			return new PhantomAnnotationVisitor(Type.getType(descriptor), av);
		}

		@Override
		public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String descriptor, boolean visible) {
			AnnotationVisitor av = super.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, descriptor, visible);
			return new PhantomAnnotationVisitor(Type.getType(descriptor), av);
		}
	}

	public class PhantomAnnotationVisitor extends AnnotationVisitor {
		private final Type type;
		private final PhantomClass phantomAnno;

		public PhantomAnnotationVisitor(Type type, AnnotationVisitor av) {
			super(PhantomVisitor.this.api, av);
			this.type = type;
			phantomAnno = hierarchy.getOrCreate(type);
			phantomAnno.setSuperType(TypeUtils.ANNO_TYPE.getInternalName());
		}

		@Override
		public void visit(String name, Object value) {
			phantomAnno.addMethodUsage(Opcodes.INVOKEINTERFACE, name, lookup(value.getClass()));
			super.visit(name, value);
		}

		@Override
		public void visitEnum(String name, String descriptor, String value) {
			// TODO: check if desc is true 'desc' or 'internal' format
			phantomAnno.addMethodUsage(Opcodes.INVOKEINTERFACE, name, lookup(value.getClass()));
			super.visitEnum(name, descriptor, value);
		}

		@Override
		public AnnotationVisitor visitAnnotation(String name, String descriptor) {
			// TODO: check if desc is true 'desc' or 'internal' format
			Type type = Type.getType(descriptor);
			phantomAnno.addMethodUsage(Opcodes.INVOKEINTERFACE, name, "()" + descriptor);
			return new PhantomAnnotationVisitor(type, super.visitAnnotation(name, descriptor));
		}

		@Override
		public AnnotationVisitor visitArray(String name) {
			// TODO: Read the docs and decide what to handle here vs in standard 'visit'
			return super.visitArray(name);
		}

		private String lookup(Class<?> cls) {
			StringBuilder array = new StringBuilder();
			while (cls.isArray()) {
				array.append("[");
				cls = cls.getComponentType();
			}
			// This mess exists because ASM passes us an object and no actual info on what the original type of that object is.
			if (cls.equals(Type.class))
				cls = Class.class;
			else if (cls.equals(Byte.class))
				cls = byte.class;
			else if (cls.equals(Boolean.class))
				cls = boolean.class;
			else if (cls.equals(Character.class))
				cls = char.class;
			else if (cls.equals(Short.class))
				cls = short.class;
			else if (cls.equals(Integer.class))
				cls = int.class;
			else if (cls.equals(Long.class))
				cls = long.class;
			else if (cls.equals(Float.class))
				cls = float.class;
			else if (cls.equals(Double.class))
				cls = double.class;
			return "()" + array + Type.getType(cls).getDescriptor();
		}
	}
}
