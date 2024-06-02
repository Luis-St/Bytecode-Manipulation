package net.luis.agent.asm.generation;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;

/**
 *
 * @author Luis-St
 *
 */

public class GenerationUtils {
	
	public static void generateDefaultConstructor(@NotNull ClassVisitor visitor) {
		MethodVisitor mv = visitor.visitMethod(Opcodes.ACC_PRIVATE, "<init>", "()V", null, null);
		mv.visitCode();
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		mv.visitInsn(Opcodes.RETURN);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}
}
