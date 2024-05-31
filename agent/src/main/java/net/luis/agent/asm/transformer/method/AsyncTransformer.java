package net.luis.agent.asm.transformer.method;

import net.luis.agent.AgentContext;
import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.ContextBasedClassVisitor;
import net.luis.agent.asm.data.Class;
import net.luis.agent.asm.data.*;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.asm.type.MethodType;
import net.luis.agent.asm.type.TypeModifier;
import net.luis.agent.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.util.HashMap;
import java.util.Map;

import static net.luis.agent.asm.Instrumentations.*;
import static net.luis.agent.asm.Types.*;

/**
 *
 * @author Luis-St
 *
 */

public class AsyncTransformer extends BaseClassTransformer {
	
	public AsyncTransformer() {
		super(true);
	}
	
	//region Type filtering
	@Override
	protected boolean shouldIgnoreClass(@NotNull Type type) {
		Class clazz = AgentContext.get().getClassData(type);
		return clazz.getMethods().values().stream().noneMatch(method -> method.isAnnotatedWith(ASYNC));
	}
	//endregion
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @NotNull ClassWriter writer) {
		return new AsyncClassVisitor(writer, type, () -> this.modified = true);
	}
	
	private static class AsyncClassVisitor extends ContextBasedClassVisitor {
		
		private static final String REPORT_CATEGORY = "Invalid Annotated Element";
		
		private final Map<Method, String> methods = new HashMap<>();
		private final Class data;
		
		private AsyncClassVisitor(@NotNull ClassVisitor visitor, @NotNull Type type, @NotNull Runnable markModified) {
			super(visitor, type, markModified);
			this.data = AgentContext.get().getClassData(type);
		}
		
		@Override
		public @NotNull MethodVisitor visitMethod(int access, @NotNull String name, @NotNull String descriptor, @Nullable String signature, String @Nullable [] exceptions) {
			Method method = this.data.getMethod(name + descriptor);
			if (method == null || method.is(TypeModifier.ABSTRACT) || !method.isAnnotatedWith(ASYNC)) {
				return super.visitMethod(access, name, descriptor, signature, exceptions);
			}
			//region Validation
			if (!method.is(MethodType.METHOD)) {
				throw CrashReport.create("Annotation @Async can not be applied to constructors and static initializers", REPORT_CATEGORY).addDetail("Method", method.getName()).exception();
			}
			if (!method.returns(VOID)) {
				throw CrashReport.create("Method annotated with @Async must return void", REPORT_CATEGORY).addDetail("Method", method.getSourceSignature()).addDetail("Return Type", method.getType().getReturnType()).exception();
			}
			if (method.getExceptionCount() > 0) {
				throw CrashReport.create("Method annotated with @Async must not throw exceptions", REPORT_CATEGORY).addDetail("Method", method.getSourceSignature()).addDetail("Exceptions", method.getExceptions()).exception();
			}
			if (method.isAnnotatedWith(SCHEDULED)) {
				throw CrashReport.create("Method annotated with @Async must not be annotated with @Scheduled", REPORT_CATEGORY).addDetail("Method", method.getName()).exception();
			}
			//endregion
			access = access & ~method.getAccess().getOpcode();
			String newName = "generated$" + Utils.capitalize(name) + "$Async";
			this.methods.put(method, newName);
			MethodVisitor visitor = super.visitMethod(access | Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC, newName, descriptor, signature, exceptions);
			return new MethodVisitor(Opcodes.ASM9, visitor) {
				//region Implementation
				@Override
				public @Nullable AnnotationVisitor visitAnnotation(@NotNull String annotationDescriptor, boolean visible) {
					return null;
				}
				
				@Override
				public @Nullable AnnotationVisitor visitParameterAnnotation(int parameter, @NotNull String annotationDescriptor, boolean visible) {
					return null;
				}
				
				@Override
				public @Nullable AnnotationVisitor visitTypeAnnotation(int typeRef, @Nullable TypePath typePath, @NotNull String annotationDescriptor, boolean visible) {
					return null;
				}
				//endregion
			};
		}
		
		@Override
		public void visitEnd() {
			for (Map.Entry<Method, String> entry : this.methods.entrySet()) {
				Method method = entry.getKey();
				MethodVisitor visitor = super.visitMethod(method.getOpcodes(), method.getName(), method.getType().getDescriptor(), method.getGenericSignature(), null);
				visitor.visitCode();
				instrumentMethodAnnotations(visitor, method);
				instrumentParameterAnnotations(visitor, method);
				//region Parameter loading
				int index = 0;
				if (!method.is(TypeModifier.STATIC)) {
					visitor.visitVarInsn(Opcodes.ALOAD, index++);
				}
				for (Parameter parameter : method.getParameters().values()) {
					visitor.visitVarInsn(parameter.getType().getOpcode(Opcodes.ILOAD), index++);
				}
				//endregion
				visitor.visitInvokeDynamicInsn("run", this.makeDescriptor(method), METAFACTORY_HANDLE, VOID_METHOD, this.createHandle(method, entry.getValue()), VOID_METHOD);
				visitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/concurrent/CompletableFuture", "runAsync", "(Ljava/lang/Runnable;)Ljava/util/concurrent/CompletableFuture;", false);
				visitor.visitInsn(Opcodes.POP);
				visitor.visitInsn(Opcodes.RETURN);
				visitor.visitMaxs(0, 0);
				visitor.visitEnd();
				this.markModified();
			}
		}
		
		//region Helper methods
		private @NotNull String makeDescriptor(@NotNull Method method) {
			StringBuilder builder = new StringBuilder();
			builder.append("(");
			if (!method.is(TypeModifier.STATIC)) {
				builder.append(this.type.getDescriptor());
			}
			for (Parameter parameter : method.getParameters().values()) {
				builder.append(parameter.getType().getDescriptor());
			}
			builder.append(")Ljava/lang/Runnable;");
			return builder.toString();
		}
		
		private @NotNull Handle createHandle(@NotNull Method method, String newName) {
			return new Handle(method.is(TypeModifier.STATIC) ? Opcodes.H_INVOKESTATIC : Opcodes.H_INVOKEVIRTUAL, this.type.getInternalName(), newName, method.getType().getDescriptor(), false);
		}
		//endregion
	}
}
