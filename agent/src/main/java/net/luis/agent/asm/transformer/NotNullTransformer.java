package net.luis.agent.asm.transformer;

import net.luis.agent.asm.ASMUtils;
import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.visitor.BaseClassVisitor;
import net.luis.agent.asm.base.visitor.BaseMethodVisitor;
import net.luis.agent.preload.PreloadContext;
import net.luis.agent.preload.data.*;
import net.luis.agent.preload.type.TypeModifier;
import net.luis.agent.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Luis-St
 *
 */

public class NotNullTransformer extends BaseClassTransformer {
	
	private static final Type NOT_NULL = Type.getType(NotNull.class);
	
	private final PreloadContext context;
	
	public NotNullTransformer(@NotNull PreloadContext context) {
		this.context = context;
	}
	
	private boolean checkReturn(@NotNull MethodData method) {
		return method.isMethod() && !method.getReturnType().equals(Type.VOID_TYPE) && method.isAnnotatedWith(NOT_NULL);
	}
	
	@Override
	@SuppressWarnings("DuplicatedCode")
	protected boolean shouldIgnore(@NotNull Type type) {
		ClassContent content = this.context.getClassContent(type);
		return (content.methods().stream().filter(method -> !method.is(TypeModifier.ABSTRACT)).map(MethodData::parameters).flatMap(List::stream).noneMatch(parameter -> parameter.isAnnotatedWith(NOT_NULL)) &&
			content.methods().stream().noneMatch(this::checkReturn)) || super.shouldIgnore(type);
	}
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @Nullable Class<?> clazz, @NotNull ClassReader reader, @NotNull ClassWriter writer) {
		ClassContent content = this.context.getClassContent(type);
		Runnable markedModified = () -> this.modified = true;
		return new BaseClassVisitor(writer) {
			@Override
			public @NotNull MethodVisitor visitMethod(int access, @NotNull String name, @NotNull String descriptor, @Nullable String signature, String @Nullable [] exceptions) {
				MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
				MethodData method = content.getMethod(name, Type.getType(descriptor));
				boolean checkReturn = NotNullTransformer.this.checkReturn(method);
				if (method.is(TypeModifier.ABSTRACT) || !(method.parameters().stream().anyMatch(parameter -> parameter.isAnnotatedWith(NOT_NULL)) || checkReturn)) {
					return visitor;
				}
				return new NotNullVisitor(type, visitor, method, !checkReturn, markedModified);
			}
		};
	}
	
	private static class NotNullVisitor extends BaseMethodVisitor {
		
		private final List<ParameterData> lookup = new ArrayList<>();
		private final Type type;
		private final MethodData method;
		private final Runnable markedModified;
		private final boolean ignoreReturn;
		
		private NotNullVisitor(@NotNull Type type, @NotNull MethodVisitor visitor, @NotNull MethodData method, boolean ignoreReturn, Runnable markedModified) {
			super(visitor);
			this.type = type;
			this.method = method;
			this.ignoreReturn = ignoreReturn;
			this.markedModified = markedModified;
			method.parameters().stream().filter(parameter -> parameter.isAnnotatedWith(NOT_NULL)).forEach(this.lookup::add);
		}
		
		private @NotNull String getMessage(@NotNull ParameterData parameter) {
			AnnotationData annotation = parameter.getAnnotation(NOT_NULL);
			if (annotation.has("message")) {
				String value = annotation.get("message");
				if (!value.isBlank()) {
					if (Utils.isSingleWord(value.strip())) {
						return Utils.capitalize(value.strip()) + " must not be null";
					}
					return value;
				}
			}
			if (parameter.isNamed()) {
				return Utils.capitalize(parameter.name()) + " must not be null";
			}
			return ASMUtils.getSimpleName(parameter.type()) + " (parameter #" + parameter.index() + ") must not be null";
		}
		
		@Override
		public void visitCode() {
			this.mv.visitCode();
			boolean isStatic = this.method.is(TypeModifier.STATIC);
			for (ParameterData parameter : this.lookup) {
				this.mv.visitVarInsn(Opcodes.ALOAD, isStatic ? parameter.index() : parameter.index() + 1);
				this.mv.visitLdcInsn(this.getMessage(parameter));
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Objects", "requireNonNull", "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;", false);
				this.mv.visitInsn(Opcodes.POP);
				this.markedModified.run();
			}
		}
		
		@Override
		public void visitInsn(int opcode) {
			if (opcode != Opcodes.ARETURN || this.ignoreReturn) {
				this.mv.visitInsn(opcode);
				return;
			}
			this.mv.visitLdcInsn("Method " + ASMUtils.getSimpleName(this.type) + "#" + this.method.name() + " must not return null");
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Objects", "requireNonNull", "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;", false);
			this.mv.visitTypeInsn(Opcodes.CHECKCAST, this.method.getReturnType().getInternalName());
			this.mv.visitInsn(opcode);
		}
	}
}
