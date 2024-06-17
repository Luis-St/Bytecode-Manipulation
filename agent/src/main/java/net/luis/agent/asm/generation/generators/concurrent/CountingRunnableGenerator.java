package net.luis.agent.asm.generation.generators.concurrent;

import net.luis.agent.asm.generation.Generator;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;

import static net.luis.agent.asm.Instrumentations.*;
import static net.luis.agent.asm.Types.*;

/**
 *
 * @author Luis-St
 *
 */

public class CountingRunnableGenerator extends Generator {
	
	private static final String ACTION_SIGNATURE = "Ljava/util/function/Consumer<Ljava/lang/Integer;>;";
	
	private static final String CONSTRUCTOR_SIGNATURE = "(Ljava/util/function/Consumer<Ljava/lang/Integer;>;)V";
	
	public CountingRunnableGenerator() {
		super(COUNTING_RUNNABLE.getClassName());
	}
	
	@Override
	public void generate(@NotNull ClassVisitor cv) {
		cv.visit(CLASS_VERSION, Opcodes.ACC_PUBLIC, COUNTING_RUNNABLE.getInternalName(), null, "java/lang/Object", new String[] { RUNNABLE.getInternalName() });
		cv.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "action", CONSUMER.getDescriptor(), ACTION_SIGNATURE, null).visitEnd();
		cv.visitField(Opcodes.ACC_PRIVATE, "count", "I", null, 0).visitEnd();
		this.generateConstructor(cv);
		this.generateRun(cv);
		cv.visitEnd();
	}
	
	private void generateConstructor(@NotNull ClassVisitor cv) {
		MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(Ljava/util/function/Consumer;)V", CONSTRUCTOR_SIGNATURE, null);
		Label start = new Label();
		Label end = new Label();
		mv.visitParameter("action", 0);
		mv.visitParameterAnnotation(0, NOT_NULL.getDescriptor(), false).visitEnd();
		mv.visitCode();
		mv.visitLabel(start);
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		instrumentNonNullCheck(mv, 1, "Action must not be null");
		mv.visitInsn(Opcodes.POP);
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitVarInsn(Opcodes.ALOAD, 1);
		mv.visitFieldInsn(Opcodes.PUTFIELD, COUNTING_RUNNABLE.getInternalName(), "action", CONSUMER.getDescriptor());
		mv.visitInsn(Opcodes.RETURN);
		mv.visitLabel(end);
		mv.visitLocalVariable("this", COUNTING_RUNNABLE.getDescriptor(), null, start, end, 0);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}
	
	private void generateRun(@NotNull ClassVisitor cv) {
		MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, "run", "()V", null, null);
		Label start = new Label();
		Label end = new Label();
		mv.visitCode();
		mv.visitLabel(start);
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitFieldInsn(Opcodes.GETFIELD, COUNTING_RUNNABLE.getInternalName(), "action", CONSUMER.getDescriptor());
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitInsn(Opcodes.DUP);
		mv.visitFieldInsn(Opcodes.GETFIELD, COUNTING_RUNNABLE.getInternalName(), "count", "I");
		mv.visitInsn(Opcodes.DUP_X1);
		mv.visitInsn(Opcodes.ICONST_1);
		mv.visitInsn(Opcodes.IADD);
		mv.visitFieldInsn(Opcodes.PUTFIELD, COUNTING_RUNNABLE.getInternalName(), "count", "I");
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
		mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, CONSUMER.getInternalName(), "accept", "(Ljava/lang/Object;)V", true);
		mv.visitInsn(Opcodes.RETURN);
		mv.visitLabel(end);
		mv.visitLocalVariable("this", COUNTING_RUNNABLE.getDescriptor(), null, start, end, 0);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}
}
