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
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.util.ArrayList;
import java.util.List;

import static net.luis.agent.asm.Types.*;

/**
 *
 * @author Luis-St
 *
 */

public class PatternTransformer extends BaseClassTransformer {
	
	public PatternTransformer(@NotNull PreloadContext context) {
		super(context);
	}
	
	private static boolean isMethodValid(@Nullable MethodData method) {
		return method != null && method.isImplementedMethod() && method.returns(STRING) && method.isAnnotatedWith(PATTERN);
	}
	
	@Override
	protected int getClassWriterFlags() {
		return ClassWriter.COMPUTE_FRAMES;
	}
	
	@Override
	protected boolean shouldTransform(@NotNull Type type) {
		ClassContent content = this.context.getClassContent(type);
		return content.methods().stream().filter(MethodData::isImplementedMethod).map(MethodData::parameters).flatMap(List::stream).anyMatch(parameter -> parameter.isAnnotatedWith(PATTERN)) || content.methods().stream().anyMatch(PatternTransformer::isMethodValid);
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
				boolean checkReturn = isMethodValid(method);
				if (method.is(TypeModifier.ABSTRACT) || !(method.parameters().stream().anyMatch(parameter -> parameter.isAnnotatedWith(PATTERN)) || checkReturn)) {
					return visitor;
				}
				LocalVariablesSorter sorter = new LocalVariablesSorter(access, descriptor, visitor);
				return new PatternVisitor(type, sorter, method, !checkReturn, markedModified);
			}
		};
	}
	
	private static class PatternVisitor extends BaseMethodVisitor {
		
		private final List<ParameterData> lookup = new ArrayList<>();
		private final Type type;
		private final LocalVariablesSorter sorter;
		private final MethodData method;
		private final Runnable markedModified;
		private final boolean ignoreReturn;
		
		@SuppressWarnings("DuplicatedCode")
		private PatternVisitor(@NotNull Type type, @NotNull LocalVariablesSorter visitor, @NotNull MethodData method, boolean ignoreReturn, Runnable markedModified) {
			super(visitor);
			this.type = type;
			this.sorter = visitor;
			this.method = method;
			this.ignoreReturn = ignoreReturn;
			this.markedModified = markedModified;
			method.parameters().stream().filter(parameter -> parameter.isAnnotatedWith(PATTERN)).forEach(this.lookup::add);
		}
		
		private @NotNull String getMessage(@NotNull ParameterData parameter, @NotNull String pattern) {
			if (parameter.isNamed()) {
				return Utils.capitalize(parameter.name()) + " must match pattern '" + pattern + "'";
			}
			return ASMUtils.getSimpleName(parameter.type()) + " (parameter #" + parameter.index() + ") must match pattern '" + pattern + "'";
		}
		
		private void visitPatternCheck(@NotNull String regex, int index, @NotNull Label label) {
			this.mv.visitLdcInsn(regex);
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/regex/Pattern", "compile", "(Ljava/lang/String;)Ljava/util/regex/Pattern;", false);
			this.mv.visitVarInsn(Opcodes.ALOAD, index);
			this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/regex/Pattern", "matcher", "(Ljava/lang/CharSequence;)Ljava/util/regex/Matcher;", false);
			this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/regex/Matcher", "matches", "()Z", false);
			this.mv.visitJumpInsn(Opcodes.IFNE, label);
		}
		
		private void visitException(@NotNull String message) {
			this.mv.visitTypeInsn(Opcodes.NEW, "java/lang/IllegalArgumentException");
			this.mv.visitInsn(Opcodes.DUP);
			this.mv.visitLdcInsn(message);
			this.mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/IllegalArgumentException", "<init>", "(Ljava/lang/String;)V", false);
			this.mv.visitInsn(Opcodes.ATHROW);
		}
		
		@Override
		public void visitCode() {
			this.mv.visitCode();
			boolean isStatic = this.method.is(TypeModifier.STATIC);
			for (ParameterData parameter : this.lookup) {
				Label label = new Label();
				String value = parameter.getAnnotation(PATTERN).get("value");
				
				this.visitPatternCheck(value == null ? ".*" : value, isStatic ? parameter.index() : parameter.index() + 1, label);
				this.visitException(this.getMessage(parameter, value == null ? ".*" : value));
				
				this.mv.visitJumpInsn(Opcodes.GOTO, label);
				this.mv.visitLabel(label);
				this.markedModified.run();
			}
		}
		
		@Override
		public void visitInsn(int opcode) {
			if (opcode != Opcodes.ARETURN || this.ignoreReturn) {
				this.mv.visitInsn(opcode);
				return;
			}
			Label label = new Label();
			String value = this.method.getAnnotation(PATTERN).get("value");
			int local = this.sorter.newLocal(this.method.getReturnType());
			this.mv.visitVarInsn(Opcodes.ASTORE, local);
			
			this.visitPatternCheck(value == null ? ".*" : value, local, label);
			this.visitException("Method " + ASMUtils.getSimpleName(this.type) + "#" + this.method.name() + " return value must match pattern '" + value + "'");
			
			this.mv.visitJumpInsn(Opcodes.GOTO, label);
			this.mv.visitLabel(label);
			this.mv.visitVarInsn(Opcodes.ALOAD, local);
			this.mv.visitInsn(opcode);
			this.markedModified.run();
		}
	}
}
