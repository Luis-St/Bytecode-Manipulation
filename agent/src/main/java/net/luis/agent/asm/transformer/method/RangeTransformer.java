package net.luis.agent.asm.transformer.method;

import net.luis.agent.Agent;
import net.luis.agent.asm.base.*;
import net.luis.agent.asm.data.Class;
import net.luis.agent.asm.data.*;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.asm.type.MethodType;
import net.luis.agent.asm.type.SignatureType;
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

public class RangeTransformer extends BaseClassTransformer {
	
	private static final Type[] ANNOS = { ABOVE, ABOVE_EQUAL, BELOW, BELOW_EQUAL };
	
	public RangeTransformer() {
		super(true);
	}
	
	//region Type filtering
	@Override
	protected boolean shouldIgnoreClass(@NotNull Type type) {
		Class clazz = Agent.getClass(type);
		return clazz.getMethods().values().stream().noneMatch(method -> method.isAnnotatedWithAny(ANNOS)) && clazz.getParameters().stream().noneMatch(parameter -> parameter.isAnnotatedWithAny(ANNOS));
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
				return method.isAnnotatedWithAny(ANNOS) || method.getParameters().values().stream().anyMatch(parameter -> parameter.isAnnotatedWithAny(ANNOS));
			}
			
			@Override
			protected @NotNull MethodVisitor createMethodVisitor(@NotNull LocalVariablesSorter visitor, @NotNull Method method) {
				return new RangeVisitor(visitor, method);
			}
		};
	}
	
	private static class RangeVisitor extends LabelTrackingMethodVisitor {
		
		private static final String INVALID_CATEGORY = "Invalid Annotated Element";
		private static final String UNSUPPORTED_CATEGORY = "Unsupported Annotation Combination";
		
		private final List<Parameter> lookup = new ArrayList<>();
		
		private RangeVisitor(@NotNull MethodVisitor visitor, @NotNull Method method) {
			super(visitor);
			this.method = method;
			//region Parameter validation
			String signature = method.getSignature(SignatureType.DEBUG);
			for (Parameter parameter : method.getParameters().values()) {
				if (parameter.isAnnotatedWithAny(ANNOS)) {
					if (this.isNoNumber(parameter.getType())) {
						throw CrashReport.create(UNSUPPORTED_CATEGORY, "Parameter annotated with @Above, @AboveEqual, @Below or @BelowEqual must be a number type").addDetail("Method", signature)
							.addDetail("Parameter Index", parameter.getIndex()).addDetail("Parameter Type", parameter.getType()).addDetail("Parameter Name", parameter.getName()).exception();
					}
					if (parameter.getAnnotations().values().stream().filter(annotation -> annotation.isAny(ABOVE, ABOVE_EQUAL)).count() > 1) {
						throw CrashReport.create(UNSUPPORTED_CATEGORY, "Parameter must not be annotated with @Above and @AboveEqual at the same time").addDetail("Method", signature)
							.addDetail("Parameter Index", parameter.getIndex()).addDetail("Parameter Type", parameter.getType()).addDetail("Parameter Name", parameter.getName())
							.addDetail("Annotations", parameter.getAnnotations().values().stream().map(Annotation::getType).toList()).exception();
					}
					if (parameter.getAnnotations().values().stream().filter(annotation -> annotation.isAny(BELOW, BELOW_EQUAL)).count() > 1) {
						throw CrashReport.create(UNSUPPORTED_CATEGORY, "Parameter must not be annotated with @Below and @BelowEqual at the same time").addDetail("Method", signature)
							.addDetail("Parameter Index", parameter.getIndex()).addDetail("Parameter Type", parameter.getType()).addDetail("Parameter Name", parameter.getName())
							.addDetail("Annotations", parameter.getAnnotations().values().stream().map(Annotation::getType).toList()).exception();
					}
					this.lookup.add(parameter);
				}
			}
			//endregion
			//region Method validation
			if (method.isAnnotatedWithAny(ANNOS)) {
				if (!this.method.is(MethodType.METHOD)) {
					throw CrashReport.create(INVALID_CATEGORY, "Annotation @Above, @AboveEqual, @Below or @BelowEqual can not be applied to constructors and static initializers").addDetail("Method", method.getName()).exception();
				}
				if (this.isNoNumber(method.getReturnType())) {
					throw CrashReport.create(INVALID_CATEGORY, "Method annotated with @Above, @AboveEqual, @Below or @BelowEqual must return a number type").addDetail("Method", signature)
						.addDetail("Return Type", method.getReturnType()).exception();
				}
				if (method.getAnnotations().values().stream().filter(annotation -> annotation.isAny(ABOVE, ABOVE_EQUAL)).count() > 1) {
					throw CrashReport.create(UNSUPPORTED_CATEGORY, "Method must not be annotated with @Above and @AboveEqual at the same time").addDetail("Method", signature)
						.addDetail("Annotations", method.getAnnotations().values().stream().map(Annotation::getType).toList()).exception();
				}
				if (method.getAnnotations().values().stream().filter(annotation -> annotation.isAny(BELOW, BELOW_EQUAL)).count() > 1) {
					throw CrashReport.create(UNSUPPORTED_CATEGORY, "Method must not be annotated with @Below and @BelowEqual at the same time").addDetail("Method", signature)
						.addDetail("Annotations", method.getAnnotations().values().stream().map(Annotation::getType).toList()).exception();
				}
			}
			//endregion
		}
		
		@Override
		public void visitCode() {
			this.mv.visitCode();
			for (Parameter parameter : this.lookup) {
				String message = parameter.getMessageName() + " must be ";
				int index = parameter.getLoadIndex();
				
				if (parameter.isAnnotatedWith(ABOVE)) {
					this.instrument(parameter.getAnnotation(ABOVE), parameter.getType(), index, true, Opcodes.IFGT, message + "above");
				}
				if (parameter.isAnnotatedWith(ABOVE_EQUAL)) {
					this.instrument(parameter.getAnnotation(ABOVE_EQUAL), parameter.getType(), index, true, Opcodes.IFGE, message + "above or equal to");
				}
				if (parameter.isAnnotatedWith(BELOW)) {
					this.instrument(parameter.getAnnotation(BELOW), parameter.getType(), index, false, Opcodes.IFGT, message + "below");
				}
				if (parameter.isAnnotatedWith(BELOW_EQUAL)) {
					this.instrument(parameter.getAnnotation(BELOW_EQUAL), parameter.getType(), index, false, Opcodes.IFGE, message + "below or equal to");
				}
			}
		}
		
		@Override
		public void visitInsn(int opcode) {
			if (isReturn(opcode) && this.method.isAnnotatedWithAny(ANNOS)) {
				Label start = new Label();
				Label end = new Label();
				Type type = this.method.getReturnType();
				
				int local = newLocal(this.mv, type);
				this.mv.visitVarInsn(type.getOpcode(Opcodes.ISTORE), local);
				this.insertLabel(start);
				
				String message = "Method " + this.method.getOwner().getClassName() + "#" + this.method.getName() + " return value must be ";
				if (this.method.isAnnotatedWith(ABOVE)) {
					this.instrument(this.method.getAnnotation(ABOVE), type, local, true, Opcodes.IFGT, message + "above");
				}
				if (this.method.isAnnotatedWith(ABOVE_EQUAL)) {
					this.instrument(this.method.getAnnotation(ABOVE_EQUAL), type, local, true, Opcodes.IFGE, message + "above or equal to");
				}
				if (this.method.isAnnotatedWith(BELOW)) {
					this.instrument(this.method.getAnnotation(BELOW), type, local, false, Opcodes.IFGT, message + "below");
				}
				if (this.method.isAnnotatedWith(BELOW_EQUAL)) {
					this.instrument(this.method.getAnnotation(BELOW_EQUAL), type, local, false, Opcodes.IFGE, message + "below or equal to");
				}
				
				this.mv.visitJumpInsn(Opcodes.GOTO, end);
				this.insertLabel(end);
				this.mv.visitVarInsn(type.getOpcode(Opcodes.ILOAD), local);
				this.visitLocalVariable(local, "generated$RangeTransformer$Temp" + local, type, null, start, end);
			}
			this.mv.visitInsn(opcode);
		}
		
		//region Helper methods
		private boolean isNoNumber(@NotNull Type type) {
			return Utils.indexOf(NUMBERS, convertToPrimitive(type)) == -1;
		}
		
		private void instrument(@NotNull Annotation annotation, @NotNull Type type, int loadIndex, boolean above, int compare, String message) {
			Label label = new Label();
			Double value = annotation.get("value");
			if (value == null) {
				return;
			}
			if (above) {
				loadNumberAsDouble(this.mv, type, loadIndex);
				loadNumber(this.mv, value);
			} else {
				loadNumber(this.mv, value);
				loadNumberAsDouble(this.mv, type, loadIndex);
			}
			this.mv.visitInsn(Opcodes.DCMPL);
			this.mv.visitJumpInsn(compare, label);
			instrumentThrownException(this.mv, ILLEGAL_ARGUMENT_EXCEPTION, message + " " + value);
			this.mv.visitJumpInsn(Opcodes.GOTO, label);
			this.insertLabel(label);
		}
		//endregion
	}
}
