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
	
	@NotNull MethodVisitor getDelegate();
	
	//region Internal constant loading
	private void loadIntegerConstant(int i) {
		MethodVisitor mv = this.getDelegate();
		if (i >= -1 && i <= 5) {
			mv.visitInsn(Opcodes.ICONST_0 + i);
		} else if (i >= Byte.MIN_VALUE && i <= Byte.MAX_VALUE) {
			mv.visitIntInsn(Opcodes.BIPUSH, i);
		} else if (i >= Short.MIN_VALUE && i <= Short.MAX_VALUE) {
			mv.visitIntInsn(Opcodes.SIPUSH, i);
		} else {
			mv.visitLdcInsn(i);
		}
	}
	
	private void loadLongConstant(long l) {
		MethodVisitor mv = this.getDelegate();
		if (l == 0L || l == 1L) {
			mv.visitInsn(Opcodes.LCONST_0 + (int) l);
		} else {
			mv.visitLdcInsn(l);
		}
	}
	
	private void loadFloatConstant(float f) {
		MethodVisitor mv = this.getDelegate();
		if (f == 0.0F) {
			mv.visitInsn(Opcodes.FCONST_0);
		} else if (f == 1.0F) {
			mv.visitInsn(Opcodes.FCONST_1);
		} else if (f == 2.0F) {
			mv.visitInsn(Opcodes.FCONST_2);
		} else {
			mv.visitLdcInsn(f);
		}
	}
	
	private void loadDoubleConstant(double d) {
		MethodVisitor mv = this.getDelegate();
		if (d == 0.0D) {
			mv.visitInsn(Opcodes.DCONST_0);
		} else if (d == 1.0D) {
			mv.visitInsn(Opcodes.DCONST_1);
		} else {
			mv.visitLdcInsn(d);
		}
	}
	//endregion
	
	default void loadNumberConstant(@NotNull Number number) {
		if (number instanceof Byte || number instanceof Short || number instanceof Integer) {
			this.loadIntegerConstant(number.intValue());
		} else if (number instanceof Long l) {
			this.loadLongConstant(l);
		} else if (number instanceof Float f) {
			this.loadFloatConstant(f);
		} else if (number instanceof Double d) {
			this.loadDoubleConstant(d);
		}
	}
}
