package net.luis.agent.asm;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;

/**
 *
 * @author Luis-St
 *
 */

public interface Instrumentations {
	
	@NotNull MethodVisitor getDelegate();
	
	//region Number loading as
	default void loadNumberAsInt(@NotNull Type type, int index) {
		MethodVisitor mv = this.getDelegate();
		mv.visitVarInsn(type.getOpcode(Opcodes.ILOAD), index);
		if (type.equals(Type.LONG_TYPE)) {
			mv.visitInsn(Opcodes.L2I);
		} else if (type.equals(Type.FLOAT_TYPE)) {
			mv.visitInsn(Opcodes.F2I);
		} else if (type.equals(Type.DOUBLE_TYPE)) {
			mv.visitInsn(Opcodes.D2I);
		}
	}
	
	default void loadNumberAsLong(@NotNull Type type, int index) {
		MethodVisitor mv = this.getDelegate();
		mv.visitVarInsn(type.getOpcode(Opcodes.ILOAD), index);
		if (type.equals(Type.BYTE_TYPE) || type.equals(Type.SHORT_TYPE) || type.equals(Type.INT_TYPE)) {
			mv.visitInsn(Opcodes.I2L);
		} else if (type.equals(Type.FLOAT_TYPE)) {
			mv.visitInsn(Opcodes.F2L);
		} else if (type.equals(Type.DOUBLE_TYPE)) {
			mv.visitInsn(Opcodes.D2L);
		}
	}
	
	default void loadNumberAsFloat(@NotNull Type type, int index) {
		MethodVisitor mv = this.getDelegate();
		mv.visitVarInsn(type.getOpcode(Opcodes.ILOAD), index);
		if (type.equals(Type.BYTE_TYPE) || type.equals(Type.SHORT_TYPE) || type.equals(Type.INT_TYPE)) {
			mv.visitInsn(Opcodes.I2F);
		} else if (type.equals(Type.LONG_TYPE)) {
			mv.visitInsn(Opcodes.L2F);
		} else if (type.equals(Type.DOUBLE_TYPE)) {
			mv.visitInsn(Opcodes.D2F);
		}
	}
	
	default void loadNumberAsDouble(@NotNull Type type, int index) {
		MethodVisitor mv = this.getDelegate();
		mv.visitVarInsn(type.getOpcode(Opcodes.ILOAD), index);
		if (type.equals(Type.BYTE_TYPE) || type.equals(Type.SHORT_TYPE) || type.equals(Type.INT_TYPE)) {
			mv.visitInsn(Opcodes.I2D);
		} else if (type.equals(Type.LONG_TYPE)) {
			mv.visitInsn(Opcodes.L2D);
		} else if (type.equals(Type.FLOAT_TYPE)) {
			mv.visitInsn(Opcodes.F2D);
		}
	}
	//endregion
	
	default void instrumentThrownException(@NotNull Type type, @NotNull String message) {
		MethodVisitor mv = this.getDelegate();
		mv.visitTypeInsn(Opcodes.NEW, type.getInternalName());
		mv.visitInsn(Opcodes.DUP);
		mv.visitLdcInsn(message);
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, type.getInternalName(), "<init>", "(Ljava/lang/String;)V", false);
		mv.visitInsn(Opcodes.ATHROW);
	}
	
	default void instrumentPatternCheck(@NotNull String pattern, int index, @NotNull Label end) {
		MethodVisitor mv = this.getDelegate();
		mv.visitLdcInsn(pattern);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/regex/Pattern", "compile", "(Ljava/lang/String;)Ljava/util/regex/Pattern;", false);
		mv.visitVarInsn(Opcodes.ALOAD, index);
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/regex/Pattern", "matcher", "(Ljava/lang/CharSequence;)Ljava/util/regex/Matcher;", false);
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/regex/Matcher", "matches", "()Z", false);
		mv.visitJumpInsn(Opcodes.IFNE, end);
	}
	
	default void instrumentNonNullCheck(@NotNull String message) {
		MethodVisitor mv = this.getDelegate();
		mv.visitLdcInsn(message);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Objects", "requireNonNull", "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;", false);
	}
}
