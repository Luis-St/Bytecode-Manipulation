package net.luis.agent.asm.transformer.method;

import net.luis.agent.asm.ASMUtils;
import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.visitor.*;
import net.luis.agent.preload.PreloadContext;
import net.luis.agent.preload.data.*;
import net.luis.agent.preload.type.MethodType;
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

public class NotNullTransformer extends BaseClassTransformer {
	
	public NotNullTransformer(@NotNull PreloadContext context) {
		super(context);
	}
	
	//region Type filtering
	@Override
	protected boolean shouldTransform(@NotNull Type type) {
		ClassContent content = this.context.getClassContent(type);
		return content.getParameters().stream().anyMatch(parameter -> parameter.isAnnotatedWith(NOT_NULL)) || content.methods().stream().anyMatch(method -> method.isAnnotatedWith(NOT_NULL));
	}
	//endregion
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @Nullable Class<?> clazz, @NotNull ClassReader reader, @NotNull ClassWriter writer) {
		ClassContent content = this.context.getClassContent(type);
		return new MethodOnlyClassVisitor(writer, this.context, type, () -> this.modified = true) {
			
			@Override
			protected @NotNull MethodVisitor createMethodVisitor(@NotNull LocalVariablesSorter visitor, @NotNull MethodData method) {
				return new NotNullVisitor(visitor, this.context, this.type, method, this::markModified);
			}
		};
	}
	
	private static class NotNullVisitor extends ModificationMethodVisitor {
		
		private final List<ParameterData> lookup = new ArrayList<>();
		
		private NotNullVisitor(@NotNull MethodVisitor visitor, @NotNull PreloadContext context, @NotNull Type type, @NotNull MethodData method, @NotNull Runnable markModified) {
			super(visitor, context, type, method, markModified);
			method.parameters().stream().filter(parameter -> parameter.isAnnotatedWith(NOT_NULL)).forEach(this.lookup::add);
		}
		
		private @NotNull String getMessage(@NotNull ParameterData parameter) {
			AnnotationData annotation = parameter.getAnnotation(NOT_NULL);
			String value = annotation.getOrDefault(this.context, "value");
			if (!value.isBlank()) {
				if (Utils.isSingleWord(value.strip())) {
					return Utils.capitalize(value.strip()) + " must not be null";
				}
				return value;
			}
			return parameter.getMessageName() + " must not be null";
		}
		
		@Override
		public void visitCode() {
			this.mv.visitCode();
			boolean isStatic = this.method.is(TypeModifier.STATIC);
			for (ParameterData parameter : this.lookup) {
				this.mv.visitVarInsn(Opcodes.ALOAD, isStatic ? parameter.index() : parameter.index() + 1);
				this.instrumentNonNullCheck(this.mv, this.getMessage(parameter));
				this.mv.visitInsn(Opcodes.POP);
				this.markModified();
			}
		}
		
		@Override
		public void visitInsn(int opcode) {
			if (opcode == Opcodes.ARETURN && this.isValidReturn()) {
				this.instrumentNonNullCheck(this.mv, "Method " + ASMUtils.getSimpleName(this.type) + "#" + this.method.name() + " must not return null");
				this.mv.visitTypeInsn(Opcodes.CHECKCAST, this.method.getReturnType().getInternalName());
				this.markModified();
			}
			this.mv.visitInsn(opcode);
		}
		
		//region Helper methods
		private boolean isValidReturn() {
			return this.method.is(MethodType.METHOD) && !this.method.returnsAny(PRIMITIVES) && this.method.isAnnotatedWith(NOT_NULL);
		}
		//endregion
	}
}
