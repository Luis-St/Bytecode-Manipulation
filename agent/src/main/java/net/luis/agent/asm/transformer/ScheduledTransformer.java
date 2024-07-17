package net.luis.agent.asm.transformer;

import net.luis.agent.Agent;
import net.luis.agent.asm.base.*;
import net.luis.agent.asm.data.Class;
import net.luis.agent.asm.data.*;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.asm.type.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;

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
		return Agent.getClass(type).getMethods().values().stream().noneMatch(method -> method.isAnnotatedWith(SCHEDULED));
	}
	//endregion
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @NotNull ClassWriter writer) {
		return new ScheduledClassVisitor(writer, type, () -> this.modified = true);
	}
	
	private static class ScheduledClassVisitor extends ContextBasedClassVisitor {
		
		private static final String REPORT_CATEGORY = "Invalid Annotated Element";
		
		private final List<Method> staticLookup = new ArrayList<>();
		private final List<Method> instanceLookup = new ArrayList<>();
		private final Scheduler staticScheduler = new Scheduler();
		private final Scheduler instanceScheduler = new Scheduler();
		
		private ScheduledClassVisitor(@NotNull ClassVisitor visitor, @NotNull Type type, @NotNull Runnable markModified) {
			super(visitor, type, markModified);
			Class data = Agent.getClass(type);
			for (Method method : data.getMethods().values()) {
				if (method.isAnnotatedWith(SCHEDULED)) {
					//region Validation
					String signature = method.getSignature(SignatureType.DEBUG);
					if (!method.is(MethodType.METHOD)) {
						throw CrashReport.create("Annotation @Scheduled can not be applied to constructors and static initializers", REPORT_CATEGORY).addDetail("Method", signature).exception();
					}
					if (!method.returns(VOID)) {
						throw CrashReport.create("Method annotated with @Scheduled must return void", REPORT_CATEGORY).addDetail("Method", signature).addDetail("Return Type", method.getReturnType()).exception();
					}
					if (method.getExceptionCount() > 0) {
						throw CrashReport.create("Method annotated with @Scheduled must not throw exceptions", REPORT_CATEGORY).addDetail("Method", signature).addDetail("Exceptions", method.getExceptions()).exception();
					}
					if (method.isAnnotatedWith(ASYNC)) {
						throw CrashReport.create("Method annotated with @Scheduled must not be annotated with @Async", REPORT_CATEGORY).addDetail("Method", signature).exception();
					}
					if (method.getParameterCount() > 0) {
						if (method.getParameterCount() == 1) {
							if (!method.getParameter(0).is(SCHEDULED_FUTURE) && !method.getParameter(0).is(INT)) {
								throw CrashReport.create("Unsupported parameter type for method annotated with @Scheduled, must be 'int' or 'ScheduledFuture<?>'", REPORT_CATEGORY).addDetail("Method", signature)
									.addDetail("Parameter Index", method.getParameter(0).getIndex()).addDetail("Parameter Type", method.getParameter(0).getType()).addDetail("Parameter Name", method.getParameter(0).getName()).exception();
							}
						} else if (method.getParameterCount() == 2) {
							if (!method.getParameter(0).is(INT) || !method.getParameter(1).is(SCHEDULED_FUTURE)) {
								throw CrashReport.create("Unsupported parameter types for method annotated with @Scheduled, must be 'int' and then 'ScheduledFuture<?>'", REPORT_CATEGORY).addDetail("Method", signature)
									.addDetail("Parameter Index", method.getParameter(0).getIndex()).addDetail("Parameter Type", method.getParameter(0).getType()).addDetail("Parameter Name", method.getParameter(0).getName())
									.addDetail("Parameter Index", method.getParameter(1).getIndex()).addDetail("Parameter Type", method.getParameter(1).getType()).addDetail("Parameter Name", method.getParameter(1).getName()).exception();
							}
						} else {
							throw CrashReport.create("Method annotated with @Scheduled must have 0 or 1 parameters", REPORT_CATEGORY).addDetail("Method", signature)
								.addDetail("Parameter Count", method.getParameterCount()).exception();
						}
					}
					//endregion
					if (method.is(TypeModifier.STATIC)) {
						this.staticLookup.add(method);
						this.staticScheduler.setRequired(true);
					} else {
						if (data.is(ClassType.INTERFACE)) {
							throw CrashReport.create("Method annotated with @Scheduled declared as non-static must be in a class, enum or record type", REPORT_CATEGORY).addDetail("Method", signature).exception();
						}
						this.instanceScheduler.setRequired(true);
						this.instanceLookup.add(method);
					}
				}
			}
			for (Field field : data.getFields().values()) {
				if (!field.is(TypeModifier.FINAL)) {
					continue;
				}
				if (field.is(SCHEDULED_EXECUTOR) || field.is(SCHEDULED_EXECUTOR_POOL)) {
					if (this.staticScheduler.isRequired() && field.is(TypeModifier.STATIC)) {
						this.staticScheduler.setScheduler(field);
					} else if (this.instanceScheduler.isRequired() && !field.is(TypeModifier.STATIC)) {
						this.instanceScheduler.setScheduler(field);
					}
				}
			}
		}
		
		@Override
		public void visit(int version, int access, @NotNull String name, @Nullable String signature, @Nullable String superClass, String @Nullable [] interfaces) {
			super.visit(version, access, name, signature, superClass, interfaces);
			if (this.staticScheduler.isRequired() && this.staticScheduler.getScheduler() == null) {
				this.staticScheduler.setGenerated(true);
				this.cv.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, "GENERATED$SCHEDULED_EXECUTOR", SCHEDULED_EXECUTOR.getDescriptor(), null, null).visitEnd();
				Field scheduler = Field.builder(this.type, "GENERATED$SCHEDULED_EXECUTOR", SCHEDULED_EXECUTOR).access(TypeAccess.PRIVATE).addModifier(TypeModifier.STATIC).addModifier(TypeModifier.FINAL).build();
				Agent.getClass(this.type).getFields().put(scheduler.getName(), scheduler);
				this.staticScheduler.setScheduler(scheduler);
			}
			if (this.instanceScheduler.isRequired() && this.instanceScheduler.getScheduler() == null) {
				this.instanceScheduler.setGenerated(true);
				this.cv.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "generated$ScheduledExecutor", SCHEDULED_EXECUTOR.getDescriptor(), null, null).visitEnd();
				Field scheduler = Field.builder(this.type, "generated$ScheduledExecutor", SCHEDULED_EXECUTOR).access(TypeAccess.PRIVATE).addModifier(TypeModifier.FINAL).build();
				Agent.getClass(this.type).getFields().put(scheduler.getName(), scheduler);
				this.instanceScheduler.setScheduler(scheduler);
			}
		}
		
		@Override
		public @NotNull MethodVisitor visitMethod(int access, @NotNull String name, @NotNull String descriptor, @Nullable String signature, String @Nullable [] exceptions) {
			MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
			Method method = Agent.getClass(this.type).getMethod(name + descriptor);
			if (this.staticScheduler.isRequired() && "<clinit>".equals(name) && method != null) {
				this.staticScheduler.setInitialized(true);
				return new ScheduledMethodVisitor(new LocalVariablesSorter(access, descriptor, visitor), this.type, method, this.staticLookup, this.staticScheduler);
			}
			if (this.instanceScheduler.isRequired() && "<init>".equals(name) && method != null && method.is(MethodType.PRIMARY_CONSTRUCTOR)) {
				this.instanceScheduler.setInitialized(true);
				return new ScheduledMethodVisitor(new LocalVariablesSorter(access, descriptor, visitor), this.type, method, this.instanceLookup, this.instanceScheduler);
			}
			return visitor;
		}
		
		@Override
		public void visitEnd() {
			if (this.staticScheduler.isRequired() && !this.staticScheduler.isInitialized()) {
				Method method = Method.builder(this.type, "<clinit>", VOID_METHOD).addModifier(TypeModifier.STATIC).build();
				Agent.getClass(this.type).getMethods().put(method.getSignature(SignatureType.FULL), method);
				MethodVisitor visitor = this.visitMethod(Opcodes.ACC_STATIC, "<clinit>", VOID_METHOD.getDescriptor(), null, null);
				visitor.visitCode();
				visitor.visitInsn(Opcodes.RETURN);
				visitor.visitMaxs(0, 0);
				visitor.visitEnd();
			}
			if (this.instanceScheduler.isRequired() && !this.instanceScheduler.isInitialized()) {
				throw CrashReport.create("No primary constructor found, class was either compiled or modified incorrectly", "Invalid Class File").addDetail("Class", this.type).exception();
			}
			super.visitEnd();
			this.markModified();
		}
	}
	
	private static class ScheduledMethodVisitor extends LabelTrackingMethodVisitor {
		
		private final Type type;
		private final List<Method> lookup;
		private final Field scheduler;
		private final boolean generated;
		private final int readOpcode;
		private final int writeOpcode;
		
		private ScheduledMethodVisitor(@NotNull MethodVisitor visitor, @NotNull Type type, @NotNull Method method, @NotNull List<Method> lookup, @NotNull Scheduler scheduler) {
			super(visitor);
			this.method = method;
			this.type = type;
			this.lookup = lookup;
			this.scheduler = scheduler.getScheduler();
			this.generated = scheduler.isGenerated();
			this.readOpcode = method.is(TypeModifier.STATIC) ? Opcodes.GETSTATIC : Opcodes.GETFIELD;
			this.writeOpcode = method.is(TypeModifier.STATIC) ? Opcodes.PUTSTATIC : Opcodes.PUTFIELD;
		}
		
		@Override
		public void visitCode() {
			super.visitCode();
			if (this.generated && this.method.is(TypeModifier.STATIC)) {
				this.instrumentInitialization();
			}
		}
		
		@Override
		public void visitMethodInsn(int opcode, @NotNull String owner, @NotNull String name, @NotNull String descriptor, boolean isInterface) {
			super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
			if (this.generated && !this.method.is(TypeModifier.STATIC)) {
				Type superType = Agent.getClass(this.type).getSuperType();
				if (superType.getInternalName().equals(owner) && "<init>".equals(name)) {
					this.instrumentInitialization();
				}
			}
		}
		
		@Override
		public void visitFieldInsn(int opcode, @NotNull String owner, @NotNull String name, @NotNull String descriptor) {
			super.visitFieldInsn(opcode, owner, name, descriptor);
			if (!this.generated && opcode == this.writeOpcode && this.scheduler.is(owner, name, descriptor)) {
				this.instrument();
			}
		}
		
		//region Instrumentation
		private void instrumentInitialization() {
			this.instrumentSelfLoad();
			this.mv.visitTypeInsn(Opcodes.NEW, SCHEDULED_EXECUTOR_POOL.getInternalName());
			this.mv.visitInsn(Opcodes.DUP);
			loadNumber(this.mv, Math.min(4, this.lookup.size()));
			this.mv.visitTypeInsn(Opcodes.NEW, DAEMON_THREAD_FACTORY.getInternalName());
			this.mv.visitInsn(Opcodes.DUP);
			this.mv.visitMethodInsn(Opcodes.INVOKESPECIAL, DAEMON_THREAD_FACTORY.getInternalName(), "<init>", "()V", false);
			this.mv.visitMethodInsn(Opcodes.INVOKESPECIAL, SCHEDULED_EXECUTOR_POOL.getInternalName(), "<init>", "(ILjava/util/concurrent/ThreadFactory;)V", false);
			this.mv.visitFieldInsn(this.writeOpcode, this.type.getInternalName(), this.scheduler.getName(), this.scheduler.getType().getDescriptor());
			this.instrument();
		}
		
		private void instrumentSelfLoad() {
			if (!this.method.is(TypeModifier.STATIC)) {
				this.visitVarInsn(Opcodes.ALOAD, 0);
			}
		}
		
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
			for (Method method : this.lookup) {
				if (method.getParameterCount() == 0) {
					this.instrumentRunnable(method);
				} else if (method.getParameterCount() == 1) {
					if (method.getParameter(0).is(INT)) {
						this.instrumentCountingRunnable(method);
					} else if (method.getParameter(0).is(SCHEDULED_FUTURE)) {
						this.instrumentCancelableRunnable(method, index);
					}
				} else if (method.getParameterCount() == 2) {
					this.instrumentContextRunnable(method, index);
				}
			}
			this.insertLabel(end);
			if (index != -1) {
				this.visitLocalVariable(index, "generated$ScheduledTransformer$Temp" + index, CONCURRENT_HASH_MAP, "Ljava/util/Map<Ljava/lang/String;Ljava/util/concurrent/ScheduledFuture<*>;>;", start, end);
			}
		}
		
		private void instrumentRunnable(@NotNull Method method) {
			this.instrumentSelfLoad();
			this.mv.visitFieldInsn(this.readOpcode, this.type.getInternalName(), this.scheduler.getName(), this.scheduler.getType().getDescriptor());
			this.instrumentSelfLoad();
			this.mv.visitInvokeDynamicInsn("run", this.getDynamic(method) + "Ljava/lang/Runnable;", METAFACTORY_HANDLE, VOID_METHOD, this.createHandle(method), VOID_METHOD);
			this.instrumentDefault(method.getAnnotation(SCHEDULED));
			this.mv.visitInsn(Opcodes.POP);
		}
		
		private void instrumentCountingRunnable(@NotNull Method method) {
			this.instrumentSelfLoad();
			this.mv.visitFieldInsn(this.readOpcode, this.type.getInternalName(), this.scheduler.getName(), this.scheduler.getType().getDescriptor());
			this.mv.visitTypeInsn(Opcodes.NEW, COUNTING_RUNNABLE.getInternalName());
			this.mv.visitInsn(Opcodes.DUP);
			this.instrumentSelfLoad();
			this.mv.visitInvokeDynamicInsn("accept", this.getDynamic(method) + "Ljava/util/function/Consumer;", METAFACTORY_HANDLE, Type.getType("(Ljava/lang/Object;)V"), this.createHandle(method), Type.getType("(Ljava/lang/Integer;)V"));
			this.mv.visitMethodInsn(Opcodes.INVOKESPECIAL, COUNTING_RUNNABLE.getInternalName(), "<init>", "(Ljava/util/function/Consumer;)V", false);
			this.instrumentDefault(method.getAnnotation(SCHEDULED));
			this.mv.visitInsn(Opcodes.POP);
		}
		
		private void instrumentCancelableRunnable(@NotNull Method method, int index) {
			this.mv.visitVarInsn(Opcodes.ALOAD, index);
			this.mv.visitLdcInsn(method.getSignature(SignatureType.FULL));
			this.instrumentSelfLoad();
			this.mv.visitFieldInsn(this.readOpcode, this.type.getInternalName(), this.scheduler.getName(), this.scheduler.getType().getDescriptor());
			this.mv.visitTypeInsn(Opcodes.NEW, CANCELABLE_RUNNABLE.getInternalName());
			this.mv.visitInsn(Opcodes.DUP);
			this.mv.visitLdcInsn(method.getSignature(SignatureType.FULL));
			this.mv.visitVarInsn(Opcodes.ALOAD, index);
			this.instrumentSelfLoad();
			this.mv.visitInvokeDynamicInsn("accept", this.getDynamic(method) + "Ljava/util/function/Consumer;", METAFACTORY_HANDLE, Type.getType("(Ljava/lang/Object;)V"), this.createHandle(method), Type.getType("(Ljava/util/concurrent/ScheduledFuture;)V"));
			this.mv.visitMethodInsn(Opcodes.INVOKESPECIAL, CANCELABLE_RUNNABLE.getInternalName(), "<init>", "(Ljava/lang/String;Ljava/util/Map;Ljava/util/function/Consumer;)V", false);
			this.instrumentDefault(method.getAnnotation(SCHEDULED));
			this.mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, MAP.getInternalName(), "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
			this.mv.visitInsn(Opcodes.POP);
		}
		
		private void instrumentContextRunnable(@NotNull Method method, int index) {
			this.mv.visitVarInsn(Opcodes.ALOAD, index);
			this.mv.visitLdcInsn(method.getSignature(SignatureType.FULL));
			this.instrumentSelfLoad();
			this.mv.visitFieldInsn(this.readOpcode, this.type.getInternalName(), this.scheduler.getName(), this.scheduler.getType().getDescriptor());
			this.mv.visitTypeInsn(Opcodes.NEW, CONTEXT_RUNNABLE.getInternalName());
			this.mv.visitInsn(Opcodes.DUP);
			this.mv.visitLdcInsn(method.getSignature(SignatureType.FULL));
			this.mv.visitVarInsn(Opcodes.ALOAD, index);
			this.instrumentSelfLoad();
			this.mv.visitInvokeDynamicInsn("accept", this.getDynamic(method) + "Ljava/util/function/BiConsumer;", METAFACTORY_HANDLE, Type.getType("(Ljava/lang/Object;Ljava/lang/Object;)V"), this.createHandle(method), Type.getType("(Ljava/lang/Integer;Ljava/util/concurrent/ScheduledFuture;)V"));
			this.mv.visitMethodInsn(Opcodes.INVOKESPECIAL, CONTEXT_RUNNABLE.getInternalName(), "<init>", "(Ljava/lang/String;Ljava/util/Map;Ljava/util/function/BiConsumer;)V", false);
			this.instrumentDefault(method.getAnnotation(SCHEDULED));
			this.mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, MAP.getInternalName(), "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
			this.mv.visitInsn(Opcodes.POP);
		}
		
		private void instrumentDefault(@NotNull Annotation annotation) {
			boolean fixedRate = annotation.getOrDefault("fixedRate");
			long initialDelay = annotation.getOrDefault("initialDelay");
			long delay = Objects.requireNonNull(annotation.get("value"));
			
			loadNumber(this.mv, initialDelay);
			loadNumber(this.mv, delay);
			this.mv.visitFieldInsn(Opcodes.GETSTATIC, TIME_UNIT.getInternalName(), annotation.getOrDefault("unit"), TIME_UNIT.getDescriptor());
			this.instrumentScheduleInvoke(fixedRate ? "scheduleAtFixedRate" : "scheduleWithFixedDelay");
		}
		
		private void instrumentScheduleInvoke(String method) {
			boolean iface = this.scheduler.is(SCHEDULED_EXECUTOR);
			this.mv.visitMethodInsn(iface ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, this.scheduler.getType().getInternalName(), method, "(Ljava/lang/Runnable;JJLjava/util/concurrent/TimeUnit;)Ljava/util/concurrent/ScheduledFuture;", iface);
		}
		//endregion
		
		//region Helper methods
		private boolean requiresLookup(@NotNull Method method) {
			int count = method.getParameterCount();
			if (count == 1) {
				return !method.getParameter(0).is(INT);
			}
			return count == 2;
		}
		
		private @NotNull String getDynamic(@NotNull Method method) {
			if (method.is(TypeModifier.STATIC)) {
				return "()";
			}
			return "(" + this.type.getDescriptor() + ")";
		}
		
		private @NotNull Handle createHandle(@NotNull Method method) {
			return new Handle(method.is(TypeModifier.STATIC) ? Opcodes.H_INVOKESTATIC : Opcodes.H_INVOKEVIRTUAL, this.type.getInternalName(), method.getName(), method.getType().getDescriptor(), false);
		}
		//endregion
	}
	
	//region Internal
	private static class Scheduler {
		
		private Field scheduler;
		private boolean required;
		private boolean generated;
		private boolean initialized;
		
		public Field getScheduler() {
			return this.scheduler;
		}
		
		public void setScheduler(@NotNull Field scheduler) {
			this.scheduler = scheduler;
		}
		
		public boolean isRequired() {
			return this.required;
		}
		
		public void setRequired(boolean required) {
			this.required = required;
		}
		
		public boolean isGenerated() {
			return this.generated;
		}
		
		public void setGenerated(boolean generated) {
			this.generated = generated;
		}
		
		public boolean isInitialized() {
			return this.initialized;
		}
		
		public void setInitialized(boolean initialized) {
			this.initialized = initialized;
		}
	}
	//endregion
}
