package net.luis.agent.asm.transformer.method;

import net.luis.agent.AgentContext;
import net.luis.agent.asm.base.*;
import net.luis.agent.asm.data.*;
import net.luis.agent.asm.data.Class;
import net.luis.agent.asm.type.ClassType;
import net.luis.agent.asm.type.TypeModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.util.List;
import java.util.Objects;

import static net.luis.agent.asm.Types.*;
import static net.luis.agent.asm.Instrumentations.*;

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
		Class clazz = AgentContext.get().getClass(type);
		return clazz.getMethods().values().stream().noneMatch(method -> method.isAnnotatedWith(RESTRICTED_ACCESS));
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
			Class clazz = AgentContext.get().getClass(this.type);
			Method method = clazz.getMethod(name + descriptor);
			MethodVisitor visitor = this.cv.visitMethod(access, name, descriptor, signature, exceptions);
			if (method == null || method.is(TypeModifier.ABSTRACT) || !method.isAnnotatedWith(RESTRICTED_ACCESS)) {
				return visitor;
			}
			
			return new RestrictedAccessMethodVisitor(new LocalVariablesSorter(access, descriptor, visitor), method);
		}
		
		@Override
		public void visitEnd() {
			this.instrumentHelperMethod();
			this.updateClass();
			this.markModified();
			this.cv.visitEnd();
		}
		
		//region Instrumentation
		@SuppressWarnings("DuplicatedCode")
		private void instrumentHelperMethod() {
			MethodVisitor visitor = this.cv.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, "generated$IsAccessAllowed", "(Ljava/lang/String;ZLjava/lang/String;Ljava/lang/String;)Z", null, null);
			//region Labels
			Label methodStart = new Label();
			Label[] labels = new Label[] {
				new Label(), new Label(), new Label(), new Label(), new Label(), new Label(), new Label()
			};
			Label methodEnd = new Label();
			//endregion
			
			//region Parameters
			visitor.visitParameter("target", 0);
			visitor.visitParameterAnnotation(0, NOT_NULL.getDescriptor(), false).visitEnd();
			visitor.visitParameter("pattern", 0);
			visitor.visitParameter("className", 0);
			visitor.visitParameterAnnotation(2, NOT_NULL.getDescriptor(), false).visitEnd();
			visitor.visitParameter("methodName", 0);
			visitor.visitParameterAnnotation(3, NOT_NULL.getDescriptor(), false).visitEnd();
			//endregion
			
			visitor.visitCode();
			visitor.visitLabel(methodStart);
			
			//region Parameter validation
			instrumentNonNullCheck(visitor, 0, "Target must not be null");
			visitor.visitInsn(Opcodes.POP);
			instrumentNonNullCheck(visitor, 2, "Class name must not be null");
			visitor.visitInsn(Opcodes.POP);
			instrumentNonNullCheck(visitor, 3, "Method name must not be null");
			visitor.visitInsn(Opcodes.POP);
			visitor.visitLabel(labels[0]);
			//endregion
			
			
			//region Create and store class and method concat
			visitor.visitLdcInsn("#");
			visitor.visitInsn(Opcodes.ICONST_2);
			visitor.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/CharSequence");
			visitor.visitInsn(Opcodes.DUP);
			visitor.visitInsn(Opcodes.ICONST_0);
			visitor.visitVarInsn(Opcodes.ALOAD, 2);
			visitor.visitInsn(Opcodes.AASTORE);
			visitor.visitInsn(Opcodes.DUP);
			visitor.visitInsn(Opcodes.ICONST_1);
			visitor.visitVarInsn(Opcodes.ALOAD, 3);
			visitor.visitInsn(Opcodes.AASTORE);
			visitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/String", "join", "(Ljava/lang/CharSequence;[Ljava/lang/CharSequence;)Ljava/lang/String;", false);
			visitor.visitVarInsn(Opcodes.ASTORE, 4);
			//endregion
			
			visitor.visitVarInsn(Opcodes.ILOAD, 1);
			visitor.visitJumpInsn(Opcodes.IFEQ, labels[1]);
			
			//region Pattern check
			visitor.visitVarInsn(Opcodes.ALOAD, 0);
			visitor.visitVarInsn(Opcodes.ALOAD, 4);
			visitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/regex/Pattern", "matches", "(Ljava/lang/String;Ljava/lang/CharSequence;)Z", false);
			visitor.visitInsn(Opcodes.IRETURN);
			//endregion
			
			visitor.visitLabel(labels[1]);
			
			//region Equals check
			instrumentEqualsIgnoreCase(visitor, 0, 2);
			visitor.visitJumpInsn(Opcodes.IFNE, labels[2]);
			
			instrumentEqualsIgnoreCase(visitor, 0, 3);
			visitor.visitJumpInsn(Opcodes.IFNE, labels[2]);
			
			instrumentEqualsIgnoreCase(visitor, 0, 4);
			visitor.visitJumpInsn(Opcodes.IFNE, labels[2]);
			
			visitor.visitVarInsn(Opcodes.ALOAD, 0);
			visitor.visitLdcInsn("#");
			visitor.visitInsn(Opcodes.ICONST_2);
			visitor.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/CharSequence");
			visitor.visitInsn(Opcodes.DUP);
			visitor.visitInsn(Opcodes.ICONST_0);
			visitor.visitLdcInsn("");
			visitor.visitInsn(Opcodes.AASTORE);
			visitor.visitInsn(Opcodes.DUP);
			visitor.visitInsn(Opcodes.ICONST_1);
			visitor.visitVarInsn(Opcodes.ALOAD, 3);
			visitor.visitInsn(Opcodes.AASTORE);
			visitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/String", "join", "(Ljava/lang/CharSequence;[Ljava/lang/CharSequence;)Ljava/lang/String;", false);
			visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equalsIgnoreCase", "(Ljava/lang/String;)Z", false);
			visitor.visitJumpInsn(Opcodes.IFEQ, labels[3]);
			//endregion
			
			visitor.visitLabel(labels[2]);
			visitor.visitInsn(Opcodes.ICONST_1);
			visitor.visitInsn(Opcodes.IRETURN);
			visitor.visitLabel(labels[3]);
			
			//region Simple name equals check
			visitor.visitVarInsn(Opcodes.ALOAD, 2);
			visitor.visitVarInsn(Opcodes.ALOAD, 2);
			visitor.visitLdcInsn(".");
			visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "lastIndexOf", "(Ljava/lang/String;)I", false);
			visitor.visitInsn(Opcodes.ICONST_1);
			visitor.visitInsn(Opcodes.IADD);
			visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "substring", "(I)Ljava/lang/String;", false);
			visitor.visitVarInsn(Opcodes.ASTORE, 5);
			
			visitor.visitVarInsn(Opcodes.ALOAD, 0);
			visitor.visitVarInsn(Opcodes.ALOAD, 5);
			visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equalsIgnoreCase", "(Ljava/lang/String;)Z", false);
			visitor.visitJumpInsn(Opcodes.IFNE, labels[4]);
			visitor.visitVarInsn(Opcodes.ALOAD, 0);
			visitor.visitLdcInsn("#");
			visitor.visitInsn(Opcodes.ICONST_2);
			visitor.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/CharSequence");
			visitor.visitInsn(Opcodes.DUP);
			visitor.visitInsn(Opcodes.ICONST_0);
			visitor.visitVarInsn(Opcodes.ALOAD, 5);
			visitor.visitInsn(Opcodes.AASTORE);
			visitor.visitInsn(Opcodes.DUP);
			visitor.visitInsn(Opcodes.ICONST_1);
			visitor.visitVarInsn(Opcodes.ALOAD, 3);
			visitor.visitInsn(Opcodes.AASTORE);
			visitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/String", "join", "(Ljava/lang/CharSequence;[Ljava/lang/CharSequence;)Ljava/lang/String;", false);
			visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equalsIgnoreCase", "(Ljava/lang/String;)Z", false);
			visitor.visitJumpInsn(Opcodes.IFEQ, labels[5]);
			
			visitor.visitLabel(labels[4]);
			visitor.visitInsn(Opcodes.ICONST_1);
			visitor.visitJumpInsn(Opcodes.GOTO, labels[6]);
			visitor.visitLabel(labels[5]);
			visitor.visitInsn(Opcodes.ICONST_0);
			visitor.visitLabel(labels[6]);
			visitor.visitInsn(Opcodes.IRETURN);
			//endregion
			
			visitor.visitLabel(methodEnd);
			visitor.visitLocalVariable("target", STRING.getDescriptor(), null, methodStart, methodEnd, 0);
			visitor.visitLocalVariable("pattern", BOOLEAN.getDescriptor(), null, methodStart, methodEnd, 1);
			visitor.visitLocalVariable("className", STRING.getDescriptor(), null, methodStart, methodEnd, 2);
			visitor.visitLocalVariable("methodName", STRING.getDescriptor(), null, methodStart, methodEnd, 3);
			visitor.visitLocalVariable("concat", STRING.getDescriptor(), null, labels[0], methodEnd, 4);
			visitor.visitLocalVariable("simple", STRING.getDescriptor(), null, labels[3], methodEnd, 5);
			visitor.visitMaxs(0, 0);
			visitor.visitEnd();
		}
		
		private void updateClass() {
			Method method = Method.of(this.type, "generated$IsAccessAllowed", Type.getType("(Ljava/lang/String;ZLjava/lang/String;Ljava/lang/String;)Z"), null, Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC);
			method.getParameters().put(0, Parameter.builder(method, "target", STRING).addAnnotation(NOT_NULL, Annotation.of(NOT_NULL)).build());
			method.getParameters().put(1, Parameter.builder(method, "pattern", BOOLEAN).build());
			method.getParameters().put(2, Parameter.builder(method, "className", STRING).addAnnotation(NOT_NULL, Annotation.of(NOT_NULL)).build());
			method.getParameters().put(3, Parameter.builder(method, "methodName", STRING).addAnnotation(NOT_NULL, Annotation.of(NOT_NULL)).build());
			method.getLocals().put(4, LocalVariable.builder(method, 4, "concat", STRING).build());
			method.getLocals().put(5, LocalVariable.builder(method, 5, "simple", STRING).build());
			AgentContext.get().getClass(this.type).getMethods().put(method.getFullSignature(), method);
		}
		//endregion
	}
	
	private static class RestrictedAccessMethodVisitor extends MethodVisitor {
		
		private static final Type STACK_TRACE_ARRAY = Type.getType("[Ljava/lang/StackTraceElement;");
		private static final Type RUNTIME_EXCEPTION = Type.getType("Ljava/lang/RuntimeException;");
		
		private final Type type;
		private final boolean isInterface;
		private final Method method;
		private final List<String> values;
		private final boolean pattern;
		
		
		private RestrictedAccessMethodVisitor(@NotNull MethodVisitor visitor, @NotNull Method method) {
			super(Opcodes.ASM9, visitor);
			this.type = method.getOwner();
			this.isInterface = AgentContext.get().getClass(method.getOwner()).is(ClassType.INTERFACE);
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
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, this.type.getInternalName(), "generated$IsAccessAllowed", "(Ljava/lang/String;ZLjava/lang/String;Ljava/lang/String;)Z", this.isInterface);
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
