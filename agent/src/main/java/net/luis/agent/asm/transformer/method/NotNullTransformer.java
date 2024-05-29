package net.luis.agent.asm.transformer.method;

import net.luis.agent.AgentContext;
import net.luis.agent.asm.ASMUtils;
import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.visitor.ContextBasedMethodVisitor;
import net.luis.agent.asm.base.visitor.MethodOnlyClassVisitor;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.preload.data.*;
import net.luis.agent.preload.type.MethodType;
import net.luis.agent.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.util.ArrayList;
import java.util.List;

import static net.luis.agent.asm.Instrumentations.*;
import static net.luis.agent.asm.Types.*;

/**
 *
 * @author Luis-St
 *
 */

public class NotNullTransformer extends BaseClassTransformer {
	
	//region Type filtering
	@Override
	protected boolean shouldIgnoreClass(@NotNull Type type) {
		ClassData data = AgentContext.get().getClassData(type);
		return data.getParameters().stream().noneMatch(parameter -> parameter.isAnnotatedWith(NOT_NULL)) && data.methods().stream().noneMatch(method -> method.isAnnotatedWith(NOT_NULL));
	}
	//endregion
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @Nullable Class<?> clazz, @NotNull ClassWriter writer) {
		return new MethodOnlyClassVisitor(writer, type, () -> this.modified = true) {
			
			@Override
			protected @NotNull MethodVisitor createMethodVisitor(@NotNull LocalVariablesSorter visitor, @NotNull MethodData method) {
				return new NotNullVisitor(visitor, method, this::markModified);
			}
		};
	}
	
	private static class NotNullVisitor extends ContextBasedMethodVisitor {
		
		private static final String REPORT_CATEGORY = "Invalid Annotated Element";
		
		private final List<ParameterData> lookup = new ArrayList<>();
		
		private NotNullVisitor(@NotNull MethodVisitor visitor, @NotNull MethodData method, @NotNull Runnable markModified) {
			super(visitor, method, markModified);
			method.parameters().stream().filter(parameter -> parameter.isAnnotatedWith(NOT_NULL)).forEach(this.lookup::add);
		}
		
		private @NotNull String getMessage(@NotNull ParameterData parameter) {
			AnnotationData annotation = parameter.getAnnotation(NOT_NULL);
			String value = annotation.getOrDefault("value");
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
			for (ParameterData parameter : this.lookup) {
				this.visitVarInsn(Opcodes.ALOAD, parameter);
				instrumentNonNullCheck(this.mv, this.getMessage(parameter));
				this.mv.visitInsn(Opcodes.POP);
				this.markModified();
			}
		}
		
		@Override
		public void visitInsn(int opcode) {
			if (opcode == Opcodes.ARETURN && this.method.isAnnotatedWith(NOT_NULL)) {
				this.validateMethod();
				instrumentNonNullCheck(this.mv, "Method " + ASMUtils.getSimpleName(this.method.owner()) + "#" + this.method.name() + " must not return null");
				this.mv.visitTypeInsn(Opcodes.CHECKCAST, this.method.getReturnType().getInternalName());
				this.markModified();
			}
			this.mv.visitInsn(opcode);
		}
		
		//region Validation
		private void validateMethod() {
			if (!this.method.is(MethodType.METHOD)) {
				throw CrashReport.create("Annotation @NotNull can not be applied to constructors and static initializers", REPORT_CATEGORY).addDetail("Method", this.method.name()).exception();
			}
			
			if (this.method.returns(VOID)) {
				throw CrashReport.create("Method annotated with @NotNull must not return void", REPORT_CATEGORY).addDetail("Method", this.method.getMethodSignature()).exception();
			}
			if (this.method.returnsAny(PRIMITIVES)) {
				throw CrashReport.create("Method annotated with @NotNull must not return a primitive type", REPORT_CATEGORY).addDetail("Method", this.method.getMethodSignature())
					.addDetail("Return Type", this.method.getReturnType()).exception();
			}
		}
		//endregion
	}
}
