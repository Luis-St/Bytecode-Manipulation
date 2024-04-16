package net.luis.asm;

import org.objectweb.asm.*;

/**
 *
 * @author Luis-St
 *
 */

public class LoggerFieldVisitor extends ClassVisitor {
	
	private final String clazz;
	private boolean loggerAbsent = true;
	
	public LoggerFieldVisitor(ClassWriter writer, String clazz) {
		super(Opcodes.ASM9, writer);
		this.clazz = clazz;
	}
	
	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		if ("LOGGER".equals(name)) {
			this.loggerAbsent = false;
		}
		return super.visitField(access, name, descriptor, signature, value);
	}
	
	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
		return new MethodVisitor(Opcodes.ASM9, mv) {
			private boolean hasLoggingAnnotation = false;
			private String loggingMessage = null;
			
			@Override
			public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
				if ("Lnet/luis/annotation/Logging;".equals(descriptor)) {
					this.hasLoggingAnnotation = true;
					return new AnnotationVisitor(Opcodes.ASM9) {
						@Override
						public void visit(String name, Object value) {
							if ("message".equals(name)) {
								loggingMessage = (String) value;
							}
							super.visit(name, value);
						}
					};
				}
				return super.visitAnnotation(descriptor, visible);
			}
			
			@Override
			public void visitCode() {
				if (this.hasLoggingAnnotation) {
					this.mv.visitFieldInsn(Opcodes.GETSTATIC, LoggerFieldVisitor.this.clazz, "LOGGER", "Lorg/apache/logging/log4j/Logger;");
					this.mv.visitLdcInsn(this.loggingMessage);
					this.mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/apache/logging/log4j/Logger", "info", "(Ljava/lang/String;)V", true);
				}
				super.visitCode();
			}
		};
	}
	
	@Override
	public void visitEnd() {
		if (this.loggerAbsent) {
			FieldVisitor fv = this.cv.visitField(Opcodes.ACC_PRIVATE + Opcodes.ACC_FINAL + Opcodes.ACC_STATIC, "LOGGER", "Lorg/apache/logging/log4j/Logger;", null, null);
			if (fv != null) {
				fv.visitEnd();
			}
			
			MethodVisitor mv = this.cv.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
			if (mv != null) {
				mv.visitCode();
				mv.visitLdcInsn(Type.getType("L" + this.clazz + ";"));
				mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/apache/logging/log4j/LogManager", "getLogger", "(Ljava/lang/Class;)Lorg/apache/logging/log4j/Logger;", false);
				mv.visitFieldInsn(Opcodes.PUTSTATIC, this.clazz, "LOGGER", "Lorg/apache/logging/log4j/Logger;");
				mv.visitInsn(Opcodes.RETURN);
				mv.visitMaxs(2, 0);
				mv.visitEnd();
			}
		}
		super.visitEnd();
	}
}
