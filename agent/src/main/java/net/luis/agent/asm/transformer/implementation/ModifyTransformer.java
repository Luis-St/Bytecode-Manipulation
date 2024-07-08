package net.luis.agent.asm.transformer.implementation;

import net.luis.agent.Agent;
import net.luis.agent.asm.ASMUtils;
import net.luis.agent.asm.base.*;
import net.luis.agent.asm.data.*;
import net.luis.agent.asm.data.Class;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.asm.type.*;
import net.luis.agent.util.ModifyTarget;
import net.luis.agent.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.util.*;
import java.util.stream.Collectors;

import static net.luis.agent.asm.Instrumentations.*;
import static net.luis.agent.asm.Types.*;

/**
 *
 * @author Luis-St
 *
 */

public class ModifyTransformer extends BaseClassTransformer {
	
	// ToDo:
	//  - Add recursive support for lambda expressions
	//  - Add priority for modify methods
	
	private static final String REPORT_CATEGORY = "Modify Implementation Error";
	
	private final Map</*Target Class*/String, /*Interfaces*/List<String>> lookup = InterfaceTransformer.createLookup(INJECT_INTERFACE);
	
	//region Type filtering
	@Override
	protected boolean shouldIgnoreClass(@NotNull Type type) {
		return !this.lookup.containsKey(type.getInternalName());
	}
	//endregion
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @NotNull ClassWriter writer) {
		return new ModifyClassVisitor(writer, type, this.lookup, () -> this.modified = true);
	}
	
	private static class ModifyClassVisitor extends ContextBasedClassVisitor {
		
		private final Map</*Method Signature*/String, /*Lookup*/EnumMap<ModifyTarget, /*Iface Method*/List<Method>>> modifiers = new HashMap<>();
		private final Map</*Target Class*/String, /*Interfaces*/List<String>> lookup;
		
		private ModifyClassVisitor(@NotNull ClassVisitor visitor, @NotNull Type type, @NotNull Map<String, List<String>> lookup, @NotNull Runnable markModified) {
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
						if (method.isAnnotatedWith(MODIFY)) {
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
			String signature = ifaceMethod.getSignature(SignatureType.DEBUG);
			if (!ifaceMethod.is(TypeAccess.PUBLIC)) {
				throw CrashReport.create("Method annotated with @Modify must be public", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Modify", signature).exception();
			}
			if (ifaceMethod.is(TypeModifier.ABSTRACT)) {
				throw CrashReport.create("Method annotated with @Modify must be default implemented", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Modify", signature).exception();
			}
			if (ifaceMethod.returns(VOID)) {
				throw CrashReport.create("Method annotated with @Modify must not return void", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Modify", signature).exception();
			}
			if (ifaceMethod.getExceptionCount() > 0) {
				throw CrashReport.create("Method annotated with @Modify must not throw exceptions", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Modify", signature)
					.addDetail("Exceptions", ifaceMethod.getExceptions()).exception();
			}
			Method existingMethod = targetClass.getMethod(ifaceMethod.getSignature(SignatureType.FULL));
			if (existingMethod != null) {
				throw CrashReport.create("Target class of modify already has method with same signature", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Modify", signature)
					.addDetail("Existing Method", existingMethod.getSignature(SignatureType.DEBUG)).exception();
			}
			String modifyName = this.getModifyName(ifaceMethod);
			List<Method> possibleMethod = ASMUtils.getBySignature(modifyName, targetClass);
			if (possibleMethod.isEmpty()) {
				throw CrashReport.create("Could not find method specified in modify", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Modify", signature).addDetail("Method", modifyName)
					.addDetail("Possible Methods", targetClass.getMethods(this.getRawModifyName(modifyName)).stream().map(Method::toString).toList()).exception();
			}
			if (possibleMethod.size() > 1) {
				throw CrashReport.create("Found multiple possible methods for modify", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Modify", signature).addDetail("Method", modifyName)
					.addDetail("Possible Methods", possibleMethod.stream().map(Method::toString).toList()).exception();
			}
			Method method = possibleMethod.getFirst();
			if (!ifaceMethod.is(TypeModifier.STATIC) && method.is(TypeModifier.STATIC)) {
				throw CrashReport.create("Method annotated with @Modify is declared none-static, but specified a static method", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Modify", signature)
					.addDetail("Method", method.getSignature(SignatureType.DEBUG)).exception();
			}
			if (ifaceMethod.getParameterCount() > 0) {
				for (Parameter parameter : ifaceMethod.getParameters().values()) {
					if (!parameter.isAnnotatedWith(ORIGINAL) && !parameter.isAnnotatedWith(THIS) && !parameter.isAnnotatedWith(LOCAL)) {
						throw CrashReport.create("Parameter of modify must be annotated with @Original, @This or @Local", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Modify", signature)
							.addDetail("Parameter Index", parameter.getIndex()).addDetail("Parameter Type", parameter.getType()).addDetail("Parameter Name", parameter.getName()).exception();
					}
					if (method.is(TypeModifier.STATIC) && parameter.isAnnotatedWith(THIS)) {
						throw CrashReport.create("Parameter of modify cannot be annotated with @This, because the specified method is static", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner())
							.addDetail("Modify", signature).addDetail("Parameter Index", parameter.getIndex()).addDetail("Parameter Type", parameter.getType()).addDetail("Parameter Name", parameter.getName())
							.addDetail("Method", method.getSignature(SignatureType.DEBUG)).exception();
					}
				}
			}
			Annotation annotation = ifaceMethod.getAnnotation(MODIFY);
			String value = annotation.getOrDefault("value");
			ModifyTarget target = ModifyTarget.valueOf(annotation.get("target"));
			if (target == ModifyTarget.PARAMETER) {
				if (method.getParameterCount() == 0) {
					throw CrashReport.create("Method specified in modify must have parameters because target is set to PARAMETER", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Modify", signature).exception();
				}
			} else if (target == ModifyTarget.RETURN) {
				if (method.returns(VOID)) {
					throw CrashReport.create("Method specified in modify must return a value because target is set to RETURN", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Modify", signature).exception();
				}
			}
			this.modifiers.computeIfAbsent(ifaceMethod.getSignature(SignatureType.FULL), key -> new EnumMap<>(ModifyTarget.class)).computeIfAbsent(target, key -> new ArrayList<>()).add(ifaceMethod);
		}
		
		@Override
		public @NotNull MethodVisitor visitMethod(int access, @NotNull String name, @NotNull String descriptor, @Nullable String signature, String @Nullable [] exceptions) {
			MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
			String fullSignature = name + descriptor;
			Method method = Agent.getClass(this.type).getMethod(fullSignature);
			if (this.modifiers.containsKey(fullSignature) && method != null) {
				this.markModified();
				return new ModifyMethodVisitor(visitor, method, this.modifiers.get(fullSignature));
			}
			return visitor;
		}
		
		//region Helper methods
		private @NotNull String getModifyName(@NotNull Method ifaceMethod) {
			Annotation annotation = ifaceMethod.getAnnotation(MODIFY);
			String target = annotation.get("method");
			if (target != null) {
				return target;
			}
			String methodName = ifaceMethod.getName();
			if (methodName.startsWith("modify")) {
				return Utils.uncapitalize(methodName.substring(6));
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
	
	private static class ModifyMethodVisitor extends LabelTrackingMethodVisitor {
		
		private static final String MISSING_INFORMATION = "Missing Debug Information";
		private static final String NOT_FOUND = "Not Found";
		
		private final Method method;
		private final Map</*Field*/String,List</*Iface Method*/Method>> fieldTargets = new HashMap<>();
		private final Map</*Parameter Index*/Integer, List</*Iface Method*/Method>> parameterTargets = new HashMap<>();
		private final Map</*Constant*/String, List</*Iface Method*/Method>> constantTargets = new HashMap<>();
		private final List</*Iface Method*/Method> returnTargets;
		
		private ModifyMethodVisitor(@NotNull MethodVisitor visitor, @NotNull Method method, @NotNull EnumMap<ModifyTarget, List<Method>> modifiers) {
			super(visitor);
			this.method = method;
			for (Method ifaceMethod : modifiers.getOrDefault(ModifyTarget.FIELD, Collections.emptyList())) {
				this.fieldTargets.computeIfAbsent(ifaceMethod.getAnnotation(MODIFY).getOrDefault("value"), key -> new ArrayList<>()).add(ifaceMethod);
			}
			for (Method ifaceMethod : modifiers.getOrDefault(ModifyTarget.PARAMETER, Collections.emptyList())) {
				this.parameterTargets.computeIfAbsent(getParameterIndex(this.method, ifaceMethod.getAnnotation(MODIFY)), key -> new ArrayList<>()).add(ifaceMethod);
			}
			for (Method ifaceMethod : modifiers.getOrDefault(ModifyTarget.CONSTANT, Collections.emptyList())) {
				this.constantTargets.computeIfAbsent(ifaceMethod.getAnnotation(MODIFY).getOrDefault("value"), key -> new ArrayList<>()).add(ifaceMethod);
			}
			this.returnTargets = modifiers.getOrDefault(ModifyTarget.RETURN, Collections.emptyList());
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
		//endregion
		
		@Override
		public void visitCode() {
			super.visitCode();
			if (!this.parameterTargets.isEmpty()) {
				for (Parameter parameter : this.method.getParameters().values()) {
					for (Method target : this.parameterTargets.getOrDefault(parameter.getIndex(), Collections.emptyList())) {
						this.instrumentModify(target, () -> {
							this.mv.visitVarInsn(parameter.getType().getOpcode(Opcodes.ILOAD), parameter.getIndex());
						});
						this.mv.visitVarInsn(parameter.getType().getOpcode(Opcodes.ISTORE), parameter.getIndex());
					}
				}
			}
		}
		
		@Override
		public void visitFieldInsn(int opcode, @NotNull String owner, @NotNull String name, @NotNull String descriptor) {
			super.visitFieldInsn(opcode, owner, name, descriptor);
		}
		
		@Override
		public void visitInsn(int opcode) {
			super.visitInsn(opcode);
		}
		
		@Override
		public void visitLdcInsn(@NotNull Object value) {
			super.visitLdcInsn(value);
		}
		
		@Override
		public void visitInvokeDynamicInsn(@NotNull String name, @NotNull String descriptor, @NotNull Handle handle, Object @NotNull ... arguments) {
			super.visitInvokeDynamicInsn(name, descriptor, handle, arguments);
		}
		
		//region Instrumentation
		private void instrumentModify(@NotNull Method ifaceMethod, @NotNull Runnable original) {
			if (!ifaceMethod.is(TypeModifier.STATIC)) {
				this.mv.visitVarInsn(Opcodes.ALOAD, 0);
			}
			for (Parameter parameter : ifaceMethod.getParameters().values()) {
				if (parameter.isAnnotatedWith(ORIGINAL)) {
					original.run();
				} else {
					this.mv.visitVarInsn(parameter.getType().getOpcode(Opcodes.ILOAD), getLoadIndex("modify", parameter, ifaceMethod, this.method, this.getScopeIndex()));
				}
			}
			this.mv.visitMethodInsn(ifaceMethod.is(TypeModifier.STATIC) ? Opcodes.INVOKESTATIC : Opcodes.INVOKEINTERFACE, ifaceMethod.getOwner().getInternalName(), ifaceMethod.getName(), ifaceMethod.getType().getDescriptor(), true);
		}
		//endregion
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
