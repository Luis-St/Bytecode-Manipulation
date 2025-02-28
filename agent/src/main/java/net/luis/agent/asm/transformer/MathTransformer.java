package net.luis.agent.asm.transformer;

import net.luis.agent.Agent;
import net.luis.agent.asm.base.*;
import net.luis.agent.asm.data.*;
import net.luis.agent.asm.data.Class;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.asm.type.MethodType;
import net.luis.agent.asm.type.SignatureType;
import net.luis.agent.util.*;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.util.*;
import java.util.stream.Stream;

import static net.luis.agent.asm.Instrumentations.*;
import static net.luis.agent.asm.Types.*;

/**
 *
 * @author Luis-St
 *
 */

public class MathTransformer extends BaseClassTransformer {
	
	private static final Type[] CONDITIONS = {
		ABOVE, ABOVE_EQUAL, BELOW, BELOW_EQUAL
	};
	private static final Type[] MODIFICATIONS = {
		ABS, CLAMP, EXP, LOG, MAX, MIN, NEGATE, POW, ROUND, TRIG
	};
	private static final Type[] ALL = Stream.concat(Arrays.stream(CONDITIONS), Arrays.stream(MODIFICATIONS)).toArray(Type[]::new);
	
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
				return new MathVisitor(visitor, method);
			}
		};
	}
	
	private static class MathVisitor extends LabelTrackingMethodVisitor {
		
		private static final String INVALID_ELEMENT_CATEGORY = "Invalid Annotated Element";
		private static final String INVALID_CONFIGURATION_CATEGORY = "Invalid Annotation Configuration";
		private static final String UNSUPPORTED_CATEGORY = "Unsupported Annotation Combination";
		
		private final List<Parameter> lookup = new ArrayList<>();
		private final boolean includeLocals;
		
		private MathVisitor(@NotNull MethodVisitor visitor, @NotNull Method method) {
			super(visitor);
			this.method = method;
			this.includeLocals = method.getLocals().stream().anyMatch(local -> local.isAnnotatedWithAny(ALL));
			
			//region Method validation
			String signature = method.getSignature(SignatureType.DEBUG);
			CrashReport report = CrashReport.create(UNSUPPORTED_CATEGORY).addDetail("Method", signature);
			
			if (method.isAnnotatedWithAny(ALL)) {
				if (this.method.is(MethodType.STATIC_INITIALIZER)) {
					throw CrashReport.create(INVALID_ELEMENT_CATEGORY, "Math annotations must not be applied to static initializers").addDetail("Method", method.getName()).exception();
				}
				if (this.isNoNumber(method.getReturnType())) {
					throw CrashReport.create(INVALID_ELEMENT_CATEGORY, "Method annotated with math annotation must return a number type").addDetail("Method", signature).addDetail("Return Type", method.getReturnType()).exception();
				}
				
				Collection<Annotation> annotations = method.getAnnotations().values();
				report.addDetail("Annotations", annotations.stream().map(Annotation::getType).toList());
				
				if (annotations.stream().filter(annotation -> annotation.isAny(ABOVE, ABOVE_EQUAL)).count() > 1) {
					throw report.exception("Method must not be annotated with @Above and @AboveEqual at the same time");
				}
				if (annotations.stream().filter(annotation -> annotation.isAny(BELOW, BELOW_EQUAL)).count() > 1) {
					throw report.exception("Method must not be annotated with @Below and @BelowEqual at the same time");
				}
				if (annotations.stream().filter(annotation -> annotation.isAny(MIN, MAX)).count() > 1) {
					throw report.addDetail("Suggestion", "Use @Clamp instead of @Min and @Max in combination").exception("Method must not be annotated with @Min and @Max at the same time");
				}
				if (annotations.stream().filter(annotation -> annotation.isAny(CLAMP, MIN, MAX)).count() > 1) {
					throw report.addDetail("Suggestion", "Reconfigure the @Clamp annotation").exception("Method must not be annotated with @Clamp and @Min or @Max at the same time");
				}
			}
			//endregion
			
			//region Parameter validation
			report.removeDetail("Annotations");
			for (Parameter parameter : method.getParameters().values()) {
				if (parameter.isAnnotatedWithAny(ALL)) {
					report.addParameterDetails(parameter);
					
					if (this.isNoNumber(parameter.getType())) {
						throw report.exception("Parameter annotated with math annotation must be a number type");
					}
					
					Collection<Annotation> annotations = parameter.getAnnotations().values();
					report.addDetail("Annotations", annotations.stream().map(Annotation::getType).toList());
					
					if (annotations.stream().filter(annotation -> annotation.isAny(ABOVE, ABOVE_EQUAL)).count() > 1) {
						throw report.exception("Parameter must not be annotated with @Above and @AboveEqual at the same time");
					}
					if (annotations.stream().filter(annotation -> annotation.isAny(BELOW, BELOW_EQUAL)).count() > 1) {
						throw report.exception("Parameter must not be annotated with @Below and @BelowEqual at the same time");
					}
					if (annotations.stream().filter(annotation -> annotation.isAny(MIN, MAX)).count() > 1) {
						throw report.addDetail("Suggestion", "Use @Clamp instead of @Min and @Max in combination").exception("Parameter must not be annotated with @Min and @Max at the same time");
					}
					if (annotations.stream().filter(annotation -> annotation.isAny(CLAMP, MIN, MAX)).count() > 1) {
						throw report.addDetail("Suggestion", "Reconfigure the @Clamp annotation").exception("Parameter must not be annotated with @Clamp and @Min or @Max at the same time");
					}
					this.lookup.add(parameter);
				}
			}
			//endregion
			
			//region Field validation
			report.removeParameterDetails().removeDetail("Annotations");
			for (Field field : Agent.getClass(method.getOwner()).getFields().values()) {
				if (field.isAnnotatedWithAny(ALL)) {
					report.addFieldDetails(field);
					
					if (this.isNoNumber(field.getType())) {
						throw report.exception("Field annotated with math annotation must be a number type");
					}
					
					Collection<Annotation> annotations = field.getAnnotations().values();
					report.addDetail("Annotations", annotations.stream().map(Annotation::getType).toList());
					
					if (annotations.stream().filter(annotation -> annotation.isAny(ABOVE, ABOVE_EQUAL)).count() > 1) {
						throw report.exception("Field must not be annotated with @Above and @AboveEqual at the same time");
					}
					if (annotations.stream().filter(annotation -> annotation.isAny(BELOW, BELOW_EQUAL)).count() > 1) {
						throw report.exception("Field must not be annotated with @Below and @BelowEqual at the same time");
					}
					if (annotations.stream().filter(annotation -> annotation.isAny(MIN, MAX)).count() > 1) {
						throw report.addDetail("Suggestion", "Use @Clamp instead of @Min and @Max in combination").exception("Field must not be annotated with @Min and @Max at the same time");
					}
					if (annotations.stream().filter(annotation -> annotation.isAny(CLAMP, MIN, MAX)).count() > 1) {
						throw report.addDetail("Suggestion", "Reconfigure the @Clamp annotation").exception("Field must not be annotated with @Clamp and @Min or @Max at the same time");
					}
				}
			}
			//endregion
			
			//region Local variable validation
			report.removeFieldDetails().removeDetail("Annotations");
			for (LocalVariable local : method.getLocals()) {
				if (local.isAnnotatedWithAny(ALL)) {
					report.addLocalDetails(local);
					
					if (this.isNoNumber(local.getType())) {
						throw report.exception("Local variable annotated with math annotation must be a number type");
					}
					
					Collection<Annotation> annotations = local.getAnnotations().values();
					report.addDetail("Annotations", annotations.stream().map(Annotation::getType).toList());
					
					if (annotations.stream().filter(annotation -> annotation.isAny(ABOVE, ABOVE_EQUAL)).count() > 1) {
						throw report.exception("Local variable must not be annotated with @Above and @AboveEqual at the same time");
					}
					if (annotations.stream().filter(annotation -> annotation.isAny(BELOW, BELOW_EQUAL)).count() > 1) {
						throw report.exception("Local variable must not be annotated with @Below and @BelowEqual at the same time");
					}
					if (annotations.stream().filter(annotation -> annotation.isAny(MIN, MAX)).count() > 1) {
						throw report.addDetail("Suggestion", "Use @Clamp instead of @Min and @Max in combination").exception("Local variable must not be annotated with @Min and @Max at the same time");
					}
					if (annotations.stream().filter(annotation -> annotation.isAny(CLAMP, MIN, MAX)).count() > 1) {
						throw report.addDetail("Suggestion", "Reconfigure the @Clamp annotation").exception("Local variable must not be annotated with @Clamp and @Min or @Max at the same time");
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
					this.visitLocalVariable(local, "generated$MathTransformer$Temp" + local, STRING, null, start, end);
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
				
				this.instrumentModifications(this.method.getAnnotations(), type, local);
				this.instrumentConditions(this.method.getAnnotations(), type, local, "Method return value must be ");
				
				this.mv.visitJumpInsn(Opcodes.GOTO, end);
				this.insertLabel(end);
				this.mv.visitVarInsn(type.getOpcode(Opcodes.ILOAD), local);
				this.visitLocalVariable(local, "generated$MathTransformer$Temp" + local, type, null, start, end);
			}
			this.mv.visitInsn(opcode);
		}
		
		//region Instrumentation
		private void instrumentModifications(@NotNull Map<Type, Annotation> annotations, @NotNull Type orginalType, int index) {
			if (Arrays.stream(MODIFICATIONS).noneMatch(annotations::containsKey)) {
				return;
			}
			
			this.mv.visitVarInsn(orginalType.getOpcode(Opcodes.ILOAD), index);
			Type resultType = orginalType;
			
			if (annotations.containsKey(LOG)) {
				resultType = this.instrumentLog(annotations.get(LOG), resultType, index);
			}
			if (annotations.containsKey(EXP)) {
				resultType = this.instrumentExp(annotations.get(EXP), resultType, index);
			}
			if (annotations.containsKey(POW)) {
				resultType = this.instrumentPow(annotations.get(POW), resultType, index);
			}
			if (annotations.containsKey(TRIG)) {
				resultType = this.instrumentTrig(annotations.get(TRIG), resultType, index);
			}
			if (annotations.containsKey(ROUND)) {
				resultType = this.instrumentRound(annotations.get(ROUND), resultType, index);
			}
			if (annotations.containsKey(ABS)) {
				resultType = this.instrumentAbs(resultType, index);
			}
			if (annotations.containsKey(NEGATE)) {
				resultType = this.instrumentNegate(resultType, index);
			}
			if (annotations.containsKey(CLAMP)) {
				resultType = this.instrumentClamp(annotations.get(CLAMP), resultType, index);
			}
			if (annotations.containsKey(MIN)) {
				resultType = this.instrumentMin(annotations.get(MIN), resultType, index);
			}
			if (annotations.containsKey(MAX)) {
				resultType = this.instrumentMax(annotations.get(MAX), resultType, index);
			}
			this.mv.visitVarInsn(resultType.getOpcode(Opcodes.ISTORE), index);
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
		private @NotNull Type instrumentLog(@NotNull Annotation annotation, @NotNull Type type, int index) {
			double base = annotation.getOrDefault("value");
			boolean natural = annotation.getOrDefault("natural");
			CrashReport report = CrashReport.create(INVALID_CONFIGURATION_CATEGORY).addDetail("Method", this.method.getSignature(SignatureType.DEBUG))
				.addDetail("Annotation", annotation.getSignature(SignatureType.SOURCE)).addDetail("Value Found", base);
			
			if (base <= 1) {
				throw report.exception("Invalid @Log annotation found, expected 'base > 1'");
			}
			
			if (natural && Math.abs(base - Math.E) > 0.0001) {
				throw report.exception("Invalid @Log annotation found, expected 'natural = true' for base 'E'");
			}
			
			instrumentNumberConversion(this.mv, type, DOUBLE);
			if (Math.abs(base - Math.E) < 0.0001) {
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, MATH.getInternalName(), natural ? "log1p" : "log", "(D)D", false);
			} else if (Math.abs(base - 10) < 0.0001) {
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, MATH.getInternalName(), "log10", "(D)D", false);
			} else {
				loadNumber(this.mv, base);
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, RUNTIME_UTILS.getInternalName(), "log", "(DD)D", false);
			}
			return DOUBLE;
		}
		
		private @NotNull Type instrumentExp(@NotNull Annotation annotation, @NotNull Type type, int index) {
			ExponentialOperation operation = ExponentialOperation.valueOf(annotation.getOrDefault("value"));
			instrumentNumberConversion(this.mv, type, DOUBLE);
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, MATH.getInternalName(), operation.name().toLowerCase(), "(D)D", false);
			return DOUBLE;
		}
		
		private @NotNull Type instrumentPow(@NotNull Annotation annotation, @NotNull Type type, int index) {
			double value = annotation.getOrDefault("value");
			instrumentNumberConversion(this.mv, type, DOUBLE);
			loadNumber(this.mv, value);
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, MATH.getInternalName(), "pow", "(DD)D", false);
			return DOUBLE;
		}
		
		private @NotNull Type instrumentTrig(@NotNull Annotation annotation, @NotNull Type type, int index) {
			TrigonometricOperation operation = TrigonometricOperation.valueOf(annotation.getOrDefault("value"));
			instrumentNumberConversion(this.mv, type, DOUBLE);
			
			if (annotation.getOrDefault("degrees")) {
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, MATH.getInternalName(), "toRadians", "(D)D", false);
			}
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, MATH.getInternalName(), operation.name().toLowerCase(), "(D)D", false);
			return DOUBLE;
		}
		
		private @NotNull Type instrumentRound(@NotNull Annotation annotation, @NotNull Type type, int index) {
			RoundingMode mode = RoundingMode.valueOf(annotation.getOrDefault("mode"));
			if (mode.requiresFloatingPointInput()) {
				instrumentNumberConversion(this.mv, type, DOUBLE);
			} else {
				instrumentNumberConversion(this.mv, type, LONG);
			}
			
			long value = annotation.getOrDefault("value");
			if (mode == RoundingMode.FLOOR || mode == RoundingMode.CEIL) {
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, MATH.getInternalName(), mode.getMethodName(), "(D)D", false);
				return DOUBLE;
			} else if (mode == RoundingMode.ROUND) {
				loadNumber(this.mv, (int) value);
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, RUNTIME_UTILS.getInternalName(), mode.getMethodName(), "(DI)D", false);
				return DOUBLE;
			} else {
				loadNumber(this.mv, value);
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, MATH.getInternalName(), mode.getMethodName(), "(JJ)J", false);
				return LONG;
			}
		}
		
		private @NotNull Type instrumentAbs(@NotNull Type type, int index) {
			Type defaultType = this.getDefaultPrimitiveNumberType(type);
			instrumentNumberConversion(this.mv, type, defaultType);
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, MATH.getInternalName(), "abs", "(" + defaultType.getDescriptor() + ")" + defaultType.getDescriptor(), false);
			return defaultType;
		}
		
		private @NotNull Type instrumentNegate(@NotNull Type type, int index) {
			Type defaultType = this.getDefaultPrimitiveNumberType(type);
			instrumentNumberConversion(this.mv, type, defaultType);
			
			if (defaultType.equals(LONG)) {
				this.mv.visitInsn(Opcodes.LNEG);
			} else if (defaultType.equals(FLOAT)) {
				this.mv.visitInsn(Opcodes.FNEG);
			} else if (defaultType.equals(DOUBLE)) {
				this.mv.visitInsn(Opcodes.DNEG);
			} else {
				this.mv.visitInsn(Opcodes.INEG);
			}
			return defaultType;
		}
		
		@SuppressWarnings("FloatingPointEquality")
		private @NotNull Type instrumentClamp(@NotNull Annotation annotation, @NotNull Type type, int index) {
			double min = annotation.getOrDefault("min");
			double max = annotation.getOrDefault("max");
			
			String value = annotation.get("value");
			if (value != null) {
				CrashReport report = CrashReport.create(INVALID_CONFIGURATION_CATEGORY).setMessage("Invalid @Clamp annotation found, expected 'min:max'")
					.addDetail("Method", this.method.getSignature(SignatureType.DEBUG)).addDetail("Annotation", annotation.getSignature(SignatureType.SOURCE))
					.addDetail("Message Details", "Value must be blank if min and max are set").addDetail("Value Found", value);
				
				if (value.isBlank() && min == Double.MIN_VALUE && max == Double.MAX_VALUE) {
					throw report.addDetailBefore("Value Found", "Message Details", "Value must not be blank if min and max are not set").exception();
				}
				if (!value.contains(":")) {
					throw report.addDetailBefore("Value Found", "Message Details", "Value must contain a colon ':' to separate min and max").exception();
				}
				String[] parts = value.split(":");
				if (parts.length != 2) {
					throw report.addDetailBefore("Value Found", "Message Details", "Value must contain exactly one colon ':' to separate min and max").exception();
				}
				if (parts[0].isBlank() || parts[1].isBlank()) {
					throw report.addDetailBefore("Value Found", "Message Details", "Value must not contain blank min or max values").exception();
				}
				if ("*".equals(parts[0])) {
					min = Double.MIN_VALUE;
				} else {
					min = Double.parseDouble(parts[0]);
				}
				if ("*".equals(parts[1])) {
					max = Double.MAX_VALUE;
				} else {
					max = Double.parseDouble(parts[1]);
				}
			}
			
			Type defaultType = this.getDefaultPrimitiveNumberType(type);
			instrumentNumberConversion(this.mv, type, defaultType);
			if (defaultType.equals(LONG)) {
				loadNumber(this.mv, (long) min);
				loadNumber(this.mv, (long) max);
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, MATH.getInternalName(), "clamp", "(JJJ)J", false);
			} else if (defaultType.equals(FLOAT)) {
				loadNumber(this.mv, (float) min);
				loadNumber(this.mv, (float) max);
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, MATH.getInternalName(), "clamp", "(FFF)F", false);
			} else if (defaultType.equals(DOUBLE)) {
				loadNumber(this.mv, min);
				loadNumber(this.mv, max);
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, MATH.getInternalName(), "clamp", "(DDD)D", false);
			} else {
				this.mv.visitInsn(Opcodes.I2L);
				loadNumber(this.mv, (int) min);
				loadNumber(this.mv, (int) max);
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, MATH.getInternalName(), "clamp", "(JII)I", false);
			}
			return defaultType;
		}
		
		private @NotNull Type instrumentMin(@NotNull Annotation annotation, @NotNull Type type, int index) {
			Type defaultType = this.getDefaultPrimitiveNumberType(type);
			double min = annotation.getOrDefault("value");
			instrumentNumberConversion(this.mv, type, defaultType);
			
			if (defaultType.equals(LONG)) {
				loadNumber(this.mv, (long) min);
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, MATH.getInternalName(), "min", "(JJ)J", false);
			} else if (defaultType.equals(FLOAT)) {
				loadNumber(this.mv, (float) min);
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, MATH.getInternalName(), "min", "(FF)F", false);
			} else if (defaultType.equals(DOUBLE)) {
				loadNumber(this.mv, min);
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, MATH.getInternalName(), "min", "(DD)D", false);
			} else {
				this.mv.visitInsn(Opcodes.I2L);
				loadNumber(this.mv, (int) min);
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, MATH.getInternalName(), "min", "(II)I", false);
			}
			return defaultType;
		}
		
		private @NotNull Type instrumentMax(@NotNull Annotation annotation, @NotNull Type type, int index) {
			Type defaultType = this.getDefaultPrimitiveNumberType(type);
			double max = annotation.getOrDefault("value");
			instrumentNumberConversion(this.mv, type, defaultType);
			
			if (defaultType.equals(LONG)) {
				loadNumber(this.mv, (long) max);
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, MATH.getInternalName(), "max", "(JJ)J", false);
			} else if (defaultType.equals(FLOAT)) {
				loadNumber(this.mv, (float) max);
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, MATH.getInternalName(), "max", "(FF)F", false);
			} else if (defaultType.equals(DOUBLE)) {
				loadNumber(this.mv, max);
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, MATH.getInternalName(), "max", "(DD)D", false);
			} else {
				this.mv.visitInsn(Opcodes.I2L);
				loadNumber(this.mv, (int) max);
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, MATH.getInternalName(), "max", "(II)I", false);
			}
			return defaultType;
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
		
		private @NotNull Type getDefaultPrimitiveNumberType(@NotNull Type type) {
			Type targetPrimitiveType = convertToPrimitive(type);
			if (targetPrimitiveType.equals(BYTE) || targetPrimitiveType.equals(SHORT)) {
				return INT;
			}
			return targetPrimitiveType;
		}
		//endregion
	}
}
