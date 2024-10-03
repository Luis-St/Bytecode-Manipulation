package net.luis.agent.asm.transformer;

import net.luis.agent.Agent;
import net.luis.agent.asm.base.*;
import net.luis.agent.asm.data.Class;
import net.luis.agent.asm.data.*;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.asm.type.MethodType;
import net.luis.agent.asm.type.SignatureType;
import net.luis.agent.util.*;
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

public class MathTransformer extends BaseClassTransformer {
	
	// ToDO: Support for Math.exp(value), Math.expm1(value), Math.log(value), Math.log10(value), Math.log(base, value) (custom impl) and Math.pow(base, exponent)
	
	private static final Type[] ALL = {
		ABOVE, ABOVE_EQUAL, ABS, BELOW, BELOW_EQUAL, CLAMP, MAX, MIN, NEGATE, ROUND, TRIGONOMETRIC
	};
	
	public MathTransformer() {
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
				if (this.method.is(MethodType.STATIC_INITIALIZER)) {
					throw CrashReport.create(INVALID_CATEGORY, "Math annotations can not be applied to static initializers").addDetail("Method", method.getName()).exception();
				}
				if (this.isNoNumber(method.getReturnType())) {
					throw CrashReport.create(INVALID_CATEGORY, "Method annotated with math annotation must return a number type").addDetail("Method", signature)
						.addDetail("Return Type", method.getReturnType()).exception();
				}
				Collection<Annotation> annotations = method.getAnnotations().values();
				if (annotations.stream().filter(annotation -> annotation.isAny(ABOVE, ABOVE_EQUAL)).count() > 1) {
					throw CrashReport.create(UNSUPPORTED_CATEGORY, "Method must not be annotated with @Above and @AboveEqual at the same time").addDetail("Method", signature)
						.addDetail("Annotations", annotations.stream().map(Annotation::getType).toList()).exception();
				}
				if (annotations.stream().filter(annotation -> annotation.isAny(BELOW, BELOW_EQUAL)).count() > 1) {
					throw CrashReport.create(UNSUPPORTED_CATEGORY, "Method must not be annotated with @Below and @BelowEqual at the same time").addDetail("Method", signature)
						.addDetail("Annotations", annotations.stream().map(Annotation::getType).toList()).exception();
				}
				if (annotations.stream().filter(annotation -> annotation.isAny(MIN, MAX)).count() > 1) {
					throw CrashReport.create(UNSUPPORTED_CATEGORY, "Method must not be annotated with @Min and @Max at the same time").addDetail("Method", signature)
						.addDetail("Annotations", annotations.stream().map(Annotation::getType).toList()).addDetail("Suggestion", "Use @Clamp instead of @Min and @Max in combination").exception();
				}
				if (annotations.stream().filter(annotation -> annotation.isAny(CLAMP, MIN, MAX)).count() > 1) {
					throw CrashReport.create(UNSUPPORTED_CATEGORY, "Method must not be annotated with @Clamp and @Min or @Max at the same time").addDetail("Method", signature)
						.addDetail("Annotations", annotations.stream().map(Annotation::getType).toList()).addDetail("Suggestion", "Reconfigure the @Clamp annotation").exception();
				}
			}
			//endregion
			//region Parameter validation
			for (Parameter parameter : method.getParameters().values()) {
				if (parameter.isAnnotatedWithAny(ALL)) {
					if (this.isNoNumber(parameter.getType())) {
						throw CrashReport.create(UNSUPPORTED_CATEGORY, "Parameter annotated with math annotation must be a number type").addDetail("Method", signature)
							.addDetail("Parameter Index", parameter.getIndex()).addDetail("Parameter Type", parameter.getType()).addDetail("Parameter Name", parameter.getName()).exception();
					}
					Collection<Annotation> annotations = parameter.getAnnotations().values();
					if (annotations.stream().filter(annotation -> annotation.isAny(ABOVE, ABOVE_EQUAL)).count() > 1) {
						throw CrashReport.create(UNSUPPORTED_CATEGORY, "Parameter must not be annotated with @Above and @AboveEqual at the same time").addDetail("Method", signature)
							.addDetail("Parameter Index", parameter.getIndex()).addDetail("Parameter Type", parameter.getType()).addDetail("Parameter Name", parameter.getName())
							.addDetail("Annotations", annotations.stream().map(Annotation::getType).toList()).exception();
					}
					if (annotations.stream().filter(annotation -> annotation.isAny(BELOW, BELOW_EQUAL)).count() > 1) {
						throw CrashReport.create(UNSUPPORTED_CATEGORY, "Parameter must not be annotated with @Below and @BelowEqual at the same time").addDetail("Method", signature)
							.addDetail("Parameter Index", parameter.getIndex()).addDetail("Parameter Type", parameter.getType()).addDetail("Parameter Name", parameter.getName())
							.addDetail("Annotations", annotations.stream().map(Annotation::getType).toList()).exception();
					}
					if (annotations.stream().filter(annotation -> annotation.isAny(MIN, MAX)).count() > 1) {
						throw CrashReport.create(UNSUPPORTED_CATEGORY, "Parameter must not be annotated with @Min and @Max at the same time").addDetail("Method", signature)
							.addDetail("Parameter Index", parameter.getIndex()).addDetail("Parameter Type", parameter.getType()).addDetail("Parameter Name", parameter.getName())
							.addDetail("Annotations", annotations.stream().map(Annotation::getType).toList()).addDetail("Suggestion", "Use @Clamp instead of @Min and @Max in combination").exception();
					}
					if (annotations.stream().filter(annotation -> annotation.isAny(CLAMP, MIN, MAX)).count() > 1) {
						throw CrashReport.create(UNSUPPORTED_CATEGORY, "Parameter must not be annotated with @Clamp and @Min or @Max at the same time").addDetail("Method", signature)
							.addDetail("Parameter Index", parameter.getIndex()).addDetail("Parameter Type", parameter.getType()).addDetail("Parameter Name", parameter.getName())
							.addDetail("Annotations", annotations.stream().map(Annotation::getType).toList()).addDetail("Suggestion", "Reconfigure the @Clamp annotation").exception();
					}
					this.lookup.add(parameter);
				}
			}
			//endregion
			//region Field validation
			for (Field field : Agent.getClass(method.getOwner()).getFields().values()) {
				if (field.isAnnotatedWithAny(ALL)) {
					if (this.isNoNumber(field.getType())) {
						throw CrashReport.create(UNSUPPORTED_CATEGORY, "Field annotated with math annotation must be a number type").addDetail("Method", signature)
							.addDetail("Field Name", field.getName()).addDetail("Field Type", field.getType()).exception();
					}
					Collection<Annotation> annotations = field.getAnnotations().values();
					if (annotations.stream().filter(annotation -> annotation.isAny(ABOVE, ABOVE_EQUAL)).count() > 1) {
						throw CrashReport.create(UNSUPPORTED_CATEGORY, "Field must not be annotated with @Above and @AboveEqual at the same time").addDetail("Method", signature)
							.addDetail("Field Name", field.getName()).addDetail("Field Type", field.getType()).addDetail("Annotations", annotations.stream().map(Annotation::getType).toList()).exception();
					}
					if (annotations.stream().filter(annotation -> annotation.isAny(BELOW, BELOW_EQUAL)).count() > 1) {
						throw CrashReport.create(UNSUPPORTED_CATEGORY, "Field must not be annotated with @Below and @BelowEqual at the same time").addDetail("Method", signature)
							.addDetail("Field Name", field.getName()).addDetail("Field Type", field.getType()).addDetail("Annotations", annotations.stream().map(Annotation::getType).toList()).exception();
					}
					if (annotations.stream().filter(annotation -> annotation.isAny(MIN, MAX)).count() > 1) {
						throw CrashReport.create(UNSUPPORTED_CATEGORY, "Field must not be annotated with @Min and @Max at the same time").addDetail("Method", signature)
							.addDetail("Field Name", field.getName()).addDetail("Field Type", field.getType()).addDetail("Annotations", annotations.stream().map(Annotation::getType).toList())
							.addDetail("Suggestion", "Use @Clamp instead of @Min and @Max in combination").exception();
					}
					if (annotations.stream().filter(annotation -> annotation.isAny(CLAMP, MIN, MAX)).count() > 1) {
						throw CrashReport.create(UNSUPPORTED_CATEGORY, "Field must not be annotated with @Clamp and @Min or @Max at the same time").addDetail("Method", signature)
							.addDetail("Field Name", field.getName()).addDetail("Field Type", field.getType()).addDetail("Annotations", annotations.stream().map(Annotation::getType).toList())
							.addDetail("Suggestion", "Reconfigure the @Clamp annotation").exception();
					}
				}
			}
			//endregion
			//region Local variable validation
			for (LocalVariable local : method.getLocals()) {
				if (local.isAnnotatedWithAny(ALL)) {
					if (this.isNoNumber(local.getType())) {
						throw CrashReport.create(UNSUPPORTED_CATEGORY, "Local variable annotated with math annotation must be a number type").addDetail("Method", signature)
							.addDetail("Local Index", local.getIndex()).addDetail("Local Name", local.getName()).addDetail("Local Type", local.getType()).exception();
					}
					Collection<Annotation> annotations = local.getAnnotations().values();
					if (annotations.stream().filter(annotation -> annotation.isAny(ABOVE, ABOVE_EQUAL)).count() > 1) {
						throw CrashReport.create(UNSUPPORTED_CATEGORY, "Local variable must not be annotated with @Above and @AboveEqual at the same time").addDetail("Method", signature).addDetail("Local Index", local.getIndex())
							.addDetail("Local Name", local.getName()).addDetail("Local Type", local.getType()).addDetail("Annotations", annotations.stream().map(Annotation::getType).toList()).exception();
					}
					if (annotations.stream().filter(annotation -> annotation.isAny(BELOW, BELOW_EQUAL)).count() > 1) {
						throw CrashReport.create(UNSUPPORTED_CATEGORY, "Local variable must not be annotated with @Below and @BelowEqual at the same time").addDetail("Method", signature).addDetail("Local Index", local.getIndex())
							.addDetail("Local Name", local.getName()).addDetail("Local Type", local.getType()).addDetail("Annotations", annotations.stream().map(Annotation::getType).toList()).exception();
					}
					if (annotations.stream().filter(annotation -> annotation.isAny(MIN, MAX)).count() > 1) {
						throw CrashReport.create(UNSUPPORTED_CATEGORY, "Local variable must not be annotated with @Min and @Max at the same time").addDetail("Method", signature).addDetail("Local Index", local.getIndex())
							.addDetail("Local Name", local.getName()).addDetail("Local Type", local.getType()).addDetail("Annotations", annotations.stream().map(Annotation::getType).toList())
							.addDetail("Suggestion", "Use @Clamp instead of @Min and @Max in combination").exception();
					}
					if (annotations.stream().filter(annotation -> annotation.isAny(CLAMP, MIN, MAX)).count() > 1) {
						throw CrashReport.create(UNSUPPORTED_CATEGORY, "Local variable must not be annotated with @Clamp and @Min or @Max at the same time").addDetail("Method", signature).addDetail("Local Index", local.getIndex())
							.addDetail("Local Name", local.getName()).addDetail("Local Type", local.getType()).addDetail("Annotations", annotations.stream().map(Annotation::getType).toList())
							.addDetail("Suggestion", "Reconfigure the @Clamp annotation").exception();
					}
				}
			}
			//endregion
		}
		
		@Override
		public void visitCode() {
			this.mv.visitCode();
			for (Parameter parameter : this.lookup) {
				this.instrumentModifications(parameter.getAnnotations(), parameter.getType(), parameter.getLoadIndex());
				this.instrumentConditions(parameter.getAnnotations(), parameter.getType(), parameter.getLoadIndex(), parameter.getMessageName() + " must be ");
			}
		}
		
		@Override
		public void visitVarInsn(int opcode, int index) {
			super.visitVarInsn(opcode, index);
			if (this.includeLocals && isStore(opcode) && this.method.isLocal(index)) {
				this.method.getLocals(index).stream().filter(l -> l.isAnnotatedWithAny(ALL)).filter(l -> l.isInScope(this.getScopeIndex())).findFirst().ifPresent(local -> {
					this.instrumentModifications(local.getAnnotations(), local.getType(), index);
					this.instrumentConditions(local.getAnnotations(), local.getType(), index, local.getMessageName() + " must be ");
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
					
					this.instrumentModifications(field.getAnnotations(), field.getType(), local);
					this.instrumentConditions(field.getAnnotations(), field.getType(), local, "Field " + field.getName() + " must be ");
					
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
				
				this.instrumentConditions(this.method.getAnnotations(), type, local, "Method return value must be ");
				
				this.mv.visitJumpInsn(Opcodes.GOTO, end);
				this.insertLabel(end);
				this.mv.visitVarInsn(type.getOpcode(Opcodes.ILOAD), local);
				this.visitLocalVariable(local, "generated$RangeTransformer$Temp" + local, type, null, start, end);
			}
			this.mv.visitInsn(opcode);
		}
		
		//region Instrumentation
		private void instrumentModifications(@NotNull Map<Type, Annotation> annotations, @NotNull Type type, int index) {
			if (annotations.containsKey(ROUND)) {
				this.instrumentRound(annotations.get(TRIGONOMETRIC), type, index);
			}
			if (annotations.containsKey(TRIGONOMETRIC)) {
				this.instrumentTrigonometric(annotations.get(TRIGONOMETRIC), type, index);
			}
			if (annotations.containsKey(ABS)) {
				this.instrumentAbs(type, index);
			}
			if (annotations.containsKey(NEGATE)) {
				this.instrumentNegate(type, index);
			}
			if (annotations.containsKey(CLAMP)) {
				this.instrumentClamp(annotations.get(CLAMP), type, index);
			}
			if (annotations.containsKey(MIN)) {
				this.instrumentMin(annotations.get(MIN), type, index);
			}
			if (annotations.containsKey(MAX)) {
				this.instrumentMax(annotations.get(MAX), type, index);
			}
		}
		
		private void instrumentConditions(@NotNull Map<Type, Annotation> annotations, @NotNull Type type, int index, @NotNull String baseMessage) {
			if (annotations.containsKey(ABOVE)) {
				this.instrumentRange(annotations.get(ABOVE), type, index, true, Opcodes.IFGT, baseMessage + "above");
			}
			if (annotations.containsKey(ABOVE_EQUAL)) {
				this.instrumentRange(annotations.get(ABOVE_EQUAL), type, index, true, Opcodes.IFGE, baseMessage + "above or equal to");
			}
			if (annotations.containsKey(BELOW)) {
				this.instrumentRange(annotations.get(BELOW), type, index, false, Opcodes.IFGT, baseMessage + "below");
			}
			if (annotations.containsKey(BELOW_EQUAL)) {
				this.instrumentRange(annotations.get(BELOW_EQUAL), type, index, false, Opcodes.IFGE, baseMessage + "below or equal to");
			}
		}
		//endregion
		
		//region Instrumentation modifications
		private void instrumentRound(@NotNull Annotation annotation, @NotNull Type type, int index) {
			RoundingMode mode = RoundingMode.valueOf(annotation.getOrDefault("mode"));
			if (mode.requiresFloatingPointInput()) {
				loadNumberAsDouble(this.mv, type, index);
			} else {
				loadNumberAsLong(this.mv, type, index);
			}
			long value = annotation.getOrDefault("value");
			Type resultType = DOUBLE;
			if (mode == RoundingMode.FLOOR || mode == RoundingMode.CEIL) {
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, MATH.getInternalName(), mode.getMethodName(), "(D)D", false);
			} else if (mode == RoundingMode.ROUND) {
				loadNumber(this.mv, (int) value);
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, RUNTIME_UTILS.getInternalName(), mode.getMethodName(), "(DI)D", false);
			} else {
				loadNumber(this.mv, value);
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, MATH.getInternalName(), mode.getMethodName(), "(JJ)J", false);
				resultType = LONG;
			}
			instrumentNumberConversion(this.mv, resultType, type);
			this.mv.visitVarInsn(type.getOpcode(Opcodes.ISTORE), index);
		}
		
		private void instrumentTrigonometric(@NotNull Annotation annotation, @NotNull Type type, int index) {
			TrigonometricOperation operation = TrigonometricOperation.valueOf(annotation.getOrDefault("value"));
			loadNumberAsDouble(this.mv, type, index);
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, MATH.getInternalName(), operation.name().toLowerCase(), "(D)D", false);
			instrumentNumberConversion(this.mv, DOUBLE, type);
			this.mv.visitVarInsn(type.getOpcode(Opcodes.ISTORE), index);
		}
		
		private void instrumentAbs(@NotNull Type type, int index) {
			Type primitiveType = convertToPrimitive(type);
			this.instrumentNumberConversionToDefaultPrimitive(type, index);
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, MATH.getInternalName(), "abs", "(" + primitiveType.getDescriptor() + ")" + primitiveType.getDescriptor(), false);
			instrumentNumberConversion(this.mv, primitiveType, type);
			this.mv.visitVarInsn(type.getOpcode(Opcodes.ISTORE), index);
		}
		
		private void instrumentNegate(@NotNull Type type, int index) {
			Type primitiveType = convertToPrimitive(type);
			this.instrumentNumberConversionToDefaultPrimitive(type, index);
			if (primitiveType.equals(LONG)) {
				this.mv.visitInsn(Opcodes.LNEG);
			} else if (primitiveType.equals(FLOAT)) {
				this.mv.visitInsn(Opcodes.FNEG);
			} else if (primitiveType.equals(DOUBLE)) {
				this.mv.visitInsn(Opcodes.DNEG);
			} else {
				this.mv.visitInsn(Opcodes.INEG);
			}
			instrumentNumberConversion(this.mv, primitiveType, type);
			this.mv.visitVarInsn(type.getOpcode(Opcodes.ISTORE), index);
		}
		
		private void instrumentClamp(@NotNull Annotation annotation, @NotNull Type type, int index) {
			Type primitiveType = convertToPrimitive(type);
			this.instrumentNumberConversionToDefaultPrimitive(type, index);
			if (primitiveType.equals(LONG)) {
				// ToDo: Load the min and max values from annotation
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, MATH.getInternalName(), "clamp", "(JJJ)J", false);
			} else if (primitiveType.equals(FLOAT)) {
				// ToDo: Load the min and max values from annotation
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, MATH.getInternalName(), "clamp", "(FFF)F", false);
			} else if (primitiveType.equals(DOUBLE)) {
				// ToDo: Load the min and max values from annotation
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, MATH.getInternalName(), "clamp", "(DDD)D", false);
			} else {
				this.mv.visitInsn(Opcodes.I2L);
				// ToDo: Load the min and max values from annotation
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, MATH.getInternalName(), "clamp", "(JII)I", false);
			}
			instrumentNumberConversion(this.mv, primitiveType, type);
			this.mv.visitVarInsn(type.getOpcode(Opcodes.ISTORE), index);
		}
		
		private void instrumentMin(@NotNull Annotation annotation, @NotNull Type type, int index) {
			double min = annotation.getOrDefault("min");
			Type primitiveType = convertToPrimitive(type);
			this.instrumentNumberConversionToDefaultPrimitive(type, index);
			if (primitiveType.equals(LONG)) {
				loadNumber(this.mv, (long) min);
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, MATH.getInternalName(), "min", "(JJ)J", false);
			} else if (primitiveType.equals(FLOAT)) {
				loadNumber(this.mv, (float) min);
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, MATH.getInternalName(), "min", "(FF)F", false);
			} else if (primitiveType.equals(DOUBLE)) {
				loadNumber(this.mv, min);
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, MATH.getInternalName(), "min", "(DD)D", false);
			} else {
				this.mv.visitInsn(Opcodes.I2L);
				loadNumber(this.mv, (int) min);
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, MATH.getInternalName(), "min", "(II)I", false);
			}
			instrumentNumberConversion(this.mv, primitiveType, type);
			this.mv.visitVarInsn(type.getOpcode(Opcodes.ISTORE), index);
		}
		
		private void instrumentMax(@NotNull Annotation annotation, @NotNull Type type, int index) {
			double max = annotation.getOrDefault("max");
			Type primitiveType = convertToPrimitive(type);
			this.instrumentNumberConversionToDefaultPrimitive(type, index);
			if (primitiveType.equals(LONG)) {
				loadNumber(this.mv, (long) max);
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, MATH.getInternalName(), "max", "(JJ)J", false);
			} else if (primitiveType.equals(FLOAT)) {
				loadNumber(this.mv, (float) max);
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, MATH.getInternalName(), "max", "(FF)F", false);
			} else if (primitiveType.equals(DOUBLE)) {
				loadNumber(this.mv, max);
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, MATH.getInternalName(), "max", "(DD)D", false);
			} else {
				this.mv.visitInsn(Opcodes.I2L);
				loadNumber(this.mv, (int) max);
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, MATH.getInternalName(), "max", "(II)I", false);
			}
			instrumentNumberConversion(this.mv, primitiveType, type);
			this.mv.visitVarInsn(type.getOpcode(Opcodes.ISTORE), index);
		}
		//endregion
		
		//region Instrumentation conditions
		private void instrumentRange(@NotNull Annotation annotation, @NotNull Type type, int index, boolean above, int compare, @NotNull String message) {
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
			instrumentThrownException(this.mv, ILLEGAL_ARGUMENT_EXCEPTION, () -> this.instrumentRangeMessage(index, type, message, value));
			this.mv.visitJumpInsn(Opcodes.GOTO, label);
			this.insertLabel(label);
		}
		
		private void instrumentRangeMessage(int index, @NotNull Type type, @NotNull String baseMessage, @NotNull Double value) {
			String valueMessage = type.equals(FLOAT) || type.equals(DOUBLE) ? String.valueOf(value) : String.valueOf(value.intValue());
			this.mv.visitVarInsn(type.getOpcode(Opcodes.ILOAD), index);
			this.mv.visitInvokeDynamicInsn("makeConcatWithConstants", "(" + type.getDescriptor() + ")Ljava/lang/String;", STRING_CONCAT_HANDLE, baseMessage + " " + valueMessage + ", but was '\u0001'");
		}
		//endregion
		
		//region Helper methods
		private boolean isNoNumber(@NotNull Type type) {
			return Utils.indexOf(NUMBERS, convertToPrimitive(type)) == -1;
		}
		
		private void instrumentNumberConversionToDefaultPrimitive(@NotNull Type type, int index) {
			if (isWrapper(type)) {
				Type targetPrimitiveType = convertToPrimitive(type);
				if (targetPrimitiveType.equals(BYTE) || targetPrimitiveType.equals(SHORT)) {
					targetPrimitiveType = INT;
				}
				instrumentNumberConversion(this.mv, type, targetPrimitiveType);
			}
		}
		//endregion
	}
}
