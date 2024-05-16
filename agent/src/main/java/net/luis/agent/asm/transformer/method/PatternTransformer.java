package net.luis.agent.asm.transformer.method;

import net.luis.agent.asm.ASMUtils;
import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.visitor.BaseMethodVisitor;
import net.luis.agent.asm.base.visitor.MethodOnlyClassVisitor;
import net.luis.agent.preload.PreloadContext;
import net.luis.agent.preload.data.*;
import net.luis.agent.preload.type.MethodType;
import net.luis.agent.preload.type.TypeModifier;
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
		super(context, true);
	}
	
	//region Type filtering
	@Override
	protected boolean shouldTransform(@NotNull Type type) {
		ClassContent content = this.context.getClassContent(type);
		return content.getParameters().stream().anyMatch(parameter -> parameter.isAnnotatedWith(PATTERN)) || content.methods().stream().anyMatch(method -> method.returns(STRING) && method.isAnnotatedWith(PATTERN));
	}
	//endregion
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @Nullable Class<?> clazz, @NotNull ClassReader reader, @NotNull ClassWriter writer) {
		ClassContent content = this.context.getClassContent(type);
		return new MethodOnlyClassVisitor(writer, this.context, type, () -> this.modified = true) {
			
			@Override
			protected @NotNull MethodVisitor createMethodVisitor(@NotNull LocalVariablesSorter visitor, @NotNull MethodData method) {
				return new PatternVisitor(visitor, this.context, this.type, method, this::markModified);
			}
		};
	}
	
	private static class PatternVisitor extends BaseMethodVisitor {
		
		private static final Type ILL_ARG = Type.getType(IllegalArgumentException.class);
		
		private final List<ParameterData> lookup = new ArrayList<>();
		
		private PatternVisitor(@NotNull LocalVariablesSorter visitor, @NotNull PreloadContext context, @NotNull Type type, @NotNull MethodData method, @NotNull Runnable markModified) {
			super(visitor, context, type, method, markModified);
			method.parameters().stream().filter(parameter -> parameter.isAnnotatedWith(PATTERN)).forEach(this.lookup::add);
		}
		
		@Override
		public void visitCode() {
			this.mv.visitCode();
			boolean isStatic = this.method.is(TypeModifier.STATIC);
			for (ParameterData parameter : this.lookup) {
				Label label = new Label();
				String value = parameter.getAnnotation(PATTERN).get("value");
				if (value == null) {
					continue;
				}
				
				this.instrumentPatternCheck(this.mv, value, isStatic ? parameter.index() : parameter.index() + 1, label);
				this.instrumentThrownException(this.mv, ILL_ARG, parameter.getMessageName() + " must match pattern '" + value + "'");
				
				this.mv.visitJumpInsn(Opcodes.GOTO, label);
				this.mv.visitLabel(label);
				this.markModified();
			}
		}
		
		@Override
		public void visitInsn(int opcode) {
			if (opcode == Opcodes.ARETURN && this.isValidReturn()) {
				String value = this.method.getAnnotation(PATTERN).get("value");
				if (value == null) {
					this.mv.visitInsn(opcode);
					return;
				}
				Label start = new Label();
				Label end = new Label();
				int local = this.newLocal(this.method.getReturnType());
				this.mv.visitLabel(start);
				this.mv.visitVarInsn(Opcodes.ASTORE, local);
				
				this.instrumentPatternCheck(this.mv, value, local, end);
				this.instrumentThrownException(this.mv, ILL_ARG, "Method " + ASMUtils.getSimpleName(this.type) + "#" + this.method.name() + " return value must match pattern '" + value + "'");
				
				this.mv.visitJumpInsn(Opcodes.GOTO, end);
				this.mv.visitLabel(end);
				this.mv.visitVarInsn(Opcodes.ALOAD, local);
				this.mv.visitLocalVariable("generated$PatternTransformer$Temp" + local, STRING.getDescriptor(), null, start, end, local);
				this.markModified();
			}
			this.mv.visitInsn(opcode);
		}
		
		//region Helper methods
		private boolean isValidReturn() {
			return this.method.is(MethodType.METHOD) && this.method.returns(STRING) && this.method.isAnnotatedWith(PATTERN);
		}
		//endregion
	}
}
