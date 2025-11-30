package net.luis.agent.asm.transformer.implementation;

import net.luis.agent.Agent;
import net.luis.agent.asm.ASMTreeUtils;
import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.ContextBasedClassVisitor;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.asm.type.TypeAccess;
import net.luis.agent.asm.type.TypeModifier;
import net.luis.agent.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.util.*;

import static net.luis.agent.asm.Types.*;

public class AccessorTransformer extends BaseClassTransformer {

	private final Map</*Target Class*/String, /*Interfaces*/List<String>> lookup = InterfaceTransformer.createLookup(INJECT_INTERFACE);

	//region Type filtering
	@Override
	protected boolean shouldIgnoreClass(@NotNull Type type) {
		return !this.lookup.containsKey(type.getInternalName());
	}
	//endregion

	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @NotNull ClassWriter writer) {
		return new AccessorVisitor(writer, type, () -> this.modified = true, this.lookup);
	}

	private static class AccessorVisitor extends ContextBasedClassVisitor {

		private static final String REPORT_CATEGORY = "Accessor Implementation Error";

		private final Map</*Target Class*/String, /*Interfaces*/List<String>> lookup;

		private AccessorVisitor(@NotNull ClassWriter writer, @NotNull Type type, @NotNull Runnable markModified, @NotNull Map<String, List<String>> lookup) {
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
				if (targetClass == null) {
					return;
				}
				for (Type iface : this.lookup.get(name).stream().map(Type::getObjectType).toList()) {
					ClassNode ifaceClass = Agent.getClass(iface);
					if (ifaceClass == null) {
						continue;
					}
					for (MethodNode method : ifaceClass.methods) {
						if (ASMTreeUtils.hasAnnotation(method, ACCESSOR)) {
							this.validateMethod(method, targetClass, iface);
						} else if (ASMTreeUtils.is(method, TypeAccess.PUBLIC)) {
							List<AnnotationNode> annotations = ASMTreeUtils.getAllAnnotations(method);
							if (annotations.isEmpty()) {
								throw createReport("Found method without annotation, does not know how to implement", iface, ASMTreeUtils.getDebugSignature(iface, method)).exception();
							} else if (annotations.stream().map(ann -> Type.getType(ann.desc)).noneMatch(IMPLEMENTATION_ANNOTATIONS::contains)) {
								throw createReport("Found method without valid annotation, does not know how to implement", iface, ASMTreeUtils.getDebugSignature(iface, method)).exception();
							}
						}
					}
				}
			}
		}

		private void validateMethod(@NotNull MethodNode ifaceMethod, @NotNull ClassNode targetClass, @NotNull Type ifaceType) {
			String signature = ASMTreeUtils.getDebugSignature(ifaceType, ifaceMethod);
			if (!ASMTreeUtils.is(ifaceMethod, TypeAccess.PUBLIC)) {
				throw CrashReport.create("Method annotated with @Accessor must be public", REPORT_CATEGORY).addDetail("Interface", ifaceType).addDetail("Accessor", signature).exception();
			}
			if (ASMTreeUtils.is(ifaceMethod, TypeModifier.STATIC)) {
				throw CrashReport.create("Method annotated with @Accessor must not be static", REPORT_CATEGORY).addDetail("Interface", ifaceType).addDetail("Accessor", signature).exception();
			}
			if (!ASMTreeUtils.is(ifaceMethod, TypeModifier.ABSTRACT)) {
				throw CrashReport.create("Method annotated with @Accessor must not be default implemented", REPORT_CATEGORY).addDetail("Interface", ifaceType).addDetail("Accessor", signature).exception();
			}
			Type returnType = ASMTreeUtils.getReturnType(ifaceMethod);
			if (returnType.equals(Type.VOID_TYPE)) {
				throw CrashReport.create("Method annotated with @Accessor has void return type", REPORT_CATEGORY).addDetail("Interface", ifaceType).addDetail("Accessor", signature).exception();
			}
			if (ASMTreeUtils.getParameterCount(ifaceMethod) > 0) {
				throw CrashReport.create("Method annotated with @Accessor must not have parameters", REPORT_CATEGORY).addDetail("Interface", ifaceType).addDetail("Accessor", signature).exception();
			}
			if (ASMTreeUtils.getExceptionCount(ifaceMethod) > 0) {
				throw CrashReport.create("Method annotated with @Accessor must not throw exceptions", REPORT_CATEGORY).addDetail("Interface", ifaceType).addDetail("Accessor", signature)
					.addDetail("Exceptions", ASMTreeUtils.getExceptions(ifaceMethod)).exception();
			}
			MethodNode existingMethod = ASMTreeUtils.getMethod(targetClass, ifaceMethod.name + ifaceMethod.desc);
			if (existingMethod != null) {
				throw CrashReport.create("Target class of accessor already has method with same signature", REPORT_CATEGORY).addDetail("Interface", ifaceType).addDetail("Accessor", signature)
					.addDetail("Existing Method", ASMTreeUtils.getDebugSignature(ASMTreeUtils.getType(targetClass), existingMethod)).exception();
			}
			String accessorTarget = this.getAccessorName(ifaceMethod, ifaceType);
			FieldNode targetField = ASMTreeUtils.getField(targetClass, accessorTarget);
			if (targetField == null) {
				throw CrashReport.create("Target field for accessor was not found in target class", REPORT_CATEGORY).addDetail("Interface", ifaceType).addDetail("Accessor", signature)
					.addDetail("Expected Accessor Target", accessorTarget).exception();
			}
			if (ASMTreeUtils.is(targetField, TypeAccess.PUBLIC)) {
				throw CrashReport.create("Target field for accessor is public, no accessor required", REPORT_CATEGORY).addDetail("Interface", ifaceType).addDetail("Accessor", signature)
					.addDetail("Accessor Target", accessorTarget).exception();
			}
			Type fieldType = ASMTreeUtils.getType(targetField);
			if (!returnType.equals(fieldType)) {
				throw CrashReport.create("Accessor return type does not match target field type", REPORT_CATEGORY).addDetail("Interface", ifaceType).addDetail("Accessor", signature)
					.addDetail("Accessor Target", accessorTarget).addDetail("Expected Type", fieldType).addDetail("Actual Type", returnType).exception();
			}
			String fieldSignature = targetField.signature;
			if (fieldSignature != null && !fieldSignature.isEmpty() && !fieldSignature.isBlank()) {
				String accessorSignature = this.getReturnTypeSignature(ifaceMethod);
				if (!Objects.equals(fieldSignature, accessorSignature)) {
					throw CrashReport.create("Accessor signature does not match target field signature", REPORT_CATEGORY).addDetail("Interface", ifaceType).addDetail("Accessor", signature)
						.addDetail("Accessor Target", accessorTarget).addDetail("Expected Signature", fieldSignature).addDetail("Actual Signature", accessorSignature).exception();
				}
			}
			this.generateAccessor(ifaceMethod, targetField, ASMTreeUtils.getType(targetClass));
		}

		private void generateAccessor(@NotNull MethodNode ifaceMethod, @NotNull FieldNode targetField, @NotNull Type ownerType) {
			MethodVisitor visitor = super.visitMethod(Opcodes.ACC_PUBLIC, ifaceMethod.name, ifaceMethod.desc, ifaceMethod.signature, null);
			Label start = new Label();
			Label end = new Label();
			// Instrument method annotations from the interface method
			if (ifaceMethod.visibleAnnotations != null) {
				for (AnnotationNode annotation : ifaceMethod.visibleAnnotations) {
					AnnotationVisitor av = visitor.visitAnnotation(annotation.desc, true);
					if (annotation.values != null) {
						for (int i = 0; i < annotation.values.size(); i += 2) {
							av.visit((String) annotation.values.get(i), annotation.values.get(i + 1));
						}
					}
					av.visitEnd();
				}
			}
			visitor.visitCode();
			visitor.visitLabel(start);
			visitor.visitVarInsn(Opcodes.ALOAD, 0);
			visitor.visitFieldInsn(Opcodes.GETFIELD, ownerType.getInternalName(), targetField.name, targetField.desc);
			Type returnType = ASMTreeUtils.getReturnType(ifaceMethod);
			visitor.visitInsn(returnType.getOpcode(Opcodes.IRETURN));
			visitor.visitLabel(end);
			String ownerSignature = targetField.signature != null ? targetField.signature : "";
			visitor.visitLocalVariable("this", ownerType.getDescriptor(), ownerSignature, start, end, 0);
			visitor.visitMaxs(0, 0);
			visitor.visitEnd();
			// Note: Cache will be automatically updated with the transformed ClassNode
			// by BaseClassTransformer after the bytecode is written
			this.markModified();
		}

		//region Helper methods
		private @NotNull String getAccessorName(@NotNull MethodNode ifaceMethod, @NotNull Type ifaceType) {
			AnnotationNode annotation = ASMTreeUtils.getAnnotation(ifaceMethod, ACCESSOR);
			String target = ASMTreeUtils.getAnnotationValue(annotation, "target");
			if (target != null) {
				return target;
			}
			String methodName = ifaceMethod.name;
			if (methodName.startsWith("get")) {
				return Utils.uncapitalize(methodName.substring(3));
			} else if (methodName.startsWith("access")) {
				return Utils.uncapitalize(methodName.substring(6));
			}
			return methodName;
		}

		private @NotNull String getReturnTypeSignature(@NotNull MethodNode method) {
			String signature = method.signature;
			if (signature == null || signature.isEmpty()) {
				return "";
			}
			int index = signature.indexOf(')');
			return signature.substring(index + 1);
		}
		//endregion
	}
}
