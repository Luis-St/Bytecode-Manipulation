package net.luis.agent.asm.transformer.method;

import net.luis.agent.AgentContext;
import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.MethodOnlyClassVisitor;
import net.luis.agent.asm.data.Class;
import net.luis.agent.asm.data.*;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.asm.type.MethodType;
import net.luis.agent.util.Utils;
import org.jetbrains.annotations.NotNull;
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
		Class clazz = AgentContext.get().getClass(type);
		return clazz.getParameters().stream().noneMatch(parameter -> parameter.isAnnotatedWith(NOT_NULL)) && clazz.getMethods().values().stream().noneMatch(method -> method.isAnnotatedWith(NOT_NULL));
	}
	//endregion
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @NotNull ClassWriter writer) {
		return new MethodOnlyClassVisitor(writer, type, () -> this.modified = true) {
			
			@Override
			protected @NotNull MethodVisitor createMethodVisitor(@NotNull LocalVariablesSorter visitor, @NotNull Method method) {
				return new NotNullVisitor(visitor, method);
			}
		};
	}
	
	private static class NotNullVisitor extends MethodVisitor {
		
		private static final String REPORT_CATEGORY = "Invalid Annotated Element";
		
		private final List<Parameter> lookup = new ArrayList<>();
		private final Method method;
		
		private NotNullVisitor(@NotNull MethodVisitor visitor, @NotNull Method method) {
			super(Opcodes.ASM9, visitor);
			this.method = method;
			method.getParameters().values().stream().filter(parameter -> parameter.isAnnotatedWith(NOT_NULL)).forEach(this.lookup::add);
		}
		
		private @NotNull String getMessage(@NotNull Parameter parameter) {
			Annotation annotation = parameter.getAnnotation(NOT_NULL);
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
			for (Parameter parameter : this.lookup) {
				instrumentNonNullCheck(this.mv, parameter.getLoadIndex(), this.getMessage(parameter));
				this.mv.visitInsn(Opcodes.POP);
			}
		}
		
		@Override
		public void visitInsn(int opcode) {
			if (opcode == Opcodes.ARETURN && this.method.isAnnotatedWith(NOT_NULL)) {
				this.validateMethod();
				instrumentNonNullCheck(this.mv, -1, "Method " + this.method.getOwner().getClassName() + "#" + this.method.getName() + " must not return null");
				this.mv.visitTypeInsn(Opcodes.CHECKCAST, this.method.getReturnType().getInternalName());
			}
			this.mv.visitInsn(opcode);
		}
		
		//region Validation
		private void validateMethod() {
			if (!this.method.is(MethodType.METHOD)) {
				throw CrashReport.create("Annotation @NotNull can not be applied to constructors and static initializers", REPORT_CATEGORY).addDetail("Method", this.method.getName()).exception();
			}
			
			if (this.method.returns(VOID)) {
				throw CrashReport.create("Method annotated with @NotNull must not return void", REPORT_CATEGORY).addDetail("Method", this.method.getSourceSignature()).exception();
			}
			if (this.method.returnsAny(PRIMITIVES)) {
				throw CrashReport.create("Method annotated with @NotNull must not return a primitive type", REPORT_CATEGORY).addDetail("Method", this.method.getSourceSignature())
					.addDetail("Return Type", this.method.getReturnType()).exception();
			}
		}
		//endregion
	}
}
