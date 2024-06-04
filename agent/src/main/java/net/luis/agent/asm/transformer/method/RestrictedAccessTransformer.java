package net.luis.agent.asm.transformer.method;

import net.luis.agent.Agent;
import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.ContextBasedClassVisitor;
import net.luis.agent.asm.data.Annotation;
import net.luis.agent.asm.data.Method;
import net.luis.agent.asm.type.TypeModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.util.List;
import java.util.Objects;

import static net.luis.agent.asm.Instrumentations.*;
import static net.luis.agent.asm.Types.*;

/**
 *
 * @author Luis-St
 *
 */

public class RestrictedAccessTransformer extends BaseClassTransformer {
	
	public RestrictedAccessTransformer() {
		super(true);
	}
	
	//region Type filtering
	@Override
	protected boolean shouldIgnoreClass(@NotNull Type type) {
		return Agent.getClass(type).getMethods().values().stream().noneMatch(method -> method.isAnnotatedWith(RESTRICTED_ACCESS));
	}
	//endregion
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @NotNull ClassWriter writer) {
		return new RestrictedAccessClassVisitor(writer, type, () -> this.modified = true);
	}
	
	private static class RestrictedAccessClassVisitor extends ContextBasedClassVisitor {
		
		private RestrictedAccessClassVisitor(@NotNull ClassVisitor visitor, @NotNull Type type, @NotNull Runnable markModified) {
			super(visitor, type, markModified);
		}
		
		@Override
		public @NotNull MethodVisitor visitMethod(int access, @NotNull String name, @NotNull String descriptor, @Nullable String signature, String @Nullable [] exceptions) {
			Method method = Agent.getClass(this.type).getMethod(name + descriptor);
			MethodVisitor visitor = this.cv.visitMethod(access, name, descriptor, signature, exceptions);
			if (method == null || method.is(TypeModifier.ABSTRACT) || !method.isAnnotatedWith(RESTRICTED_ACCESS)) {
				return visitor;
			}
			
			return new RestrictedAccessMethodVisitor(new LocalVariablesSorter(access, descriptor, visitor), method);
		}
		
		@Override
		public void visitEnd() {
			this.markModified();
			this.cv.visitEnd();
		}
	}
	
	private static class RestrictedAccessMethodVisitor extends MethodVisitor {
		
		private static final Type STACK_TRACE_ARRAY = Type.getType("[Ljava/lang/StackTraceElement;");
		private static final Type RUNTIME_EXCEPTION = Type.getType("Ljava/lang/RuntimeException;");
		
		private final Type type;
		private final Method method;
		private final List<String> values;
		private final boolean pattern;
		
		private RestrictedAccessMethodVisitor(@NotNull MethodVisitor visitor, @NotNull Method method) {
			super(Opcodes.ASM9, visitor);
			this.type = method.getOwner();
			this.method = method;
			Annotation annotation = method.getAnnotation(RESTRICTED_ACCESS);
			this.values = Objects.requireNonNull(annotation.get("value"));
			this.pattern = Boolean.TRUE.equals(annotation.get("pattern"));
		}
		
		@Override
		public void visitCode() {
			this.mv.visitCode();
			Label start = new Label();
			Label clazzVariable = new Label();
			Label methodVariable = new Label();
			Label end = new Label();
			
			int array = newLocal(this.mv, STACK_TRACE_ARRAY);
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
			this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Thread", "getStackTrace", "()[Ljava/lang/StackTraceElement;", false);
			this.mv.visitLabel(start);
			this.mv.visitVarInsn(Opcodes.ASTORE, array);
			
			this.mv.visitVarInsn(Opcodes.ALOAD, array);
			this.mv.visitInsn(Opcodes.ARRAYLENGTH);
			this.mv.visitInsn(Opcodes.ICONST_2);
			this.mv.visitJumpInsn(Opcodes.IF_ICMPLE, end);
			
			if (this.values.isEmpty()) {
				instrumentThrownException(this.mv, RUNTIME_EXCEPTION, this.getMessage());
				this.mv.visitLabel(end);
				return;
			}
			
			int clazz = newLocal(this.mv, STRING);
			this.mv.visitVarInsn(Opcodes.ALOAD, array);
			this.mv.visitInsn(Opcodes.ICONST_2);
			this.mv.visitInsn(Opcodes.AALOAD);
			this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StackTraceElement", "getClassName", "()Ljava/lang/String;", false);
			this.mv.visitLabel(clazzVariable);
			this.mv.visitVarInsn(Opcodes.ASTORE, clazz);
			
			int method = newLocal(this.mv, STRING);
			this.mv.visitVarInsn(Opcodes.ALOAD, array);
			this.mv.visitInsn(Opcodes.ICONST_2);
			this.mv.visitInsn(Opcodes.AALOAD);
			this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StackTraceElement", "getMethodName", "()Ljava/lang/String;", false);
			this.mv.visitLabel(methodVariable);
			this.mv.visitVarInsn(Opcodes.ASTORE, method);
			
			for (String value : this.values) {
				this.mv.visitLdcInsn(value);
				this.mv.visitInsn(this.pattern ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
				this.mv.visitVarInsn(Opcodes.ALOAD, clazz);
				this.mv.visitVarInsn(Opcodes.ALOAD, method);
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, RUNTIME_UTILS.getInternalName(), "isAccessAllowed", "(Ljava/lang/String;ZLjava/lang/String;Ljava/lang/String;)Z", false);
				this.mv.visitJumpInsn(Opcodes.IFNE, end);
			}
			
			instrumentThrownException(this.mv, RUNTIME_EXCEPTION, this.getMessage());
			
			this.mv.visitLabel(end);
			this.mv.visitLocalVariable("generated$RestrictedAccessTransformer$Temp" + array, STACK_TRACE_ARRAY.getDescriptor(), null, start, end, array);
			this.mv.visitLocalVariable("generated$RestrictedAccessTransformer$Temp" + clazz, STRING.getDescriptor(), null, clazzVariable, end, clazz);
			this.mv.visitLocalVariable("generated$RestrictedAccessTransformer$Temp" + method, STRING.getDescriptor(), null, methodVariable, end, method);
		}
		
		//region Helper methods
		private @NotNull String getMessage() {
			if (this.values.isEmpty()) {
				return "Method '" + this.type.getClassName() + "#" + this.method.getName() + "' is not callable";
			}
			String base = "Method '" + this.type.getClassName() + "#" + this.method.getName() + "' has restricted access, ";
			if (this.pattern) {
				return base + "the caller must match one of the following patterns: '" + String.join("', '", this.values) + "'";
			}
			return base + "the caller must be one of the following: '" + String.join("', '", this.values) + "'";
		}
		//endregion
	}
}
