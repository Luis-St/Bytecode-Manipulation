package net.luis.agent.asm.transformer.method;

import net.luis.agent.Agent;
import net.luis.agent.asm.base.*;
import net.luis.agent.asm.data.Class;
import net.luis.agent.asm.data.*;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.asm.type.MethodType;
import net.luis.agent.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.util.*;

import static net.luis.agent.asm.Instrumentations.*;
import static net.luis.agent.asm.Types.*;

/**
 *
 * @author Luis-St
 *
 */

public class NotNullTransformer extends BaseClassTransformer {
	
	public NotNullTransformer() {
		super(true);
	}
	
	//region Type filtering
	@Override
	protected boolean shouldIgnoreClass(@NotNull Type type) {
		Class clazz = Agent.getClass(type);
		return clazz.getMethods().values().stream().noneMatch(method -> method.isAnnotatedWith(NOT_NULL)) && clazz.getParameters().stream().noneMatch(parameter -> parameter.isAnnotatedWith(NOT_NULL));
	}
	//endregion
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @NotNull ClassWriter writer) {
		return new MethodOnlyClassVisitor(writer, type, () -> this.modified = true) {
			
			@Override
			protected boolean isMethodValid(@NotNull Method method) {
				if (!super.isMethodValid(method)) {
					return false;
				}
				if (method.isAnnotatedWith(NOT_NULL)) {
					return true;
				}
				return method.getParameters().values().stream().anyMatch(parameter -> parameter.isAnnotatedWith(NOT_NULL)) || method.getLocals().stream().anyMatch(local -> local.isAnnotatedWith(NOT_NULL));
			}
			
			@Override
			protected @NotNull MethodVisitor createMethodVisitor(@NotNull LocalVariablesSorter visitor, @NotNull Method method) {
				return new NotNullVisitor(visitor, method);
			}
		};
	}
	
	private static class NotNullVisitor extends LabelTrackingMethodVisitor {
		
		private static final String REPORT_CATEGORY = "Invalid Annotated Element";
		
		private final List<Parameter> parameters = new ArrayList<>();
		private final Method method;
		
		private NotNullVisitor(@NotNull MethodVisitor visitor, @NotNull Method method) {
			super(visitor);
			this.setMethod(method);
			this.method = method;
			method.getParameters().values().stream().filter(parameter -> parameter.isAnnotatedWith(NOT_NULL)).forEach(this.parameters::add);
		}
		
		@Override
		public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
			super.visitFrame(type, numLocal, local, numStack, stack);
		}
		
		@Override
		public void visitCode() {
			this.mv.visitCode();
			for (Parameter parameter : this.parameters) {
				this.validateParameter(parameter);
				instrumentNonNullCheck(this.mv, parameter.getLoadIndex(), this.getMessage(parameter.getAnnotation(NOT_NULL), parameter.getMessageName()));
				this.mv.visitInsn(Opcodes.POP);
			}
		}
		
		@Override
		public void visitVarInsn(int opcode, int index) {
			if (isStore(opcode) && this.method.isLocal(index)) {
				List<LocalVariable> locals = this.method.getLocals(index);
				if (locals.isEmpty() || locals.stream().noneMatch(local -> local.isAnnotatedWith(NOT_NULL))) {
					super.visitVarInsn(opcode, index);
					return;
				}
				
				LocalVariable local = locals.stream().filter(l -> l.isAnnotatedWith(NOT_NULL)).filter(l -> l.getIndex() == index && l.isInBounds(this.getCurrentLabelIndex())).findFirst().orElse(null);
				if (local == null) {
					System.out.println("Local variable with index " + index + " and label index " + this.getCurrentLabelIndex() + " not found in method " + this.method.getSourceSignature(true));
					this.method.getLocals().forEach(l -> System.out.println(" - " + l.getSourceSignature(false)));
					super.visitVarInsn(opcode, index);
					return;
				}
				if (!local.isAnnotatedWith(NOT_NULL)) {
					super.visitVarInsn(opcode, index);
					return;
				}
				
				this.validateLocal(local);
				instrumentNonNullCheck(this.mv, -1, this.getMessage(local.getAnnotation(NOT_NULL), local.getMessageName()));
				this.mv.visitTypeInsn(Opcodes.CHECKCAST, local.getType().getInternalName());
			}
			super.visitVarInsn(opcode, index);
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
		
		//region Helper methods
		private @NotNull String getMessage(@NotNull Annotation annotation, @NotNull String messageName) {
			String value = annotation.getOrDefault("value");
			if (!value.isBlank()) {
				if (Utils.isSingleWord(value.strip())) {
					return Utils.capitalize(value.strip()) + " must not be null";
				}
				return value;
			}
			return messageName + " must not be null";
		}
		
		private void validateParameter(@NotNull Parameter parameter) {
			if (parameter.isAny(PRIMITIVES)) {
				throw CrashReport.create("Parameter annotated with @NotNull must not be a primitive type", REPORT_CATEGORY).addDetail("Method", this.method.getSourceSignature(true))
					.addDetail("Parameter Index", parameter.getIndex()).addDetail("Parameter Type", parameter.getType()).addDetail("Parameter Name", parameter.getName()).exception();
			}
		}
		
		private void validateLocal(@NotNull LocalVariable local) {
			if (local.isAny(PRIMITIVES)) {
				throw CrashReport.create("Parameter annotated with @NotNull must not be a primitive type", REPORT_CATEGORY).addDetail("Method", this.method.getSourceSignature(true))
					.addDetail("Local Variable Index", local.getIndex()).addDetail("Local Variable  Type", local.getType()).addDetail("Local Variable  Name", local.getName()).exception();
			}
		}
		
		private void validateMethod() {
			if (!this.method.is(MethodType.METHOD)) {
				throw CrashReport.create("Annotation @NotNull can not be applied to constructors and static initializers", REPORT_CATEGORY).addDetail("Method", this.method.getSourceSignature(true)).exception();
			}
			if (this.method.returns(VOID)) {
				throw CrashReport.create("Method annotated with @NotNull must not return void", REPORT_CATEGORY).addDetail("Method", this.method.getSourceSignature(true)).exception();
			}
			if (this.method.returnsAny(PRIMITIVES)) {
				throw CrashReport.create("Method annotated with @NotNull must not return a primitive type", REPORT_CATEGORY).addDetail("Method", this.method.getSourceSignature(true))
					.addDetail("Return Type", this.method.getReturnType()).exception();
			}
		}
		//endregion
	}
}
