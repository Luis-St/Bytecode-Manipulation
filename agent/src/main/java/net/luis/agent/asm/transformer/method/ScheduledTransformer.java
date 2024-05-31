package net.luis.agent.asm.transformer.method;

import net.luis.agent.AgentContext;
import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.ContextBasedClassVisitor;
import net.luis.agent.asm.data.Class;
import net.luis.agent.asm.data.*;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.asm.type.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

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
		Class clazz = AgentContext.get().getClass(type);
		return clazz.getMethods().values().stream().noneMatch(method -> method.isAnnotatedWith(SCHEDULED));
	}
	//endregion
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @NotNull ClassWriter writer) {
		return new ScheduledClassVisitor(writer, type, () -> this.modified = true);
	}
	
	private static class ScheduledClassVisitor extends ContextBasedClassVisitor {
		
		private static final String REPORT_CATEGORY = "Invalid Annotated Element";
		
		private final List<Method> lookup = new ArrayList<>();
		private Field executor;
		private boolean generated;
		private boolean initialized;
		
		private ScheduledClassVisitor(@NotNull ClassVisitor visitor, @NotNull Type type, @NotNull Runnable markModified) {
			super(visitor, type, markModified);
			Class data = AgentContext.get().getClass(type);
			for (Method method : data.getMethods().values()) {
				if (method.isAnnotatedWith(SCHEDULED)) {
					//region Validation
					if (!method.is(MethodType.METHOD)) {
						throw CrashReport.create("Annotation @Scheduled can not be applied to constructors and static initializers", REPORT_CATEGORY).addDetail("Method", method.getName()).exception();
					}
					if (!method.is(TypeModifier.STATIC)) {
						throw CrashReport.create("Method annotated with @Scheduled must be static", REPORT_CATEGORY).addDetail("Method", method.getSourceSignature()).exception();
					}
					if (!method.returns(VOID)) {
						throw CrashReport.create("Method annotated with @Scheduled must return void", REPORT_CATEGORY).addDetail("Method", method.getSourceSignature()).addDetail("Return Type", method.getReturnType()).exception();
					}
					if (method.getParameterCount() > 0) {
						throw CrashReport.create("Method annotated with @Scheduled must not have parameters", REPORT_CATEGORY).addDetail("Method", method.getSourceSignature()).exception();
					}
					if (method.getExceptionCount() > 0) {
						throw CrashReport.create("Method annotated with @Scheduled must not throw exceptions", REPORT_CATEGORY).addDetail("Method", method.getSourceSignature()).addDetail("Exceptions", method.getExceptions()).exception();
					}
					if (method.isAnnotatedWith(ASYNC)) {
						throw CrashReport.create("Method annotated with @Scheduled must not be annotated with @Async", REPORT_CATEGORY).addDetail("Method", method.getName()).exception();
					}
					//endregion
					this.lookup.add(method);
				}
			}
			for (Field field : data.getFields().values()) {
				if (field.is(TypeModifier.STATIC) && field.is(TypeModifier.FINAL)) {
					if (field.is(SCHEDULED_EXECUTOR) || field.is(SCHEDULED_EXECUTOR_POOL)) {
						this.executor = field;
					}
				}
			}
		}
		
		@Override
		public void visit(int version, int access, @NotNull String name, @Nullable String signature, @Nullable String superClass, String @Nullable [] interfaces) {
			super.visit(version, access, name, signature, superClass, interfaces);
			if (this.executor == null) {
				this.cv.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, "GENERATED$SCHEDULED_EXECUTOR", SCHEDULED_EXECUTOR.getDescriptor(), null, null).visitEnd();
				this.generated = true;
				this.executor = Field.builder(this.type, "GENERATED$SCHEDULED_EXECUTOR", SCHEDULED_EXECUTOR).access(TypeAccess.PRIVATE).addModifier(TypeModifier.STATIC).addModifier(TypeModifier.FINAL).build();
				AgentContext.get().getClass(this.type).getFields().put(this.executor.getName(), this.executor);
			}
		}
		
		@Override
		public @NotNull MethodVisitor visitMethod(int access, @NotNull String name, @NotNull String descriptor, @Nullable String signature, String @Nullable [] exceptions) {
			MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
			if ("<clinit>".equals(name)) {
				this.initialized = true;
				return new ScheduledMethodVisitor(visitor, this.type, this.lookup, this.executor, this.generated);
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
				Method method = Method.builder(this.type, "<clinit>", VOID_METHOD).addModifier(TypeModifier.STATIC).build();
				AgentContext.get().getClass(this.type).getMethods().put(method.getFullSignature(), method);
			}
			super.visitEnd();
			this.markModified();
		}
	}
	
	private static class ScheduledMethodVisitor extends MethodVisitor {
		
		private final Type type;
		private final List<Method> lookup;
		private final Field executor;
		private final boolean generated;
		
		private ScheduledMethodVisitor(@NotNull MethodVisitor visitor, @NotNull Type type, @NotNull List<Method> lookup, @NotNull Field executor, boolean generated) {
			super(Opcodes.ASM9, visitor);
			this.type = type;
			this.lookup = lookup;
			this.executor = executor;
			this.generated = generated;
		}
		
		@Override
		public void visitInsn(int opcode) {
			if (opcode == Opcodes.RETURN) {
				return;
			}
			super.visitInsn(opcode);
		}
		
		@Override
		public void visitEnd() {
			if (this.generated) {
				this.mv.visitTypeInsn(Opcodes.NEW, SCHEDULED_EXECUTOR_POOL.getInternalName());
				this.mv.visitInsn(Opcodes.DUP);
				loadNumber(this.mv, Math.min(4, this.lookup.size()));
				this.mv.visitMethodInsn(Opcodes.INVOKESPECIAL, SCHEDULED_EXECUTOR_POOL.getInternalName(), "<init>", "(I)V", false);
				this.mv.visitFieldInsn(Opcodes.PUTSTATIC, this.type.getInternalName(), this.executor.getName(), this.executor.getType().getDescriptor());
			}
			for (Method method : this.lookup) {
				Annotation annotation = method.getAnnotation(SCHEDULED);
				long initialDelay = annotation.getOrDefault("initialDelay");
				long delay = Objects.requireNonNull(annotation.get("value"));
				String unit = annotation.getOrDefault("unit");
				boolean fixedRate = annotation.getOrDefault("fixedRate");
				
				this.mv.visitFieldInsn(Opcodes.GETSTATIC, this.type.getInternalName(), this.executor.getName(), this.executor.getType().getDescriptor());
				this.mv.visitInvokeDynamicInsn("run", "()Ljava/lang/Runnable;", METAFACTORY_HANDLE, VOID_METHOD, this.createHandle(method), VOID_METHOD);
				loadNumber(this.mv, initialDelay);
				loadNumber(this.mv, delay);
				this.mv.visitFieldInsn(Opcodes.GETSTATIC, TIME_UNIT.getInternalName(), unit, TIME_UNIT.getDescriptor());
				this.instrumentScheduleInvoke(fixedRate ? "scheduleAtFixedRate" : "scheduleWithFixedDelay");
				this.mv.visitInsn(Opcodes.POP);
			}
			this.mv.visitInsn(Opcodes.RETURN);
			super.visitEnd();
		}
		
		private @NotNull Handle createHandle(@NotNull Method method) {
			return new Handle(Opcodes.H_INVOKESTATIC, this.type.getInternalName(), method.getName(), method.getType().getDescriptor(), false);
		}
		
		private void instrumentScheduleInvoke(String method) {
			boolean iface = this.executor.is(SCHEDULED_EXECUTOR);
			this.mv.visitMethodInsn(iface ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, this.executor.getType().getInternalName(), method, "(Ljava/lang/Runnable;JJLjava/util/concurrent/TimeUnit;)Ljava/util/concurrent/ScheduledFuture;", iface);
		}
	}
}
