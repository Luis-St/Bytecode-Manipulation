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

public class CancelableRunnableGenerator extends Generator {
	
	private static final String LOOKUP_SIGNATURE = "Ljava/util/Map<Ljava/lang/String;Ljava/util/concurrent/ScheduledFuture<*>;>;";
	private static final String ACTION_SIGNATURE = "Ljava/util/function/Consumer<Ljava/util/concurrent/ScheduledFuture<*>;>;";
	private static final String FUTURE_SIGNATURE = "Ljava/util/concurrent/ScheduledFuture<*>;";
	
	private static final String CONSTRUCTOR_SIGNATURE = "(Ljava/lang/String;Ljava/util/Map<Ljava/lang/String;Ljava/util/concurrent/ScheduledFuture<*>;>;Ljava/util/function/Consumer<Ljava/util/concurrent/ScheduledFuture<*>;>;)V";
	
	public CancelableRunnableGenerator() {
		super(CANCELABLE_RUNNABLE.getClassName());
	}
	
	@Override
	public void generate(@NotNull ClassVisitor cv) {
		cv.visit(CLASS_VERSION, Opcodes.ACC_PUBLIC, CANCELABLE_RUNNABLE.getInternalName(), null, "java/lang/Object", new String[] { RUNNABLE.getInternalName() });
		cv.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "method", STRING.getDescriptor(), null, null).visitEnd();
		cv.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "lookup", MAP.getDescriptor(), LOOKUP_SIGNATURE, null).visitEnd();
		cv.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "action", CONSUMER.getDescriptor(), ACTION_SIGNATURE, null).visitEnd();
		cv.visitField(Opcodes.ACC_PRIVATE, "future", SCHEDULED_FUTURE.getDescriptor(), FUTURE_SIGNATURE, null).visitEnd();
		this.generateConstructor(cv);
		this.generateRun(cv);
		cv.visitEnd();
	}
	
	private void generateConstructor(@NotNull ClassVisitor cv) {
		MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(Ljava/lang/String;Ljava/util/Map;Ljava/util/function/Consumer;)V", CONSTRUCTOR_SIGNATURE, null);
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
		mv.visitFieldInsn(Opcodes.PUTFIELD, CANCELABLE_RUNNABLE.getInternalName(), "method", STRING.getDescriptor());
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitVarInsn(Opcodes.ALOAD, 2);
		mv.visitFieldInsn(Opcodes.PUTFIELD, CANCELABLE_RUNNABLE.getInternalName(), "lookup", MAP.getDescriptor());
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitVarInsn(Opcodes.ALOAD, 3);
		mv.visitFieldInsn(Opcodes.PUTFIELD, CANCELABLE_RUNNABLE.getInternalName(), "action", CONSUMER.getDescriptor());
		mv.visitInsn(Opcodes.RETURN);
		mv.visitLabel(end);
		mv.visitLocalVariable("this", CANCELABLE_RUNNABLE.getDescriptor(), null, start, end, 0);
		mv.visitLocalVariable("method", STRING.getDescriptor(), null, start, end, 1);
		mv.visitLocalVariable("lookup", MAP.getDescriptor(), LOOKUP_SIGNATURE, start, end, 2);
		mv.visitLocalVariable("action", CONSUMER.getDescriptor(), ACTION_SIGNATURE, start, end, 3);
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
		mv.visitFieldInsn(Opcodes.GETFIELD, CANCELABLE_RUNNABLE.getInternalName(), "future", SCHEDULED_FUTURE.getDescriptor());
		mv.visitJumpInsn(Opcodes.IFNONNULL, jump);
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitFieldInsn(Opcodes.GETFIELD, CANCELABLE_RUNNABLE.getInternalName(), "lookup", MAP.getDescriptor());
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitFieldInsn(Opcodes.GETFIELD, CANCELABLE_RUNNABLE.getInternalName(), "method", STRING.getDescriptor());
		mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, MAP.getInternalName(), "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
		mv.visitTypeInsn(Opcodes.CHECKCAST, SCHEDULED_FUTURE.getInternalName());
		mv.visitFieldInsn(Opcodes.PUTFIELD, CANCELABLE_RUNNABLE.getInternalName(), "future", SCHEDULED_FUTURE.getDescriptor());
		mv.visitLabel(jump);
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitFieldInsn(Opcodes.GETFIELD, CANCELABLE_RUNNABLE.getInternalName(), "action", CONSUMER.getDescriptor());
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitFieldInsn(Opcodes.GETFIELD, CANCELABLE_RUNNABLE.getInternalName(), "future", SCHEDULED_FUTURE.getDescriptor());
		mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, CONSUMER.getInternalName(), "accept", "(Ljava/lang/Object;)V", true);
		mv.visitInsn(Opcodes.RETURN);
		mv.visitLabel(end);
		mv.visitLocalVariable("this", CANCELABLE_RUNNABLE.getDescriptor(), null, start, end, 0);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}
}
