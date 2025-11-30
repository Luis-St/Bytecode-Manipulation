package net.luis.agent.asm.transformer.method;

import net.luis.agent.Agent;
import net.luis.agent.asm.ASMTreeUtils;
import net.luis.agent.asm.base.*;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.asm.type.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.concurrent.*;

import static net.luis.agent.asm.Instrumentations.*;
import static net.luis.agent.asm.Types.*;

/**
 *
 * @author Luis-St
 *
 */

public class ScheduledTransformer extends BaseClassTransformer {

	private static final Type SCHEDULED_EXECUTOR = Type.getType(ScheduledExecutorService.class);
	private static final Type SCHEDULED_EXECUTOR_POOL = Type.getType(ScheduledThreadPoolExecutor.class);
	private static final Type TIME_UNIT = Type.getType(TimeUnit.class);

	//region Type filtering
	@Override
	protected boolean shouldIgnoreClass(@NotNull Type type) {
		ClassNode classNode = Agent.getClass(type);
		if (classNode == null) {
			return true;
		}
		for (MethodNode method : classNode.methods) {
			if (ASMTreeUtils.hasAnnotation(method, SCHEDULED)) {
				return false;
			}
		}
		return true;
	}
	//endregion

	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @NotNull ClassWriter writer) {
		return new ScheduledClassVisitor(writer, type, () -> this.modified = true);
	}

	private static class ScheduledClassVisitor extends ContextBasedClassVisitor {

		private static final String REPORT_CATEGORY = "Invalid Annotated Element";

		private final List<MethodInfo> lookup = new ArrayList<>();
		private FieldInfo executor;
		private boolean generated;
		private boolean initialized;

		private ScheduledClassVisitor(@NotNull ClassVisitor visitor, @NotNull Type type, @NotNull Runnable markModified) {
			super(visitor, type, markModified);
			ClassNode classNode = Agent.getClass(type);
			if (classNode == null) {
				return;
			}

			// Collect methods annotated with @Scheduled
			for (MethodNode method : classNode.methods) {
				if (ASMTreeUtils.hasAnnotation(method, SCHEDULED)) {
					validateScheduledMethod(method);
					this.lookup.add(new MethodInfo(method));
				}
			}

			// Find existing executor field
			for (FieldNode field : classNode.fields) {
				if (ASMTreeUtils.is(field, TypeModifier.STATIC) && ASMTreeUtils.is(field, TypeModifier.FINAL)) {
					Type fieldType = ASMTreeUtils.getType(field);
					if (fieldType.equals(SCHEDULED_EXECUTOR) || fieldType.equals(SCHEDULED_EXECUTOR_POOL)) {
						this.executor = new FieldInfo(field.name, fieldType);
					}
				}
			}
		}

		private void validateScheduledMethod(@NotNull MethodNode method) {
			String signature = ASMTreeUtils.getDebugSignature(this.type, method);

			if (!ASMTreeUtils.is(method, MethodType.METHOD)) {
				throw CrashReport.create("Annotation @Scheduled can not be applied to constructors and static initializers", REPORT_CATEGORY)
					.addDetail("Method", signature).exception();
			}
			if (!ASMTreeUtils.is(method, TypeModifier.STATIC)) {
				throw CrashReport.create("Method annotated with @Scheduled must be static", REPORT_CATEGORY)
					.addDetail("Method", signature).exception();
			}
			if (!ASMTreeUtils.returns(method, VOID)) {
				throw CrashReport.create("Method annotated with @Scheduled must return void", REPORT_CATEGORY)
					.addDetail("Method", signature).addDetail("Return Type", ASMTreeUtils.getReturnType(method)).exception();
			}
			if (ASMTreeUtils.getExceptionCount(method) > 0) {
				throw CrashReport.create("Method annotated with @Scheduled must not throw exceptions", REPORT_CATEGORY)
					.addDetail("Method", signature).addDetail("Exceptions", ASMTreeUtils.getExceptions(method)).exception();
			}
			if (ASMTreeUtils.hasAnnotation(method, ASYNC)) {
				throw CrashReport.create("Method annotated with @Scheduled must not be annotated with @Async", REPORT_CATEGORY)
					.addDetail("Method", signature).exception();
			}

			int paramCount = ASMTreeUtils.getParameterCount(method);
			if (paramCount > 0) {
				Type[] paramTypes = ASMTreeUtils.getParameterTypes(method);
				if (paramCount == 1) {
					if (!paramTypes[0].equals(SCHEDULED_FUTURE) && !paramTypes[0].equals(INT)) {
						throw CrashReport.create("Unsupported parameter type for method annotated with @Scheduled, must be ScheduledFuture<?> or int", REPORT_CATEGORY)
							.addDetail("Method", signature)
							.addDetail("Parameter Index", 0)
							.addDetail("Parameter Type", paramTypes[0])
							.addDetail("Parameter Name", getParameterName(method, 0)).exception();
					}
				} else if (paramCount == 2) {
					if (!paramTypes[0].equals(INT) || !paramTypes[1].equals(SCHEDULED_FUTURE)) {
						throw CrashReport.create("Unsupported parameter types for method annotated with @Scheduled, must be int and ScheduledFuture<?>", REPORT_CATEGORY)
							.addDetail("Method", signature)
							.addDetail("Parameter Index", 0)
							.addDetail("Parameter Type", paramTypes[0])
							.addDetail("Parameter Name", getParameterName(method, 0))
							.addDetail("Parameter Index", 1)
							.addDetail("Parameter Type", paramTypes[1])
							.addDetail("Parameter Name", getParameterName(method, 1)).exception();
					}
				} else {
					throw CrashReport.create("Method annotated with @Scheduled must have 0 or 1 parameters", REPORT_CATEGORY)
						.addDetail("Method", signature)
						.addDetail("Parameter Count", paramCount).exception();
				}
			}
		}

		private static @NotNull String getParameterName(@NotNull MethodNode method, int paramIndex) {
			if (method.parameters != null && paramIndex < method.parameters.size()) {
				ParameterNode param = method.parameters.get(paramIndex);
				if (param.name != null) {
					return param.name;
				}
			}
			return "arg" + paramIndex;
		}

		@Override
		public void visit(int version, int access, @NotNull String name, @Nullable String signature, @Nullable String superClass, String @Nullable [] interfaces) {
			super.visit(version, access, name, signature, superClass, interfaces);
			if (this.executor == null) {
				this.cv.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, "GENERATED$SCHEDULED_EXECUTOR", SCHEDULED_EXECUTOR.getDescriptor(), null, null).visitEnd();
				this.generated = true;
				this.executor = new FieldInfo("GENERATED$SCHEDULED_EXECUTOR", SCHEDULED_EXECUTOR);
			}
		}

		@Override
		public @NotNull MethodVisitor visitMethod(int access, @NotNull String name, @NotNull String descriptor, @Nullable String signature, String @Nullable [] exceptions) {
			MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
			if ("<clinit>".equals(name)) {
				this.initialized = true;
				return new ScheduledMethodVisitor(new LocalVariablesSorter(access, descriptor, visitor), this.type, this.lookup, this.executor, this.generated);
			}
			return visitor;
		}

		@Override
		public void visitEnd() {
			if (!this.initialized) {
				MethodVisitor visitor = this.visitMethod(Opcodes.ACC_STATIC, "<clinit>", VOID_METHOD.getDescriptor(), null, null);
				visitor.visitCode();
				visitor.visitInsn(Opcodes.RETURN);
				visitor.visitMaxs(0, 0);
				visitor.visitEnd();
			}
			super.visitEnd();
			this.markModified();
		}
	}

	private static class ScheduledMethodVisitor extends LabelTrackingMethodVisitor {

		private final Type type;
		private final List<MethodInfo> lookup;
		private final FieldInfo executor;
		private final boolean generated;

		private ScheduledMethodVisitor(@NotNull MethodVisitor visitor, @NotNull Type type, @NotNull List<MethodInfo> lookup, @NotNull FieldInfo executor, boolean generated) {
			super(visitor);
			this.type = type;
			this.lookup = lookup;
			this.executor = executor;
			this.generated = generated;
		}

		@Override
		public void visitCode() {
			super.visitCode();
			if (this.generated) {
				this.mv.visitTypeInsn(Opcodes.NEW, SCHEDULED_EXECUTOR_POOL.getInternalName());
				this.mv.visitInsn(Opcodes.DUP);
				loadNumber(this.mv, Math.min(4, this.lookup.size()));
				this.mv.visitTypeInsn(Opcodes.NEW, DAEMON_THREAD_FACTORY.getInternalName());
				this.mv.visitInsn(Opcodes.DUP);
				this.mv.visitMethodInsn(Opcodes.INVOKESPECIAL, DAEMON_THREAD_FACTORY.getInternalName(), "<init>", "()V", false);
				this.mv.visitMethodInsn(Opcodes.INVOKESPECIAL, SCHEDULED_EXECUTOR_POOL.getInternalName(), "<init>", "(ILjava/util/concurrent/ThreadFactory;)V", false);
				this.mv.visitFieldInsn(Opcodes.PUTSTATIC, this.type.getInternalName(), this.executor.name, this.executor.type.getDescriptor());
				this.instrument();
			}
		}

		@Override
		public void visitFieldInsn(int opcode, @NotNull String owner, @NotNull String name, @NotNull String descriptor) {
			super.visitFieldInsn(opcode, owner, name, descriptor);
			if (!this.generated && opcode == Opcodes.PUTSTATIC && this.executor.matches(owner, name, descriptor)) {
				this.instrument();
			}
		}

		//region Instrumentation
		private void instrument() {
			int index = -1;
			Label start = new Label();
			Label end = new Label();

			if (this.lookup.stream().anyMatch(this::requiresLookup)) {
				index = newLocal(this.mv, CONCURRENT_HASH_MAP);
				this.mv.visitTypeInsn(Opcodes.NEW, CONCURRENT_HASH_MAP.getInternalName());
				this.mv.visitInsn(Opcodes.DUP);
				this.mv.visitMethodInsn(Opcodes.INVOKESPECIAL, CONCURRENT_HASH_MAP.getInternalName(), "<init>", "()V", false);
				this.mv.visitVarInsn(Opcodes.ASTORE, index);
			}
			this.insertLabel(start);
			for (MethodInfo method : this.lookup) {
				if (method.parameterCount == 0) {
					this.instrumentRunnable(method);
				} else if (method.parameterCount == 1) {
					if (method.parameterTypes[0].equals(INT)) {
						this.instrumentCountingRunnable(method);
					} else if (method.parameterTypes[0].equals(SCHEDULED_FUTURE)) {
						this.instrumentCancelableRunnable(method, index);
					}
				} else if (method.parameterCount == 2) {
					this.instrumentContextRunnable(method, index);
				}
			}
			this.insertLabel(end);
			if (index != -1) {
				this.visitLocalVariable(index, "generated$ScheduledTransformer$Temp" + index, CONCURRENT_HASH_MAP, "Ljava/util/Map<Ljava/lang/String;Ljava/util/concurrent/ScheduledFuture<*>;>;", start, end);
			}
		}

		private void instrumentRunnable(@NotNull MethodInfo method) {
			this.mv.visitFieldInsn(Opcodes.GETSTATIC, this.type.getInternalName(), this.executor.name, this.executor.type.getDescriptor());
			this.mv.visitInvokeDynamicInsn("run", "()Ljava/lang/Runnable;", METAFACTORY_HANDLE, VOID_METHOD, this.createHandle(method), VOID_METHOD);
			this.instrumentDefault(method.annotation);
			this.mv.visitInsn(Opcodes.POP);
		}

		private void instrumentCountingRunnable(@NotNull MethodInfo method) {
			this.mv.visitFieldInsn(Opcodes.GETSTATIC, this.type.getInternalName(), this.executor.name, this.executor.type.getDescriptor());
			this.mv.visitTypeInsn(Opcodes.NEW, COUNTING_RUNNABLE.getInternalName());
			this.mv.visitInsn(Opcodes.DUP);
			this.mv.visitInvokeDynamicInsn("accept", "()Ljava/util/function/Consumer;", METAFACTORY_HANDLE, Type.getType("(Ljava/lang/Object;)V"), this.createHandle(method), Type.getType("(Ljava/lang/Integer;)V"));
			this.mv.visitMethodInsn(Opcodes.INVOKESPECIAL, COUNTING_RUNNABLE.getInternalName(), "<init>", "(Ljava/util/function/Consumer;)V", false);
			this.instrumentDefault(method.annotation);
			this.mv.visitInsn(Opcodes.POP);
		}

		private void instrumentCancelableRunnable(@NotNull MethodInfo method, int index) {
			this.mv.visitVarInsn(Opcodes.ALOAD, index);
			this.mv.visitLdcInsn(method.fullSignature);
			this.mv.visitFieldInsn(Opcodes.GETSTATIC, this.type.getInternalName(), this.executor.name, this.executor.type.getDescriptor());
			this.mv.visitTypeInsn(Opcodes.NEW, CANCELABLE_RUNNABLE.getInternalName());
			this.mv.visitInsn(Opcodes.DUP);
			this.mv.visitLdcInsn(method.fullSignature);
			this.mv.visitVarInsn(Opcodes.ALOAD, index);
			this.mv.visitInvokeDynamicInsn("accept", "()Ljava/util/function/Consumer;", METAFACTORY_HANDLE, Type.getType("(Ljava/lang/Object;)V"), this.createHandle(method), Type.getType("(Ljava/util/concurrent/ScheduledFuture;)V"));
			this.mv.visitMethodInsn(Opcodes.INVOKESPECIAL, CANCELABLE_RUNNABLE.getInternalName(), "<init>", "(Ljava/lang/String;Ljava/util/Map;Ljava/util/function/Consumer;)V", false);
			this.instrumentDefault(method.annotation);
			this.mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, MAP.getInternalName(), "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
			this.mv.visitInsn(Opcodes.POP);
		}

		private void instrumentContextRunnable(@NotNull MethodInfo method, int index) {
			this.mv.visitVarInsn(Opcodes.ALOAD, index);
			this.mv.visitLdcInsn(method.fullSignature);
			this.mv.visitFieldInsn(Opcodes.GETSTATIC, this.type.getInternalName(), this.executor.name, this.executor.type.getDescriptor());
			this.mv.visitTypeInsn(Opcodes.NEW, CONTEXT_RUNNABLE.getInternalName());
			this.mv.visitInsn(Opcodes.DUP);
			this.mv.visitLdcInsn(method.fullSignature);
			this.mv.visitVarInsn(Opcodes.ALOAD, index);
			this.mv.visitInvokeDynamicInsn("accept", "()Ljava/util/function/BiConsumer;", METAFACTORY_HANDLE, Type.getType("(Ljava/lang/Object;Ljava/lang/Object;)V"), this.createHandle(method), Type.getType("(Ljava/lang/Integer;Ljava/util/concurrent/ScheduledFuture;)V"));
			this.mv.visitMethodInsn(Opcodes.INVOKESPECIAL, CONTEXT_RUNNABLE.getInternalName(), "<init>", "(Ljava/lang/String;Ljava/util/Map;Ljava/util/function/BiConsumer;)V", false);
			this.instrumentDefault(method.annotation);
			this.mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, MAP.getInternalName(), "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
			this.mv.visitInsn(Opcodes.POP);
		}

		private void instrumentDefault(@NotNull AnnotationNode annotation) {
			Boolean fixedRate = ASMTreeUtils.getAnnotationValue(annotation, "fixedRate", false);
			Long initialDelay = ASMTreeUtils.getAnnotationValue(annotation, "initialDelay", 0L);
			Long delay = (Long) ASMTreeUtils.getAnnotationValue(annotation, "value");
			if (delay == null) {
				throw new IllegalStateException("@Scheduled annotation must have a 'value' attribute");
			}

			loadNumber(this.mv, initialDelay);
			loadNumber(this.mv, delay);
			// Enum values are stored as String arrays [descriptor, value]
			Object unitValue = ASMTreeUtils.getAnnotationValue(annotation, "unit");
			String unit;
			if (unitValue instanceof String[]) {
				String[] unitEnum = (String[]) unitValue;
				unit = (unitEnum.length >= 2) ? unitEnum[1] : "MILLISECONDS";
			} else {
				unit = "MILLISECONDS";
			}
			this.mv.visitFieldInsn(Opcodes.GETSTATIC, TIME_UNIT.getInternalName(), unit, TIME_UNIT.getDescriptor());
			this.instrumentScheduleInvoke(fixedRate ? "scheduleAtFixedRate" : "scheduleWithFixedDelay");
		}

		private void instrumentScheduleInvoke(String method) {
			boolean iface = this.executor.type.equals(SCHEDULED_EXECUTOR);
			this.mv.visitMethodInsn(iface ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, this.executor.type.getInternalName(), method, "(Ljava/lang/Runnable;JJLjava/util/concurrent/TimeUnit;)Ljava/util/concurrent/ScheduledFuture;", iface);
		}
		//endregion

		//region Helper methods
		private boolean requiresLookup(@NotNull MethodInfo method) {
			int count = method.parameterCount;
			if (count == 1) {
				return !method.parameterTypes[0].equals(INT);
			}
			return count == 2;
		}

		private @NotNull Handle createHandle(@NotNull MethodInfo method) {
			return new Handle(Opcodes.H_INVOKESTATIC, this.type.getInternalName(), method.name, method.descriptor, false);
		}
		//endregion
	}

	private static class MethodInfo {
		private final String name;
		private final String descriptor;
		private final String fullSignature;
		private final int parameterCount;
		private final Type[] parameterTypes;
		private final AnnotationNode annotation;

		private MethodInfo(@NotNull MethodNode methodNode) {
			this.name = methodNode.name;
			this.descriptor = methodNode.desc;
			this.fullSignature = methodNode.name + methodNode.desc;
			this.parameterCount = ASMTreeUtils.getParameterCount(methodNode);
			this.parameterTypes = ASMTreeUtils.getParameterTypes(methodNode);
			this.annotation = ASMTreeUtils.getAnnotation(methodNode, SCHEDULED);
		}
	}

	private static class FieldInfo {
		private final String name;
		private final Type type;

		private FieldInfo(@NotNull String name, @NotNull Type type) {
			this.name = name;
			this.type = type;
		}

		private boolean matches(@NotNull String owner, @NotNull String name, @NotNull String descriptor) {
			return this.name.equals(name) && this.type.getDescriptor().equals(descriptor);
		}
	}
}
