package net.luis.agent.asm.transformer.implementation;

import net.luis.agent.Agent;
import net.luis.agent.asm.ASMTreeUtils;
import net.luis.agent.asm.ASMUtils;
import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.ContextBasedClassVisitor;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.asm.type.*;
import net.luis.agent.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.*;

import static net.luis.agent.asm.Types.*;

public class InvokerTransformer extends BaseClassTransformer {
	
	private final Map</*Target Class*/String, /*Interfaces*/List<InterfaceTransformer.InterfaceInfo>> lookup = InterfaceTransformer.createLookup(INJECT_INTERFACE);
	
	//region Type filtering
	@Override
	protected boolean shouldIgnoreClass(@NotNull Type type) {
		return !this.lookup.containsKey(type.getInternalName());
	}
	//endregion
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @NotNull ClassWriter writer) {
		return new InvokerVisitor(writer, type, () -> this.modified = true, this.lookup);
	}
	
	private static class InvokerVisitor extends ContextBasedClassVisitor {
		
		private static final String REPORT_CATEGORY = "Invoker Implementation Error";
		
		private final Map</*Target Class*/String, /*Interfaces*/List<InterfaceTransformer.InterfaceInfo>> lookup;
		
		private InvokerVisitor(@NotNull ClassWriter writer, @NotNull Type type, @NotNull Runnable markModified, @NotNull Map<String, List<InterfaceTransformer.InterfaceInfo>> lookup) {
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
				Type targetType = Type.getObjectType(name);
				ClassNode targetClass = Agent.getClass(targetType);
				if (targetClass == null) {
					return;
				}
				for (InterfaceTransformer.InterfaceInfo ifaceInfo : this.lookup.get(name)) {
					ClassNode ifaceClass = ifaceInfo.classNode;
					Type iface = ifaceInfo.type;
					for (MethodNode method : ifaceClass.methods) {
						if (ASMTreeUtils.hasAnnotation(method, INVOKER)) {
							this.validateMethod(method, targetClass);
						} else if (ASMTreeUtils.is(method, TypeAccess.PUBLIC)) {
							List<AnnotationNode> annotations = ASMTreeUtils.getAllAnnotations(method);
							if (annotations.isEmpty()) {
								throw createReport("Found method without annotation, does not know how to implement", iface, ASMTreeUtils.getDebugSignature(iface, method)).exception();
							} else if (annotations.stream().map(a -> Type.getType(a.desc)).noneMatch(IMPLEMENTATION_ANNOTATIONS::contains)) {
								throw createReport("Found method without valid annotation, does not know how to implement", iface, ASMTreeUtils.getDebugSignature(iface, method)).exception();
							}
						}
					}
				}
			}
		}
		
		private void validateMethod(@NotNull MethodNode ifaceMethod, @NotNull ClassNode targetClass) {
			Type ifaceType = ASMTreeUtils.getType(targetClass);
			String signature = ASMTreeUtils.getDebugSignature(ifaceType, ifaceMethod);
			if (!ASMTreeUtils.is(ifaceMethod, TypeAccess.PUBLIC)) {
				throw CrashReport.create("Method annotated with @Invoker must be public", REPORT_CATEGORY).addDetail("Interface", ifaceType).addDetail("Invoker", signature).exception();
			}
			if (ASMTreeUtils.is(ifaceMethod, TypeModifier.STATIC)) {
				throw CrashReport.create("Method annotated with @Invoker must not be static", REPORT_CATEGORY).addDetail("Interface", ifaceType).addDetail("Invoker", signature).exception();
			}
			if (!ASMTreeUtils.is(ifaceMethod, TypeModifier.ABSTRACT)) {
				throw CrashReport.create("Method annotated with @Invoker must not be default implemented", REPORT_CATEGORY).addDetail("Interface", ifaceType).addDetail("Invoker", signature).exception();
			}
			MethodNode existingMethod = ASMTreeUtils.getMethod(targetClass, ASMTreeUtils.getFullSignature(ifaceMethod));
			if (existingMethod != null) {
				throw CrashReport.create("Target class of invoker already has method with same signature", REPORT_CATEGORY).addDetail("Interface", ifaceType).addDetail("Invoker", signature)
					.addDetail("Existing Method", ASMTreeUtils.getDebugSignature(ifaceType, existingMethod)).exception();
			}
			String invokerTarget = this.getInvokerName(ifaceMethod);
			List<MethodNode> possibleTargets = ASMUtils.getBySignature(invokerTarget, targetClass);
			if (possibleTargets.isEmpty()) {
				List<MethodNode> rawTargets = ASMTreeUtils.getMethods(targetClass, this.getRawInvokerName(invokerTarget));
				List<String> possibleTargetSigs = rawTargets.stream().map(mn -> ASMTreeUtils.getDebugSignature(ASMTreeUtils.getType(targetClass), mn)).toList();
				throw CrashReport.create("Could not find target method for invoker", REPORT_CATEGORY).addDetail("Interface", ifaceType).addDetail("Invoker", signature).addDetail("Target", invokerTarget)
					.addDetail("Possible Targets", possibleTargetSigs).exception();
			}
			if (possibleTargets.size() > 1) {
				List<String> possibleTargetSigs = possibleTargets.stream().map(mn -> ASMTreeUtils.getDebugSignature(ASMTreeUtils.getType(targetClass), mn)).toList();
				throw CrashReport.create("Found multiple possible targets for invoker", REPORT_CATEGORY).addDetail("Interface", ifaceType).addDetail("Invoker", signature).addDetail("Target", invokerTarget)
					.addDetail("Possible Targets", possibleTargetSigs).exception();
			}
			MethodNode targetMethod = possibleTargets.getFirst();
			if (ASMTreeUtils.is(targetMethod, TypeAccess.PUBLIC)) {
				throw CrashReport.create("Target method of invoker is public, no invoker required", REPORT_CATEGORY).addDetail("Interface", ifaceType).addDetail("Invoker", signature)
					.addDetail("Target Method", ASMTreeUtils.getDebugSignature(ASMTreeUtils.getType(targetClass), targetMethod)).exception();
			}
			if (!targetMethod.desc.equals(ifaceMethod.desc)) {
				throw CrashReport.create("Invoker method signature does not match target method signature", REPORT_CATEGORY).addDetail("Interface", ifaceType).addDetail("Invoker", signature)
					.addDetail("Target Method", ASMTreeUtils.getDebugSignature(ASMTreeUtils.getType(targetClass), targetMethod)).exception();
			}

			if (!Objects.equals(targetMethod.signature, ifaceMethod.signature)) {
				throw CrashReport.create("Invoker method signature does not match target method signature", REPORT_CATEGORY).addDetail("Interface", ifaceType).addDetail("Invoker", signature)
					.addDetail("Target Method", targetMethod.signature).exception();
			}
			this.generateInvoker(ifaceMethod, targetMethod, targetClass);
		}
		
		private void generateInvoker(@NotNull MethodNode ifaceMethod, @NotNull MethodNode targetMethod, @NotNull ClassNode targetClass) {
			Type targetType = ASMTreeUtils.getType(targetClass);
			MethodVisitor visitor = super.visitMethod(Opcodes.ACC_PUBLIC, ifaceMethod.name, ifaceMethod.desc, ifaceMethod.signature, null);
			Label start = new Label();
			Label end = new Label();
			this.instrumentMethodAnnotations(visitor, ifaceMethod);
			this.instrumentParameterAnnotations(visitor, ifaceMethod);
			visitor.visitCode();
			visitor.visitLabel(start);
			visitor.visitVarInsn(Opcodes.ALOAD, 0);
			Type[] paramTypes = ASMTreeUtils.getParameterTypes(ifaceMethod);
			int paramIndex = 1;
			for (Type paramType : paramTypes) {
				visitor.visitVarInsn(paramType.getOpcode(Opcodes.ILOAD), paramIndex);
				paramIndex += paramType.getSize();
			}
			visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, targetType.getInternalName(), targetMethod.name, targetMethod.desc, false);
			Type returnType = ASMTreeUtils.getReturnType(ifaceMethod);
			visitor.visitInsn(returnType.getOpcode(Opcodes.IRETURN));
			visitor.visitLabel(end);
			visitor.visitLocalVariable("this", targetType.getDescriptor(), ifaceMethod.signature, start, end, 0);
			paramIndex = 1;
			for (int i = 0; i < paramTypes.length; i++) {
				visitor.visitLocalVariable("generated$InvokerTransformer$Temp" + (i + 1), paramTypes[i].getDescriptor(), null, start, end, paramIndex);
				paramIndex += paramTypes[i].getSize();
			}
			visitor.visitMaxs(0, 0);
			visitor.visitEnd();
			this.markModified();
		}
		
		//region Helper methods
		private void instrumentMethodAnnotations(@NotNull MethodVisitor visitor, @NotNull MethodNode methodNode) {
			for (AnnotationNode annotation : ASMTreeUtils.getAllAnnotations(methodNode)) {
				visitor.visitAnnotation(annotation.desc, true);
			}
		}

		private void instrumentParameterAnnotations(@NotNull MethodVisitor visitor, @NotNull MethodNode methodNode) {
			if (methodNode.visibleParameterAnnotations != null) {
				for (int i = 0; i < methodNode.visibleParameterAnnotations.length; i++) {
					if (methodNode.visibleParameterAnnotations[i] != null) {
						for (AnnotationNode annotation : methodNode.visibleParameterAnnotations[i]) {
							visitor.visitParameterAnnotation(i, annotation.desc, true);
						}
					}
				}
			}
			if (methodNode.invisibleParameterAnnotations != null) {
				for (int i = 0; i < methodNode.invisibleParameterAnnotations.length; i++) {
					if (methodNode.invisibleParameterAnnotations[i] != null) {
						for (AnnotationNode annotation : methodNode.invisibleParameterAnnotations[i]) {
							visitor.visitParameterAnnotation(i, annotation.desc, false);
						}
					}
				}
			}
		}

		private @NotNull String getInvokerName(@NotNull MethodNode ifaceMethod) {
			AnnotationNode annotation = ASMTreeUtils.getAnnotation(ifaceMethod, INVOKER);
			Object targetValue = ASMTreeUtils.getAnnotationValue(annotation, "target");
			String target = targetValue instanceof String ? (String) targetValue : null;
			if (target != null) {
				return target;
			}
			String methodName = ifaceMethod.name;
			if (methodName.startsWith("invoke")) {
				return Utils.uncapitalize(methodName.substring(6));
			}
			return methodName;
		}

		private @NotNull String getRawInvokerName(@NotNull String target) {
			if (target.contains("(")) {
				return target.substring(0, target.indexOf('('));
			}
			return target;
		}
		//endregion
	}
}
