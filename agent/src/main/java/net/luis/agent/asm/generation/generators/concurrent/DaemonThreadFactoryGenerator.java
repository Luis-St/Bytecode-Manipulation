package net.luis.agent.asm.generation.generators.concurrent;

import net.luis.agent.asm.generation.Generator;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;

import static net.luis.agent.asm.Instrumentations.*;
import static net.luis.agent.asm.Types.*;
import static net.luis.agent.asm.generation.Generations.*;

/**
 *
 * @author Luis-St
 *
 */

public class DaemonThreadFactoryGenerator extends Generator {
	
	public DaemonThreadFactoryGenerator() {
		super(DAEMON_THREAD_FACTORY.getClassName());
	}
	
	@Override
	public void generate(@NotNull ClassVisitor cv) {
		cv.visit(CLASS_VERSION, Opcodes.ACC_PUBLIC, DAEMON_THREAD_FACTORY.getInternalName(), null, "java/lang/Object", new String[] { THREAD_FACTORY.getInternalName() });
		cv.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "defaultFactory", THREAD_FACTORY.getDescriptor(), null, null).visitEnd();
		this.generateConstructor(cv);
		this.generateNewThread(cv);
		cv.visitEnd();
	}
	
	private void generateConstructor(@NotNull ClassVisitor cv) {
		MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
		Label start = new Label();
		Label end = new Label();
		mv.visitCode();
		mv.visitLabel(start);
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/concurrent/Executors", "defaultThreadFactory", "()" + THREAD_FACTORY.getDescriptor(), false);
		mv.visitFieldInsn(Opcodes.PUTFIELD, DAEMON_THREAD_FACTORY.getInternalName(), "defaultFactory", THREAD_FACTORY.getDescriptor());
		mv.visitInsn(Opcodes.RETURN);
		mv.visitLabel(end);
		mv.visitLocalVariable("this", DAEMON_THREAD_FACTORY.getDescriptor(), null, start, end, 0);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}
	
	private void generateNewThread(@NotNull ClassVisitor cv) {
		MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, "newThread", "(Ljava/lang/Runnable;)Ljava/lang/Thread;", null, null);
		Label start = new Label();
		Label end = new Label();
		mv.visitAnnotation(NOT_NULL.getDescriptor(), false).visitEnd();
		mv.visitParameter("runnable", 0);
		mv.visitParameterAnnotation(0, NOT_NULL.getDescriptor(), false).visitEnd();
		mv.visitCode();
		mv.visitLabel(start);
		instrumentNonNullCheck(mv, 1, "Runnable must not be null");
		mv.visitInsn(Opcodes.POP);
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitFieldInsn(Opcodes.GETFIELD, DAEMON_THREAD_FACTORY.getInternalName(), "defaultFactory", THREAD_FACTORY.getDescriptor());
		mv.visitVarInsn(Opcodes.ALOAD, 1);
		mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/concurrent/ThreadFactory", "newThread", "(Ljava/lang/Runnable;)Ljava/lang/Thread;", true);
		mv.visitVarInsn(Opcodes.ASTORE, 2);
		mv.visitVarInsn(Opcodes.ALOAD, 2);
		mv.visitInsn(Opcodes.ICONST_1);
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Thread", "setDaemon", "(Z)V", false);
		mv.visitVarInsn(Opcodes.ALOAD, 2);
		mv.visitInsn(Opcodes.ARETURN);
		mv.visitLabel(end);
		mv.visitLocalVariable("this", DAEMON_THREAD_FACTORY.getDescriptor(), null, start, end, 0);
		mv.visitLocalVariable("runnable", "Ljava/lang/Runnable;", null, start, end, 1);
		mv.visitLocalVariable("thread", "Ljava/lang/Thread;", null, start, end, 2);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}
}
