package net.luis.agent.asm.transformer;

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

import java.util.*;

import static net.luis.agent.asm.Instrumentations.*;
import static net.luis.agent.asm.Types.*;

/**
 *
 * @author Luis-St
 *
 */

public class RangeTransformer extends BaseClassTransformer {
	
	private static final Type[] ALL = { ABOVE, ABOVE_EQUAL, BELOW, BELOW_EQUAL };
	
	public RangeTransformer() {
		super(true);
	}
	
	//region Type filtering
	@Override
	protected boolean shouldIgnoreClass(@NotNull Type type) {
		Class clazz = Agent.getClass(type);
		return clazz.getMethods().values().stream().noneMatch(method -> method.isAnnotatedWithAny(ALL)) && clazz.getParameters().stream().noneMatch(parameter -> parameter.isAnnotatedWithAny(ALL));
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
				if (method.isAnnotatedWithAny(ALL)) {
					return true;
				}
				Class clazz = Agent.getClass(method.getOwner());
				if (clazz.getFields().values().stream().anyMatch(field -> field.isAnnotatedWithAny(ALL))) {
					return true;
				}
				if (method.getParameters().values().stream().anyMatch(parameter -> parameter.isAnnotatedWithAny(ALL))) {
					return true;
				}
				return method.getLocals().stream().anyMatch(local -> local.isAnnotatedWithAny(ALL));
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
		private final boolean includeLocals;
		
		private RangeVisitor(@NotNull MethodVisitor visitor, @NotNull Method method) {
			super(visitor);
			this.method = method;
			this.includeLocals = method.getLocals().stream().anyMatch(local -> local.isAnnotatedWithAny(ALL));
			//region Method validation
			String signature = method.getSignature(SignatureType.DEBUG);
			if (method.isAnnotatedWithAny(ALL)) {
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
			//region Parameter validation
			for (Parameter parameter : method.getParameters().values()) {
				if (parameter.isAnnotatedWithAny(ALL)) {
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
			//region Field validation
			for (Field field : Agent.getClass(method.getOwner()).getFields().values()) {
				if (field.isAnnotatedWithAny(ALL)) {
					if (this.isNoNumber(field.getType())) {
						throw CrashReport.create(UNSUPPORTED_CATEGORY, "Field annotated with @Above, @AboveEqual, @Below or @BelowEqual must be a number type").addDetail("Method", signature)
							.addDetail("Field Name", field.getName()).addDetail("Field Type", field.getType()).exception();
					}
					if (field.getAnnotations().values().stream().filter(annotation -> annotation.isAny(ABOVE, ABOVE_EQUAL)).count() > 1) {
						throw CrashReport.create(UNSUPPORTED_CATEGORY, "Field must not be annotated with @Above and @AboveEqual at the same time").addDetail("Method", signature)
							.addDetail("Field Name", field.getName()).addDetail("Field Type", field.getType()).addDetail("Annotations", field.getAnnotations().values().stream().map(Annotation::getType).toList()).exception();
					}
					if (field.getAnnotations().values().stream().filter(annotation -> annotation.isAny(BELOW, BELOW_EQUAL)).count() > 1) {
						throw CrashReport.create(UNSUPPORTED_CATEGORY, "Field must not be annotated with @Below and @BelowEqual at the same time").addDetail("Method", signature)
							.addDetail("Field Name", field.getName()).addDetail("Field Type", field.getType()).addDetail("Annotations", field.getAnnotations().values().stream().map(Annotation::getType).toList()).exception();
					}
				}
			}
			//endregion
			//region Local variable validation
			for (LocalVariable local : method.getLocals()) {
				if (local.isAnnotatedWithAny(ALL)) {
					if (this.isNoNumber(local.getType())) {
						throw CrashReport.create(UNSUPPORTED_CATEGORY, "Local variable annotated with @Above, @AboveEqual, @Below or @BelowEqual must be a number type").addDetail("Method", signature)
							.addDetail("Local Index", local.getIndex()).addDetail("Local Name", local.getName()).addDetail("Local Type", local.getType()).exception();
					}
					if (local.getAnnotations().values().stream().filter(annotation -> annotation.isAny(ABOVE, ABOVE_EQUAL)).count() > 1) {
						throw CrashReport.create(UNSUPPORTED_CATEGORY, "Local variable must not be annotated with @Above and @AboveEqual at the same time").addDetail("Method", signature).addDetail("Local Index", local.getIndex())
							.addDetail("Local Name", local.getName()).addDetail("Local Type", local.getType()).addDetail("Annotations", local.getAnnotations().values().stream().map(Annotation::getType).toList()).exception();
					}
					if (local.getAnnotations().values().stream().filter(annotation -> annotation.isAny(BELOW, BELOW_EQUAL)).count() > 1) {
						throw CrashReport.create(UNSUPPORTED_CATEGORY, "Local variable must not be annotated with @Below and @BelowEqual at the same time").addDetail("Method", signature).addDetail("Local Index", local.getIndex())
							.addDetail("Local Name", local.getName()).addDetail("Local Type", local.getType()).addDetail("Annotations", local.getAnnotations().values().stream().map(Annotation::getType).toList()).exception();
					}
				}
			}
			//endregion
		}
		
		@Override
		public void visitCode() {
			this.mv.visitCode();
			for (Parameter parameter : this.lookup) {
				this.instrument(parameter, parameter.getType(), parameter.getLoadIndex(), parameter.getMessageName() + " must be ");
			}
		}
		
		@Override
		public void visitVarInsn(int opcode, int index) {
			super.visitVarInsn(opcode, index);
			if (this.includeLocals && isStore(opcode) && this.method.isLocal(index)) {
				this.method.getLocals(index).stream().filter(l -> l.isAnnotatedWithAny(ALL)).filter(l -> l.isInScope(this.getScopeIndex())).findFirst().ifPresent(local -> {
					this.instrument(local, local.getType(), index, local.getMessageName() + " must be ");
				});
			}
		}
		
		@Override
		public void visitFieldInsn(int opcode, @NotNull String owner, @NotNull String name, @NotNull String descriptor) {
			if (opcode == Opcodes.PUTFIELD || opcode == Opcodes.PUTSTATIC) {
				Field field = Agent.getClass(this.method.getOwner()).getField(name);
				if (field != null && field.isAnnotatedWithAny(ALL)) {
					int local = newLocal(this.mv, field.getType());
					Label start = new Label();
					Label end = new Label();
					this.mv.visitVarInsn(Opcodes.ASTORE, local);
					this.insertLabel(start);
					
					this.instrument(field, field.getType(), local, "Field " + field.getName() + " must be ");
					
					this.mv.visitVarInsn(Opcodes.ALOAD, local);
					this.insertLabel(end);
					this.visitLocalVariable(local, "generated$RangeTransformer$Temp" + local, STRING, null, start, end);
				}
			}
			super.visitFieldInsn(opcode, owner, name, descriptor);
		}
		
		@Override
		public void visitInsn(int opcode) {
			if (isReturn(opcode) && this.method.isAnnotatedWithAny(ALL)) {
				Label start = new Label();
				Label end = new Label();
				Type type = this.method.getReturnType();
				
				int local = newLocal(this.mv, type);
				this.mv.visitVarInsn(type.getOpcode(Opcodes.ISTORE), local);
				this.insertLabel(start);
				
				this.instrument(this.method, type, local, "Method return value must be ");
				
				this.mv.visitJumpInsn(Opcodes.GOTO, end);
				this.insertLabel(end);
				this.mv.visitVarInsn(type.getOpcode(Opcodes.ILOAD), local);
				this.visitLocalVariable(local, "generated$RangeTransformer$Temp" + local, type, null, start, end);
			}
			this.mv.visitInsn(opcode);
		}
		
		//region Instrumentation
		private void instrument(@NotNull ASMData data, @NotNull Type type, int index, String baseMessage) {
			if (data.isAnnotatedWith(ABOVE)) {
				this.instrument(data.getAnnotation(ABOVE), type, index, true, Opcodes.IFGT, baseMessage + "above");
			}
			if (data.isAnnotatedWith(ABOVE_EQUAL)) {
				this.instrument(data.getAnnotation(ABOVE_EQUAL), type, index, true, Opcodes.IFGE, baseMessage + "above or equal to");
			}
			if (data.isAnnotatedWith(BELOW)) {
				this.instrument(data.getAnnotation(BELOW), type, index, false, Opcodes.IFGT, baseMessage + "below");
			}
			if (data.isAnnotatedWith(BELOW_EQUAL)) {
				this.instrument(data.getAnnotation(BELOW_EQUAL), type, index, false, Opcodes.IFGE, baseMessage + "below or equal to");
			}
		}
		
		private void instrument(@NotNull Annotation annotation, @NotNull Type type, int index, boolean above, int compare, String message) {
			Label label = new Label();
			Double value = annotation.get("value");
			if (value == null) {
				return;
			}
			if (above) {
				loadNumberAsDouble(this.mv, type, index);
				loadNumber(this.mv, value);
			} else {
				loadNumber(this.mv, value);
				loadNumberAsDouble(this.mv, type, index);
			}
			this.mv.visitInsn(Opcodes.DCMPL);
			this.mv.visitJumpInsn(compare, label);
			instrumentThrownException(this.mv, ILLEGAL_ARGUMENT_EXCEPTION, /*message + " " + value */() -> this.instrumentMessage(index, type, message, value));
			this.mv.visitJumpInsn(Opcodes.GOTO, label);
			this.insertLabel(label);
		}
		
		private void instrumentMessage(int index, @NotNull Type type, @NotNull String baseMessage, @NotNull Double value) {
			String valueMessage = type.equals(FLOAT) || type.equals(DOUBLE) ? String.valueOf(value) : String.valueOf(value.intValue());
			this.mv.visitVarInsn(type.getOpcode(Opcodes.ILOAD), index);
			this.mv.visitInvokeDynamicInsn("makeConcatWithConstants", "(" + type.getDescriptor() + ")Ljava/lang/String;", STRING_CONCAT_HANDLE, baseMessage + " " + valueMessage + ", but was '\u0001'");
		}
		//endregion
		
		//region Helper methods
		private boolean isNoNumber(@NotNull Type type) {
			return Utils.indexOf(NUMBERS, convertToPrimitive(type)) == -1;
		}
		//endregion
	}
}
