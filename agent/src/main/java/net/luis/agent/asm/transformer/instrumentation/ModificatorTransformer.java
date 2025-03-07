package net.luis.agent.asm.transformer.instrumentation;

import net.luis.agent.Agent;
import net.luis.agent.asm.ASMUtils;
import net.luis.agent.asm.base.*;
import net.luis.agent.asm.data.*;
import net.luis.agent.asm.data.Class;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.asm.scanner.ClassFileScanner;
import net.luis.agent.asm.scanner.LambdaMethodScanner;
import net.luis.agent.asm.type.*;
import net.luis.agent.util.ModifyTarget;
import net.luis.agent.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.util.*;
import java.util.function.BiConsumer;

import static net.luis.agent.asm.Instrumentations.*;
import static net.luis.agent.asm.Types.*;

/**
 *
 * @author Luis-St
 *
 */

public class ModificatorTransformer extends BaseClassTransformer {
	
	private static final String REPORT_CATEGORY = "Modificator Implementation Error";
	
	private final Map</*Target Class*/String, /*Interfaces*/List<String>> lookup = InterfaceTransformer.createLookup(INJECT_INTERFACE);
	
	//region Type filtering
	@Override
	protected boolean shouldIgnoreClass(@NotNull Type type) {
		return !this.lookup.containsKey(type.getInternalName());
	}
	//endregion
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @NotNull ClassWriter writer) {
		return new ModificatorClassVisitor(writer, type, this.lookup, () -> this.modified = true);
	}
	
	private static final class ModificatorClassVisitor extends ContextBasedClassVisitor {
		
		private final Map</*Method Signature*/String, /*Lookup*/EnumMap<ModifyTarget, /*Iface Method*/List<Method>>> modifiers = new HashMap<>();
		private final Map</*Target Class*/String, /*Interfaces*/List<String>> lookup;
		
		private ModificatorClassVisitor(@NotNull ClassVisitor visitor, @NotNull Type type, @NotNull Map<String, List<String>> lookup, @NotNull Runnable markModified) {
			super(visitor, type, markModified);
			this.lookup = lookup;
		}
		
		private static @NotNull CrashReport createReport(@NotNull String message, @NotNull Type iface, @NotNull String methodSignature) {
			return CrashReport.create(message, REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Interface Method", methodSignature);
		}
		
		@Override
		public void visit(int version, int access, @NotNull String name, @Nullable String signature, @Nullable String superClass, String @Nullable [] interfaces) {
			super.visit(version, access, name, signature, superClass, interfaces);
			if (this.lookup.containsKey(name)) {
				Class targetClass = Agent.getClass(Type.getObjectType(name));
				
				for (Type iface : this.lookup.get(name).stream().map(Type::getObjectType).toList()) {
					Class ifaceClass = Agent.getClass(iface);
					
					for (Method method : ifaceClass.getMethods().values()) {
						if (method.isAnnotatedWith(MODIFICATOR)) {
							this.validateMethod(method, targetClass);
						} else if (method.is(TypeAccess.PUBLIC)) {
							if (method.getAnnotations().isEmpty()) {
								throw createReport("Found method without annotation, does not know how to implement", iface, method.getSignature(SignatureType.DEBUG)).exception();
							} else if (method.getAnnotations().values().stream().map(Annotation::getType).noneMatch(IMPLEMENTATION_ANNOTATIONS::contains)) {
								throw createReport("Found method without valid annotation, does not know how to implement", iface, method.getSignature(SignatureType.DEBUG)).exception();
							}
						}
					}
				}
			}
		}
		
		private void validateMethod(@NotNull Method ifaceMethod, @NotNull Class targetClass) {
			CrashReport report = CrashReport.create(REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Modificator", ifaceMethod.getSignature(SignatureType.DEBUG));
			
			if (!ifaceMethod.is(TypeAccess.PUBLIC)) {
				throw report.exception("Method annotated with @Modificator must be public");
			}
			if (ifaceMethod.is(TypeModifier.ABSTRACT)) {
				throw report.exception("Method annotated with @Modificator must be default implemented");
			}
			if (ifaceMethod.returns(VOID)) {
				throw report.exception("Method annotated with @Modificator must not return void");
			}
			if (ifaceMethod.getExceptionCount() > 0) {
				throw report.addDetail("Exceptions", ifaceMethod.getExceptions()).exception("Method annotated with @Modificator must not throw exceptions");
			}
			
			Method existingMethod = targetClass.getMethod(ifaceMethod.getSignature(SignatureType.FULL));
			if (existingMethod != null) {
				throw report.addDetail("Existing Method", existingMethod.getSignature(SignatureType.DEBUG)).exception("Target class of modificator already has method with same signature");
			}
			
			String modifyName = this.getModifyName(ifaceMethod);
			List<Method> possibleMethod = ASMUtils.getBySignature(modifyName, targetClass);
			if (possibleMethod.isEmpty()) {
				throw report.addDetail("Method", modifyName).addDetail("Possible Methods", targetClass.getMethods(this.getRawModifyName(modifyName)).stream().map(Method::toString).toList())
					.exception("Could not find method specified in modificator");
			}
			if (possibleMethod.size() > 1) {
				throw report.addDetail("Method", modifyName).addDetail("Possible Methods", possibleMethod.stream().map(Method::toString).toList()).exception("Found multiple possible methods for modificator");
			}
			
			Method method = possibleMethod.getFirst();
			if (!ifaceMethod.is(TypeModifier.STATIC) && method.is(TypeModifier.STATIC)) {
				throw report.addDetail("Method", method.getSignature(SignatureType.DEBUG)).exception("Method annotated with @Modificator is declared none-static, but specified a static method");
			}
			if (ifaceMethod.getParameterCount() > 0) {
				for (Parameter parameter : ifaceMethod.getParameters().values()) {
					if (!parameter.isAnnotatedWith(ORIGINAL) && !parameter.isAnnotatedWith(THIS) && !parameter.isAnnotatedWith(LOCAL)) {
						throw report.addParameterDetails(parameter).exception("Parameter of modificator must be annotated with @Original, @This or @Local");
					}
					if (method.is(TypeModifier.STATIC) && parameter.isAnnotatedWith(THIS)) {
						throw report.addParameterDetails(parameter).addDetail("Method", method.getSignature(SignatureType.DEBUG)).exception("Parameter of modificator cannot be annotated with @This, because the specified method is static");
					}
				}
			}
			
			Annotation annotation = ifaceMethod.getAnnotation(MODIFICATOR);
			ModifyTarget target = ModifyTarget.valueOf(annotation.get("target"));
			if (target == ModifyTarget.PARAMETER) {
				if (method.getParameterCount() == 0) {
					throw report.exception("Method specified in modificator must have parameters because target is set to PARAMETER");
				}
			} else if (target == ModifyTarget.RETURN) {
				if (method.returns(VOID)) {
					throw report.exception("Method specified in modificator must return a value because target is set to RETURN");
				}
			}
			
			LambdaMethodScanner scanner = new LambdaMethodScanner(method);
			ClassFileScanner.scanClass(targetClass.getType(), scanner);
			
			this.modifiers.computeIfAbsent(method.getSignature(SignatureType.FULL), key -> new EnumMap<>(ModifyTarget.class)).computeIfAbsent(target, key -> new ArrayList<>()).add(ifaceMethod);
			for (Method lambdaMethod : scanner.getLambdaMethods()) {
				this.modifiers.computeIfAbsent(lambdaMethod.getSignature(SignatureType.FULL), key -> new EnumMap<>(ModifyTarget.class)).computeIfAbsent(target, key -> new ArrayList<>()).add(ifaceMethod);
			}
		}
		
		@Override
		public @NotNull MethodVisitor visitMethod(int access, @NotNull String name, @NotNull String descriptor, @Nullable String signature, String @Nullable [] exceptions) {
			MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
			String fullSignature = name + descriptor;
			Method method = Agent.getClass(this.type).getMethod(fullSignature);
			
			if (this.modifiers.containsKey(fullSignature) && method != null) {
				this.markModified();
				return new ModificatorMethodVisitor(new LocalVariablesSorter(access, descriptor, visitor), method, this.modifiers.get(fullSignature), (target, ifaceMethod) -> {
					for (String fullMethodSignature : this.modifiers.keySet()) {
						this.modifiers.getOrDefault(fullMethodSignature, new EnumMap<>(ModifyTarget.class)).getOrDefault(target, Collections.emptyList()).remove(ifaceMethod);
					}
				});
			}
			return visitor;
		}
		
		//region Helper methods
		private @NotNull String getModifyName(@NotNull Method ifaceMethod) {
			Annotation annotation = ifaceMethod.getAnnotation(MODIFICATOR);
			String target = annotation.get("method");
			if (target != null) {
				return target;
			}
			String methodName = ifaceMethod.getName();
			if (methodName.startsWith("modify")) {
				return Utils.uncapitalize(methodName.substring(6));
			} else if (methodName.startsWith("modificator")) {
				return Utils.uncapitalize(methodName.substring(11));
			}
			return methodName;
		}
		
		private @NotNull String getRawModifyName(@NotNull String target) {
			if (target.contains("(")) {
				return target.substring(0, target.indexOf('('));
			}
			return target;
		}
		//endregion
	}
	
	private static final class ModificatorMethodVisitor extends LabelTrackingMethodVisitor {
		
		private static final String MISSING_INFORMATION = "Missing Debug Information";
		private static final String TYPE_MISMATCH = "Type Mismatch";
		private static final String NOT_FOUND = "Not Found";
		
		private final Method method;
		private final BiConsumer<ModifyTarget, /*Iface Method*/ Method> deleter;
		private final EnumMap<ModifyTarget, Map</*Unique Target Identifier*/ String, Integer>> ordinals = new EnumMap<>(ModifyTarget.class);
		private final Map</*Field*/String, List</*Iface Method*/Method>> fieldTargets = new HashMap<>();
		private final Map</*Parameter Index*/Integer, List</*Iface Method*/Method>> parameterTargets = new HashMap<>();
		private final Map</*Constant*/String, List</*Iface Method*/Method>> constantTargets = new HashMap<>();
		private final List</*Iface Method*/Method> returnTargets;
		
		private ModificatorMethodVisitor(
			@NotNull LocalVariablesSorter visitor, @NotNull Method method, @NotNull EnumMap<ModifyTarget, List<Method>> modifiers, @NotNull BiConsumer<ModifyTarget, /*Iface Method*/ Method> deleter
		) {
			super(visitor);
			this.method = method;
			this.deleter = deleter;
			for (Method ifaceMethod : modifiers.getOrDefault(ModifyTarget.FIELD, Collections.emptyList())) {
				this.fieldTargets.computeIfAbsent(ifaceMethod.getAnnotation(MODIFICATOR).getOrDefault("value"), key -> new ArrayList<>()).add(ifaceMethod);
			}
			this.fieldTargets.forEach((key, value) -> value.sort(Comparator.comparingInt(ModificatorMethodVisitor::getPriority)));
			
			for (Method ifaceMethod : modifiers.getOrDefault(ModifyTarget.PARAMETER, Collections.emptyList())) {
				this.parameterTargets.computeIfAbsent(getParameterIndex(this.method, ifaceMethod.getAnnotation(MODIFICATOR)), key -> new ArrayList<>()).add(ifaceMethod);
			}
			this.parameterTargets.forEach((key, value) -> value.sort(Comparator.comparingInt(ModificatorMethodVisitor::getPriority)));
			
			for (Method ifaceMethod : modifiers.getOrDefault(ModifyTarget.CONSTANT, Collections.emptyList())) {
				this.constantTargets.computeIfAbsent(ifaceMethod.getAnnotation(MODIFICATOR).getOrDefault("value"), key -> new ArrayList<>()).add(ifaceMethod);
			}
			this.constantTargets.forEach((key, value) -> value.sort(Comparator.comparingInt(ModificatorMethodVisitor::getPriority)));
			
			this.returnTargets = new ArrayList<>(modifiers.getOrDefault(ModifyTarget.RETURN, Collections.emptyList()));
			this.returnTargets.sort(Comparator.comparingInt(ModificatorMethodVisitor::getPriority));
		}
		
		//region Static helper methods
		private static int getParameterIndex(@NotNull Method method, @NotNull Annotation annotation) {
			int index = -1;
			String value = annotation.getOrDefault("value");
			if (value.chars().allMatch(Character::isDigit)) {
				index = Integer.parseInt(value);
			} else {
				for (Parameter parameter : method.getParameters().values()) {
					if (!parameter.isNamed()) {
						throw CrashReport.create("Unable to find parameter by name, because the parameter names were not included into the class file during compilation", MISSING_INFORMATION)
							.addDetail("Method", method.getSignature(SignatureType.DEBUG)).addDetail("Target Parameter Name", value).addDetail("Parameter Index", parameter.getIndex()).addDetail("Parameter Type", parameter.getType())
							.addDetail("Parameter Name (Generated)", parameter.getName()).exception();
					}
					if (parameter.getName().equals(value)) {
						index = parameter.getIndex();
						break;
					}
				}
			}
			if (index == -1 || index >= method.getParameterCount()) {
				throw CrashReport.create("Parameter not found", NOT_FOUND).addDetail("Method", method.getSignature(SignatureType.DEBUG)).addDetail("Target Value", value).addDetail("Parameter Index", index)
					.addDetail("Parameter Indexes", method.getParameters().values().stream().map(Parameter::getIndex).toList()).exception();
			}
			return index;
		}
		
		private static int getPriority(@NotNull Method method) {
			return method.getAnnotation(MODIFICATOR).getOrDefault("priority");
		}
		
		private static int getOrdinal(@NotNull Method method) {
			return method.getAnnotation(MODIFICATOR).getOrDefault("ordinal");
		}
		//endregion
		
		@Override
		public void visitCode() {
			super.visitCode();
			if (!this.parameterTargets.isEmpty()) {
				for (Parameter parameter : this.method.getParameters().values()) {
					for (Method ifaceMethod : this.parameterTargets.getOrDefault(parameter.getIndex(), Collections.emptyList())) {
						//region Validation
						if (!ifaceMethod.returns(parameter.getType())) {
							throw CrashReport.create("Method annotated with @Modificator with target type parameter must return the same type as the parameter", TYPE_MISMATCH).addDetail("Interface", ifaceMethod.getOwner())
								.addDetail("Modificator", ifaceMethod.getSignature(SignatureType.DEBUG)).addParameterDetails(parameter).exception();
						}
						if (getOrdinal(ifaceMethod) != 0) {
							throw CrashReport.create("Method annotated with @Modificator with target type parameter must have ordinal set to 0", TYPE_MISMATCH).addDetail("Interface", ifaceMethod.getOwner())
								.addDetail("Modificator", ifaceMethod.getSignature(SignatureType.DEBUG)).addParameterDetails(parameter).exception();
						}
						//endregion
						
						this.instrumentModify(ifaceMethod, parameter.getType(), parameter.getLoadIndex());
						this.deleter.accept(ModifyTarget.PARAMETER, ifaceMethod);
					}
				}
			}
		}
		
		@Override
		public void visitFieldInsn(int opcode, @NotNull String owner, @NotNull String name, @NotNull String descriptor) {
			if (opcode == Opcodes.PUTFIELD || opcode == Opcodes.PUTSTATIC) {
				if (!this.fieldTargets.isEmpty()) {
					List<Method> ifaceMethods = this.fieldTargets.entrySet().stream().filter(entry -> {
						return ASMUtils.matchesTarget(entry.getKey(), Type.getObjectType(owner), name, Type.getType(descriptor));
					}).map(Map.Entry::getValue).flatMap(Collection::stream).toList();
					
					if (!ifaceMethods.isEmpty()) {
						Type type = Type.getType(descriptor);
						String message = "Method annotated with @Modificator with target type field must return the same type as the field";
						this.instrumentModify(type, ifaceMethods, ModifyTarget.FIELD, owner + '#' + name, message, Map.of("Field Owner", Type.getObjectType(owner), "Field", name, "Field Type", type));
					}
				}
			}
			super.visitFieldInsn(opcode, owner, name, descriptor);
		}
		
		@Override
		public void visitInsn(int opcode) {
			if (isReturn(opcode) && !this.returnTargets.isEmpty()) {
				Type type = this.method.getReturnType();
				Label start = new Label();
				Label end = new Label();
				int index = newLocal(this.mv, type);
				this.insertLabel(start);
				this.mv.visitVarInsn(type.getOpcode(Opcodes.ISTORE), index);
				
				for (Method ifaceMethod : this.returnTargets) {
					//region Validation
					if (!ifaceMethod.returns(type)) {
						throw CrashReport.create("Method annotated with @Modificator with target type return must return the same type as the method", TYPE_MISMATCH).addDetail("Interface", ifaceMethod.getOwner())
							.addDetail("Modificator", ifaceMethod.getSignature(SignatureType.DEBUG)).addDetail("Return Type", type).exception();
					}
					if (getOrdinal(ifaceMethod) != 0) {
						throw CrashReport.create("Method annotated with @Modificator with target type return must have ordinal set to 0", TYPE_MISMATCH).addDetail("Interface", ifaceMethod.getOwner())
							.addDetail("Modificator", ifaceMethod.getSignature(SignatureType.DEBUG)).addDetail("Return Type", type).exception();
					}
					//endregion
					
					this.instrumentModify(ifaceMethod, type, index);
					this.deleter.accept(ModifyTarget.RETURN, ifaceMethod);
				}
				this.mv.visitVarInsn(type.getOpcode(Opcodes.ILOAD), index);
				this.insertLabel(end);
				this.visitLocalVariable(index, "generated$ModificatorTransformer$Temp" + index, type, null, start, end);
			}
			super.visitInsn(opcode);
			if (isConstant(opcode) && !this.constantTargets.isEmpty()) {
				List<Method> ifaceMethods = this.constantTargets.getOrDefault(this.getConstant(opcode), Collections.emptyList());
				if (ifaceMethods.isEmpty()) {
					return;
				}
				
				Type type = this.getTypeOfConstant(opcode);
				String identifier = this.getConstant(opcode);
				String message = "Method annotated with @Modificator with target type constant must return the same type as the constant";
				this.instrumentModify(type, ifaceMethods, ModifyTarget.CONSTANT, identifier, message, Map.of("Constant", this.getConstant(opcode), "Constant Type", type));
			}
		}
		
		@Override
		public void visitIntInsn(int opcode, int operand) {
			super.visitIntInsn(opcode, operand);
			if (!this.constantTargets.isEmpty() && (opcode == Opcodes.BIPUSH || opcode == Opcodes.SIPUSH)) {
				List<Method> ifaceMethods = this.constantTargets.getOrDefault(String.valueOf(operand), Collections.emptyList());
				if (ifaceMethods.isEmpty()) {
					return;
				}
				
				Type type = opcode == Opcodes.BIPUSH ? BYTE : SHORT;
				String identifier = String.valueOf(operand);
				String message = "Method annotated with @Modificator with target type constant must return the same type as the constant";
				this.instrumentModify(type, ifaceMethods, ModifyTarget.CONSTANT, identifier, message, Map.of("Constant", operand, "Constant Type", type));
			}
		}
		
		@Override
		public void visitLdcInsn(@NotNull Object value) {
			super.visitLdcInsn(value);
			if (!this.constantTargets.isEmpty()) {
				Type type = this.getTypeOfConstant(value);
				String str = type.equals(CLASS) ? getSimpleName((Type) value) + ".class" : value.toString();
				List<Method> ifaceMethods = this.constantTargets.getOrDefault(str, Collections.emptyList());
				if (ifaceMethods.isEmpty()) {
					return;
				}
				
				String message = "Method annotated with @Modificator with target type constant must return the same type as the constant";
				this.instrumentModify(type, ifaceMethods, ModifyTarget.CONSTANT, str, message, Map.of("Constant", value, "Constant Type", type));
			}
		}
		
		@Override
		public void visitInvokeDynamicInsn(@NotNull String name, @NotNull String descriptor, @NotNull Handle handle, Object @NotNull ... arguments) {
			super.visitInvokeDynamicInsn(name, descriptor, handle, arguments);
			if (STRING_CONCAT_HANDLE.equals(handle) && !this.constantTargets.isEmpty()) {
				if (arguments.length != 1 || !(arguments[0] instanceof String argument)) {
					return;
				}
				String str = argument.replace("\u0001", "${\0}");
				List<Method> ifaceMethods = this.constantTargets.getOrDefault(str, Collections.emptyList());
				if (ifaceMethods.isEmpty()) {
					return;
				}
				
				String message = "Method annotated with @Modificator with target type constant must return the same type as the constant";
				this.instrumentModify(STRING, ifaceMethods, ModifyTarget.CONSTANT, str, message, Map.of("Constant", str, "Constant Type", STRING));
			}
		}
		
		//region Instrumentation
		private void instrumentModify(@NotNull Type type, @NotNull List<Method> ifaceMethods, @NotNull ModifyTarget target, @NotNull String identifier, @NotNull String message, @NotNull Map<String, Object> details) {
			int ordinal = this.getOrdinal(target, identifier);
			
			Label start = new Label();
			Label end = new Label();
			int index = newLocal(this.mv, type);
			this.insertLabel(start);
			this.mv.visitVarInsn(type.getOpcode(Opcodes.ISTORE), index);
			
			for (Method ifaceMethod : ifaceMethods) {
				//region Validation
				if (!ifaceMethod.returns(type)) {
					CrashReport report = CrashReport.create(message, TYPE_MISMATCH).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Modificator", ifaceMethod.getSignature(SignatureType.DEBUG));
					details.forEach(report::addDetail);
					throw report.exception();
				}
				//endregion
				
				if (getOrdinal(ifaceMethod) == ordinal) {
					this.instrumentModify(ifaceMethod, type, index);
					this.deleter.accept(target, ifaceMethod);
				}
			}
			this.mv.visitVarInsn(type.getOpcode(Opcodes.ILOAD), index);
			this.insertLabel(end);
			this.visitLocalVariable(index, "generated$ModificatorTransformer$Temp" + index, type, null, start, end);
			this.incrementOrdinal(target, identifier);
		}
		
		private void instrumentModify(@NotNull Method ifaceMethod, @NotNull Type type, int index) {
			if (this.method.is(TypeModifier.STATIC) && !ifaceMethod.is(TypeModifier.STATIC)) {
				throw CrashReport.create("Method annotated with @Modificator is declared static, but specified a non-static method", REPORT_CATEGORY)
					.addDetail("Interface", ifaceMethod.getOwner()).addDetail("Modificator", ifaceMethod.getSignature(SignatureType.DEBUG))
					.addDetail("Method", this.method.getSignature(SignatureType.DEBUG)).addDetail("Lambda Method", this.method.is(TypeModifier.SYNTHETIC)).exception();
			}
			if (!ifaceMethod.is(TypeModifier.STATIC)) {
				this.mv.visitVarInsn(Opcodes.ALOAD, 0);
			}
			for (Parameter parameter : ifaceMethod.getParameters().values()) {
				if (parameter.isAnnotatedWith(ORIGINAL)) {
					this.mv.visitVarInsn(type.getOpcode(Opcodes.ILOAD), index);
				} else {
					this.mv.visitVarInsn(parameter.getType().getOpcode(Opcodes.ILOAD), getLoadIndex("modificator", parameter, ifaceMethod, this.method, this.getScopeIndex()));
				}
			}
			this.mv.visitMethodInsn(ifaceMethod.is(TypeModifier.STATIC) ? Opcodes.INVOKESTATIC : Opcodes.INVOKEINTERFACE, ifaceMethod.getOwner().getInternalName(), ifaceMethod.getName(), ifaceMethod.getType().getDescriptor(), true);
			this.mv.visitVarInsn(type.getOpcode(Opcodes.ISTORE), index);
			if (this.method.is(TypeModifier.SYNTHETIC)) {
				ifaceMethod.getAnnotation(MODIFICATOR).getValues().put("lambda", true);
			}
		}
		//endregion
		
		//region Helper methods
		private int getOrdinal(@NotNull ModifyTarget target, @NotNull String identifier) {
			if (!this.ordinals.containsKey(target)) {
				return 0;
			}
			return this.ordinals.get(target).getOrDefault(identifier, 0);
		}
		
		private void incrementOrdinal(@NotNull ModifyTarget target, @NotNull String identifier) {
			this.ordinals.computeIfAbsent(target, key -> new HashMap<>()).merge(identifier, 1, Integer::sum);
		}
		
		private @NotNull String getConstant(int opcode) {
			return switch (opcode) {
				case Opcodes.ACONST_NULL -> "null";
				case Opcodes.ICONST_M1 -> "-1";
				case Opcodes.ICONST_0, Opcodes.LCONST_0 -> "0";
				case Opcodes.ICONST_1, Opcodes.LCONST_1 -> "1";
				case Opcodes.ICONST_2 -> "2";
				case Opcodes.ICONST_3 -> "3";
				case Opcodes.ICONST_4 -> "4";
				case Opcodes.ICONST_5 -> "5";
				case Opcodes.FCONST_0, Opcodes.DCONST_0 -> "0.0";
				case Opcodes.FCONST_1, Opcodes.DCONST_1 -> "1.0";
				case Opcodes.FCONST_2 -> "2.0";
				default -> throw new IllegalArgumentException("Unknown constant opcode: " + opcode);
			};
		}
		
		private @NotNull Type getTypeOfConstant(int opcode) {
			return switch (opcode) {
				case Opcodes.ACONST_NULL -> OBJECT;
				case Opcodes.ICONST_M1, Opcodes.ICONST_0, Opcodes.ICONST_1, Opcodes.ICONST_2, Opcodes.ICONST_3, Opcodes.ICONST_4, Opcodes.ICONST_5 -> INT;
				case Opcodes.LCONST_0, Opcodes.LCONST_1 -> LONG;
				case Opcodes.FCONST_0, Opcodes.FCONST_1, Opcodes.FCONST_2 -> FLOAT;
				case Opcodes.DCONST_0, Opcodes.DCONST_1 -> DOUBLE;
				default -> throw new IllegalArgumentException("Unknown constant opcode: " + opcode);
			};
		}
		
		private @NotNull Type getTypeOfConstant(@NotNull Object value) {
			return switch (value) {
				case Integer ignored -> INT;
				case Long ignored -> LONG;
				case Float ignored -> FLOAT;
				case Double ignored -> DOUBLE;
				case String ignored -> STRING;
				case Type ignored -> CLASS;
				default -> throw new IllegalStateException("Unexpected value: " + value);
			};
		}
		//endregion
	}
}
