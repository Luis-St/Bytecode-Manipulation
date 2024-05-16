package net.luis.agent.asm;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 *
 * @author Luis-St
 *
 */

@SuppressWarnings("FloatingPointEquality")
public interface Constants {
	
	//region Internal constant loading
	private void loadIntegerConstant(@NotNull MethodVisitor visitor, int i) {
		if (i >= -1 && i <= 5) {
			visitor.visitInsn(Opcodes.ICONST_0 + i);
		} else if (i >= Byte.MIN_VALUE && i <= Byte.MAX_VALUE) {
			visitor.visitIntInsn(Opcodes.BIPUSH, i);
		} else if (i >= Short.MIN_VALUE && i <= Short.MAX_VALUE) {
			visitor.visitIntInsn(Opcodes.SIPUSH, i);
		} else {
			visitor.visitLdcInsn(i);
		}
	}
	
	private void loadLongConstant(@NotNull MethodVisitor visitor, long l) {
		if (l == 0L || l == 1L) {
			visitor.visitInsn(Opcodes.LCONST_0 + (int) l);
		} else {
			visitor.visitLdcInsn(l);
		}
	}
	
	private void loadFloatConstant(@NotNull MethodVisitor visitor, float f) {
		if (f == 0.0F) {
			visitor.visitInsn(Opcodes.FCONST_0);
		} else if (f == 1.0F) {
			visitor.visitInsn(Opcodes.FCONST_1);
		} else if (f == 2.0F) {
			visitor.visitInsn(Opcodes.FCONST_2);
		} else {
			visitor.visitLdcInsn(f);
		}
	}
	
	private void loadDoubleConstant(@NotNull MethodVisitor visitor, double d) {
		if (d == 0.0D) {
			visitor.visitInsn(Opcodes.DCONST_0);
		} else if (d == 1.0D) {
			visitor.visitInsn(Opcodes.DCONST_1);
		} else {
			visitor.visitLdcInsn(d);
		}
	}
	//endregion
	
	default void loadNumberConstant(@NotNull MethodVisitor visitor, @NotNull Number number) {
		if (number instanceof Byte || number instanceof Short || number instanceof Integer) {
			this.loadIntegerConstant(visitor, number.intValue());
		} else if (number instanceof Long l) {
			this.loadLongConstant(visitor, l);
		} else if (number instanceof Float f) {
			this.loadFloatConstant(visitor, f);
		} else if (number instanceof Double d) {
			this.loadDoubleConstant(visitor, d);
		}
	}
}
