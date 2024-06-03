package net.luis.agent.asm.generation;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;

/**
 *
 * @author Luis-St
 *
 */

public class Generations {
	
	public static void generateDefaultConstructor(@NotNull ClassVisitor visitor, @NotNull Type type) {
		MethodVisitor mv = visitor.visitMethod(Opcodes.ACC_PRIVATE, "<init>", "()V", null, null);
		Label start = new Label();
		Label end = new Label();
		mv.visitCode();
		mv.visitLabel(start);
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		mv.visitInsn(Opcodes.RETURN);
		mv.visitLabel(end);
		mv.visitLocalVariable("this", type.getDescriptor(), null, start, end, 0);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}
}
