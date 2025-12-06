package net.luis.agent.asm.transformer.implementation;

import net.luis.agent.Agent;
import net.luis.agent.asm.ASMTreeUtils;
import net.luis.agent.asm.Instrumentations;
import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.ContextBasedClassVisitor;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.asm.type.*;
import net.luis.agent.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.util.*;

import static net.luis.agent.asm.Types.*;

public class AssignorTransformer extends BaseClassTransformer {
	
	private final Map</*Target Class*/String, /*Interfaces*/List<InterfaceTransformer.InterfaceInfo>> lookup = InterfaceTransformer.createLookup(INJECT_INTERFACE);
	
	//region Type filtering
	@Override
	protected boolean shouldIgnoreClass(@NotNull Type type) {
		return !this.lookup.containsKey(type.getInternalName());
	}
	//endregion
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @NotNull ClassWriter writer) {
		return new AssignorVisitor(writer, type, () -> this.modified = true, this.lookup);
	}
	
	private static class AssignorVisitor extends ContextBasedClassVisitor {

		private static final String REPORT_CATEGORY = "Assignor Implementation Error";

		private final Map</*Target Class*/String, /*Interfaces*/List<InterfaceTransformer.InterfaceInfo>> lookup;
		private final List<String> unfinal = new ArrayList<>();

		private AssignorVisitor(@NotNull ClassWriter writer, @NotNull Type type, @NotNull Runnable markModified, @NotNull Map<String, List<InterfaceTransformer.InterfaceInfo>> lookup) {
			super(writer, type, markModified);
			this.lookup = lookup;
		}
		
		private static @NotNull CrashReport createReport(@NotNull String message, @NotNull Type iface, @NotNull String methodSignature) {
			return CrashReport.create(message, REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Interface Method", methodSignature);
		}
		
		@Override
		public void visit(int version, int access, @NotNull String name, @Nullable String signature, @Nullable String superClass, String @Nullable [] interfaces) {
			super.visit(version, access, name, signature, superClass, interfaces);
			if (this.lookup.containsKey(name)) {
				ClassNode targetClass = Agent.getClass(Type.getObjectType(name));
				for (InterfaceTransformer.InterfaceInfo ifaceInfo : this.lookup.get(name)) {
					ClassNode ifaceClass = ifaceInfo.classNode;
					Type iface = ifaceInfo.type;
					for (MethodNode method : ifaceClass.methods) {
						if (ASMTreeUtils.hasAnnotation(method, ASSIGNOR)) {
							this.validateMethod(method, targetClass);
						} else if (ASMTreeUtils.is(method, TypeAccess.PUBLIC)) {
							if (!ASMTreeUtils.hasAnnotation(method, ASSIGNOR) && !ASMTreeUtils.hasAnnotation(method, ACCESSOR) &&
								!ASMTreeUtils.hasAnnotation(method, INVOKER) && !ASMTreeUtils.hasAnnotation(method, INJECT) &&
								!ASMTreeUtils.hasAnnotation(method, REDIRECT) && !ASMTreeUtils.hasAnnotation(method, MODIFY) &&
								!ASMTreeUtils.hasAnnotation(method, IMPLEMENTED)) {
								throw createReport("Found method without annotation, does not know how to implement", iface, ASMTreeUtils.getDebugSignature(iface, method)).exception();
							}
						}
					}
				}
			}
		}
		
		private void validateMethod(@NotNull MethodNode ifaceMethod, @NotNull ClassNode targetClass) {
			Type ifaceType = ASMTreeUtils.getType(targetClass);
			String signature = ASMTreeUtils.getDebugSignature(ifaceType, ifaceMethod);
			Type methodType = Type.getMethodType(ifaceMethod.desc);

			if (!ASMTreeUtils.is(ifaceMethod, TypeAccess.PUBLIC)) {
				throw CrashReport.create("Method annotated with @Assignor must be public", REPORT_CATEGORY).addDetail("Interface", ifaceType).addDetail("Assignor", signature).exception();
			}
			if (ASMTreeUtils.is(ifaceMethod, TypeModifier.STATIC)) {
				throw CrashReport.create("Method annotated with @Assignor must not be static", REPORT_CATEGORY).addDetail("Interface", ifaceType).addDetail("Assignor", signature).exception();
			}
			if (!ASMTreeUtils.is(ifaceMethod, TypeModifier.ABSTRACT)) {
				throw CrashReport.create("Method annotated with @Assignor must not be default implemented", REPORT_CATEGORY).addDetail("Interface", ifaceType).addDetail("Assignor", signature).exception();
			}
			if (!ASMTreeUtils.returns(ifaceMethod, Type.VOID_TYPE)) {
				throw CrashReport.create("Method annotated with @Assignor must return void", REPORT_CATEGORY).addDetail("Interface", ifaceType).addDetail("Assignor", signature).exception();
			}
			if (ASMTreeUtils.getParameterCount(ifaceMethod) != 1) {
				throw CrashReport.create("Method annotated with @Assignor must have exactly one parameter", REPORT_CATEGORY).addDetail("Interface", ifaceType).addDetail("Assignor", signature).exception();
			}
			if (ASMTreeUtils.getExceptionCount(ifaceMethod) > 0) {
				throw CrashReport.create("Method annotated with @Assignor must not throw exceptions", REPORT_CATEGORY).addDetail("Interface", ifaceType).addDetail("Assignor", signature)
					.addDetail("Exceptions", ASMTreeUtils.getExceptions(ifaceMethod)).exception();
			}
			MethodNode existingMethod = ASMTreeUtils.getMethod(targetClass, ASMTreeUtils.getFullSignature(ifaceMethod));
			if (existingMethod != null) {
				throw CrashReport.create("Target class of assignor already has method with same signature", REPORT_CATEGORY).addDetail("Interface", ifaceType).addDetail("Assignor", signature)
					.addDetail("Existing Method", ASMTreeUtils.getDebugSignature(ASMTreeUtils.getType(targetClass), existingMethod)).exception();
			}
			String accessorTarget = this.getAssignorName(ifaceMethod);
			FieldNode targetField = ASMTreeUtils.getField(targetClass, accessorTarget);
			if (targetField == null) {
				throw CrashReport.create("Target field for assignor was not found in target class", REPORT_CATEGORY).addDetail("Interface", ifaceType).addDetail("Assignor", signature)
					.addDetail("Expected Accessor Target", accessorTarget).exception();
			}
			if (ASMTreeUtils.is(targetField, TypeAccess.PUBLIC) && !ASMTreeUtils.is(targetField, TypeModifier.FINAL)) {
				throw CrashReport.create("Target field for assignor is public and not final, no assignor required", REPORT_CATEGORY).addDetail("Interface", ifaceType).addDetail("Assignor", signature)
					.addDetail("Accessor Target", accessorTarget).exception();
			}
			Type parameterType = methodType.getArgumentTypes()[0];
			Type fieldType = Type.getType(targetField.desc);
			if (!fieldType.equals(parameterType)) {
				throw CrashReport.create("Assignor parameter type does not match target field type", REPORT_CATEGORY).addDetail("Interface", ifaceType).addDetail("Assignor", signature)
					.addDetail("Accessor Target", accessorTarget).addDetail("Expected Type", fieldType).addDetail("Actual Type", parameterType).exception();
			}
			String fieldSignature = targetField.signature;
			if (fieldSignature != null && !fieldSignature.isEmpty() && !fieldSignature.isBlank()) {
				String assignorSignature = this.getParameterTypesSignature(ifaceMethod);
				if (!Objects.equals(fieldSignature, assignorSignature)) {
					throw CrashReport.create("Assignor signature does not match target field signature", REPORT_CATEGORY).addDetail("Interface", ifaceType).addDetail("Assignor", signature)
						.addDetail("Accessor Target", accessorTarget).addDetail("Expected Signature", fieldSignature).addDetail("Actual Signature", assignorSignature).exception();
				}
			}
			this.generateAssignor(ifaceMethod, targetField, ASMTreeUtils.getType(targetClass));
		}
		
		private void generateAssignor(@NotNull MethodNode ifaceMethod, @NotNull FieldNode targetField, @NotNull Type targetType) {
			if (ASMTreeUtils.is(targetField, TypeModifier.FINAL)) {
				this.unfinal.add(targetField.name);
			}
			String genericSignature = ifaceMethod.signature != null && !ifaceMethod.signature.isEmpty() ? ifaceMethod.signature : null;
			MethodVisitor visitor = super.visitMethod(Opcodes.ACC_PUBLIC, ifaceMethod.name, ifaceMethod.desc, genericSignature, null);
			Label start = new Label();
			Label end = new Label();
			Instrumentations.instrumentMethodAnnotations(visitor, ifaceMethod);
			Instrumentations.instrumentParameterAnnotations(visitor, ifaceMethod);
			visitor.visitCode();
			visitor.visitLabel(start);
			visitor.visitVarInsn(Opcodes.ALOAD, 0);
			Type parameterType = Type.getMethodType(ifaceMethod.desc).getArgumentTypes()[0];
			visitor.visitVarInsn(parameterType.getOpcode(Opcodes.ILOAD), 1);
			visitor.visitFieldInsn(Opcodes.PUTFIELD, targetType.getInternalName(), targetField.name, targetField.desc);
			visitor.visitInsn(Opcodes.RETURN);
			visitor.visitLabel(end);
			String fieldSignature = targetField.signature != null && !targetField.signature.isEmpty() ? targetField.signature : null;
			visitor.visitLocalVariable("this", targetType.getDescriptor(), fieldSignature, start, end, 0);
			visitor.visitLocalVariable("generated$AssignorTransformer$Temp" + 1, targetField.desc, null, start, end, 1);
			visitor.visitMaxs(0, 0);
			visitor.visitEnd();
			this.markModified();
		}
		
		@Override
		public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
			if (this.unfinal.contains(name)) {
				access = access & ~Opcodes.ACC_FINAL;
				this.markModified();
			}
			return super.visitField(access, name, descriptor, signature, value);
		}
		
		//region Helper methods
		private @NotNull String getAssignorName(@NotNull MethodNode ifaceMethod) {
			AnnotationNode annotation = ASMTreeUtils.getAnnotation(ifaceMethod, ASSIGNOR);
			if (annotation != null) {
				String target = ASMTreeUtils.getAnnotationValue(annotation, "target", null);
				if (target != null) {
					return target;
				}
			}
			String methodName = ifaceMethod.name;
			if (methodName.startsWith("set")) {
				return Utils.uncapitalize(methodName.substring(3));
			} else if (methodName.startsWith("assign")) {
				return Utils.uncapitalize(methodName.substring(6));
			}
			return methodName;
		}

		private @NotNull String getParameterTypesSignature(@NotNull MethodNode method) {
			String signature = method.signature;
			if (signature == null || signature.isEmpty()) {
				return "";
			}
			int start = signature.indexOf('(');
			int end = signature.indexOf(')');
			return signature.substring(start + 1, end);
		}
		//endregion
	}
}
