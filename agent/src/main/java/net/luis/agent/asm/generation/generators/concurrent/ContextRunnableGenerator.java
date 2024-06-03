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

public class ContextRunnableGenerator extends Generator {
	
	public ContextRunnableGenerator() {
		super(CONTEXT_RUNNABLE.getClassName());
	}
	
	@Override
	public void generate(@NotNull ClassVisitor cv) {
		cv.visit(CLASS_VERSION, Opcodes.ACC_PUBLIC, CONTEXT_RUNNABLE.getInternalName(), null, "java/lang/Object", new String[] { RUNNABLE.getInternalName() });
		cv.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "method", STRING.getDescriptor(), null, null).visitEnd();
		cv.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "lookup", MAP.getDescriptor(), "Ljava/util/Map<Ljava/lang/String;Ljava/util/concurrent/ScheduledFuture<*>;>;", null).visitEnd();
		cv.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "action", BI_CONSUMER.getDescriptor(), "Ljava/util/function/BiConsumer<Ljava/lang/Integer;Ljava/util/concurrent/ScheduledFuture<*>;>;", null).visitEnd();
		cv.visitField(Opcodes.ACC_PRIVATE, "future", SCHEDULED_FUTURE.getDescriptor(), "Ljava/util/concurrent/ScheduledFuture<*>;", null).visitEnd();
		cv.visitField(Opcodes.ACC_PRIVATE, "count", "I", null, 0).visitEnd();
		this.generateConstructor(cv);
		this.generateRun(cv);
		cv.visitEnd();
	}
	
	private void generateConstructor(@NotNull ClassVisitor cv) {
		MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(Ljava/lang/String;Ljava/util/Map;Ljava/util/function/BiConsumer;)V", "(Ljava/lang/String;Ljava/util/Map<Ljava/lang/String;Ljava/util/concurrent/ScheduledFuture<*>;>;Ljava/util/function/BiConsumer<Ljava/lang/Integer;Ljava/util/concurrent/ScheduledFuture<*>;>;)V", null);
		Label start = new Label();
		Label end = new Label();
		mv.visitParameter("method", 0);
		mv.visitParameterAnnotation(0, NOT_NULL.getDescriptor(), false).visitEnd();
		mv.visitParameter("lookup", 1);
		mv.visitParameterAnnotation(1, NOT_NULL.getDescriptor(), false).visitEnd();
		mv.visitParameter("action", 2);
		mv.visitParameterAnnotation(2, NOT_NULL.getDescriptor(), false).visitEnd();
		mv.visitCode();
		mv.visitLabel(start);
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		instrumentNonNullCheck(mv, 1, "Method must not be null");
		mv.visitInsn(Opcodes.POP);
		instrumentNonNullCheck(mv, 2, "Lookup must not be null");
		mv.visitInsn(Opcodes.POP);
		instrumentNonNullCheck(mv, 3, "Action must not be null");
		mv.visitInsn(Opcodes.POP);
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitVarInsn(Opcodes.ALOAD, 1);
		mv.visitFieldInsn(Opcodes.PUTFIELD, CONTEXT_RUNNABLE.getInternalName(), "method", STRING.getDescriptor());
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitVarInsn(Opcodes.ALOAD, 2);
		mv.visitFieldInsn(Opcodes.PUTFIELD, CONTEXT_RUNNABLE.getInternalName(), "lookup", MAP.getDescriptor());
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitVarInsn(Opcodes.ALOAD, 3);
		mv.visitFieldInsn(Opcodes.PUTFIELD, CONTEXT_RUNNABLE.getInternalName(), "action", BI_CONSUMER.getDescriptor());
		mv.visitInsn(Opcodes.RETURN);
		mv.visitLabel(end);
		mv.visitLocalVariable("this", CONTEXT_RUNNABLE.getDescriptor(), null, start, end, 0);
		mv.visitLocalVariable("method", STRING.getDescriptor(), null, start, end, 1);
		mv.visitLocalVariable("lookup", MAP.getDescriptor(), "Ljava/util/Map<Ljava/lang/String;Ljava/util/concurrent/ScheduledFuture<*>;>;", start, end, 2);
		mv.visitLocalVariable("action", BI_CONSUMER.getDescriptor(), "Ljava/util/function/BiConsumer<Ljava/lang/Integer;Ljava/util/concurrent/ScheduledFuture<*>;>;", start, end, 3);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}
	
	private void generateRun(@NotNull ClassVisitor cv) {
		MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, "run", "()V", null, null);
		Label start = new Label();
		Label jump = new Label();
		Label end = new Label();
		mv.visitCode();
		mv.visitLabel(start);
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitFieldInsn(Opcodes.GETFIELD, CONTEXT_RUNNABLE.getInternalName(), "future", SCHEDULED_FUTURE.getDescriptor());
		mv.visitJumpInsn(Opcodes.IFNONNULL, jump);
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitFieldInsn(Opcodes.GETFIELD, CONTEXT_RUNNABLE.getInternalName(), "lookup", MAP.getDescriptor());
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitFieldInsn(Opcodes.GETFIELD, CONTEXT_RUNNABLE.getInternalName(), "method", STRING.getDescriptor());
		mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, MAP.getInternalName(), "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
		mv.visitTypeInsn(Opcodes.CHECKCAST, SCHEDULED_FUTURE.getInternalName());
		mv.visitFieldInsn(Opcodes.PUTFIELD, CONTEXT_RUNNABLE.getInternalName(), "future", SCHEDULED_FUTURE.getDescriptor());
		mv.visitLabel(jump);
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitFieldInsn(Opcodes.GETFIELD, CONTEXT_RUNNABLE.getInternalName(), "action", BI_CONSUMER.getDescriptor());
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitInsn(Opcodes.DUP);
		mv.visitFieldInsn(Opcodes.GETFIELD, CONTEXT_RUNNABLE.getInternalName(), "count", "I");
		mv.visitInsn(Opcodes.DUP_X1);
		mv.visitInsn(Opcodes.ICONST_1);
		mv.visitInsn(Opcodes.IADD);
		mv.visitFieldInsn(Opcodes.PUTFIELD, CONTEXT_RUNNABLE.getInternalName(), "count", "I");
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitFieldInsn(Opcodes.GETFIELD, CONTEXT_RUNNABLE.getInternalName(), "future", SCHEDULED_FUTURE.getDescriptor());
		mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, BI_CONSUMER.getInternalName(), "accept", "(Ljava/lang/Object;Ljava/lang/Object;)V", true);
		mv.visitInsn(Opcodes.RETURN);
		mv.visitLabel(end);
		mv.visitLocalVariable("this", CONTEXT_RUNNABLE.getDescriptor(), null, start, end, 0);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}
}
