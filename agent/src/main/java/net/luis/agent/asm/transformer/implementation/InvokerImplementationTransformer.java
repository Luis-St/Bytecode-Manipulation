package net.luis.agent.asm.transformer.implementation;

import net.luis.agent.asm.ASMUtils;
import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.visitor.BaseClassVisitor;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.preload.PreloadContext;
import net.luis.agent.preload.data.*;
import net.luis.agent.preload.type.TypeAccess;
import net.luis.agent.preload.type.TypeModifier;
import net.luis.agent.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.util.*;

import static net.luis.agent.asm.Types.*;

public class InvokerImplementationTransformer extends BaseClassTransformer {
	
	private final PreloadContext context;
	
	public InvokerImplementationTransformer(@NotNull PreloadContext context) {
		this.context = context;
	}
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @Nullable Class<?> clazz, @NotNull ClassReader reader, @NotNull ClassWriter writer) {
		return new InvokerImplementationVisitor(writer, this.context, ASMUtils.createTargetsLookup(this.context, INJECT_INTERFACE), () -> this.modified = true);
	}
	
	private static class InvokerImplementationVisitor extends BaseClassVisitor {
		
		private static final String REPORT_CATEGORY = "Invoker Implementation Error";
		
		private final PreloadContext context;
		private final Map</*Target Class*/String, /*Interfaces*/List<String>> lookup;
		private final Runnable markedModified;
		
		protected InvokerImplementationVisitor(@NotNull ClassWriter writer, @NotNull PreloadContext context, @NotNull Map</*Target Class*/String, /*Interfaces*/List<String>> lookup, Runnable markedModified) {
			super(writer);
			this.context = context;
			this.lookup = lookup;
			this.markedModified = markedModified;
		}
		
		private static @NotNull CrashReport createReport(@NotNull String message, @NotNull Type iface, @NotNull String methodSignature) {
			return CrashReport.create(message, REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Interface Method", methodSignature);
		}
		
		@Override
		@SuppressWarnings("DuplicatedCode")
		public void visit(int version, int access, @NotNull String name, @Nullable String signature, @Nullable String superClass, String @Nullable [] interfaces) {
			super.visit(version, access, name, signature, superClass, interfaces);
			if (this.lookup.containsKey(name)) {
				Type target = Type.getObjectType(name);
				ClassContent targetContent = this.context.getClassContent(target);
				for (Type iface : this.lookup.get(name).stream().map(Type::getObjectType).toList()) {
					ClassContent ifaceContent = this.context.getClassContent(iface);
					for (MethodData method : ifaceContent.methods()) {
						if (method.isAnnotatedWith(INVOKER)) {
							this.validateMethod(iface, method, target, targetContent);
						} else if (method.access() == TypeAccess.PUBLIC && method.is(TypeModifier.ABSTRACT)) {
							if (method.getAnnotations().isEmpty()) {
								throw createReport("Found method without annotation, does not know how to implement", iface, method.getMethodSignature()).exception();
							} else if (method.getAnnotations().stream().map(AnnotationData::type).noneMatch(IMPLEMENTATION_ANNOTATIONS::contains)) {
								throw createReport("Found method without valid annotation, does not know how to implement", iface, method.getMethodSignature()).exception();
							}
						}
					}
				}
			}
		}
		
		private @NotNull String getInvokerName(@NotNull MethodData ifaceMethod) {
			AnnotationData annotation = ifaceMethod.getAnnotation(INVOKER);
			if (annotation.has("target", String.class)) {
				return annotation.get("target");
			}
			String methodName = ifaceMethod.name();
			if (methodName.startsWith("invoke")) {
				return Utils.uncapitalize(methodName.substring(6));
			}
			return methodName;
		}
		
		private @NotNull String getRawInvokerName(@NotNull String invokerTarget) {
			if (invokerTarget.contains("(")) {
				return invokerTarget.substring(0, invokerTarget.indexOf('('));
			}
			return invokerTarget;
		}
		
		private void validateMethod(@NotNull Type iface, @NotNull MethodData ifaceMethod, @NotNull Type target, @NotNull ClassContent targetContent) {
			//System.out.println("Validating Invoker - " + ifaceMethod.name() + " - " + iface.getInternalName());
			String signature = ifaceMethod.getMethodSignature();
			//region Base validation
			if (ifaceMethod.access() != TypeAccess.PUBLIC) {
				throw CrashReport.create("Method annotated with @Invoker must be public", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Invoker", signature).exception();
			}
			if (ifaceMethod.is(TypeModifier.STATIC)) {
				throw CrashReport.create("Method annotated with @Invoker must not be static", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Invoker", signature).exception();
			}
			if (!ifaceMethod.is(TypeModifier.ABSTRACT)) {
				throw CrashReport.create("Method annotated with @Invoker must not be default implemented", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Invoker", signature).exception();
			}
			//endregion
			MethodData existingMethod = targetContent.getMethod(ifaceMethod.name(), ifaceMethod.type());
			if (existingMethod != null) {
				throw CrashReport.create("Target class of invoker already has method with same signature", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Invoker", signature)
					.addDetail("Existing Method", existingMethod.getMethodSignature()).exception();
			}
			String invokerTarget = this.getInvokerName(ifaceMethod);
			List<MethodData> possibleTargets = ASMUtils.getBySignature(invokerTarget, targetContent);
			if (possibleTargets.isEmpty()) {
				throw CrashReport.create("Could not find target method for invoker", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Invoker", signature).addDetail("Target", invokerTarget)
					.addDetail("Possible Targets", targetContent.getMethods(this.getRawInvokerName(invokerTarget)).stream().map(MethodData::getMethodSignature).toList()).exception();
			}
			if (possibleTargets.size() > 1) {
				throw CrashReport.create("Found multiple possible targets for invoker", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Invoker", signature).addDetail("Target", invokerTarget)
					.addDetail("Possible Targets", possibleTargets.stream().map(MethodData::getMethodSignature).toList()).exception();
			}
			MethodData targetMethod = possibleTargets.getFirst();
			if (targetMethod.access() == TypeAccess.PUBLIC) {
				throw CrashReport.create("Target method of invoker is public, no invoker required", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Invoker", signature)
					.addDetail("Target Method", targetMethod.getMethodSignature()).exception();
			}
			if (!Objects.equals(targetMethod.type(), ifaceMethod.type())) {
				throw CrashReport.create("Invoker method signature does not match target method signature", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Invoker", signature)
					.addDetail("Target Method", targetMethod.getMethodSignature()).exception();
			}
			
			if (!Objects.equals(targetMethod.signature(), ifaceMethod.signature())) {
				throw CrashReport.create("Invoker method signature does not match target method signature", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Invoker", signature)
					.addDetail("Target Method", targetMethod.signature()).exception();
			}
			this.generateInvoker(ifaceMethod, target, targetMethod);
		}
		
		private void generateInvoker(@NotNull MethodData ifaceMethod, @NotNull Type target, @NotNull MethodData targetMethod) {
			//System.out.println("Generating Invoker");
			MethodVisitor method = super.visitMethod(Opcodes.ACC_PUBLIC, ifaceMethod.name(), ifaceMethod.type().getDescriptor(), ifaceMethod.signature(), null);
			ASMUtils.addMethodAnnotations(method, ifaceMethod);
			ASMUtils.addParameterAnnotations(method, ifaceMethod);
			method.visitCode();
			method.visitVarInsn(Opcodes.ALOAD, 0);
			for (int i = 0; i < ifaceMethod.getParameterCount(); i++) {
				method.visitVarInsn(ifaceMethod.getParameterType(i).getOpcode(Opcodes.ILOAD), i + 1); // 0 is this
			}
			method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, target.getInternalName(), targetMethod.name(), targetMethod.type().getDescriptor(), false);
			method.visitInsn(ifaceMethod.getReturnType().getOpcode(Opcodes.IRETURN));
			method.visitLocalVariable("this", target.getDescriptor(), ifaceMethod.signature(), new Label(), new Label(), 0);
			for (int i = 0; i < ifaceMethod.getParameterCount(); i++) {
				method.visitLocalVariable("arg" + i, ifaceMethod.getParameterType(i).getDescriptor(), null, new Label(), new Label(), i + 1); // 0 is this
			}
			int max = ifaceMethod.getParameterCount() + 1;
			method.visitMaxs(max, max);
			method.visitEnd();
			this.markedModified.run();
		}
	}
}
