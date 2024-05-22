package net.luis.agent.asm.transformer.implementation;

import net.luis.agent.asm.ASMUtils;
import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.visitor.BaseClassVisitor;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.preload.PreloadContext;
import net.luis.agent.preload.data.*;
import net.luis.agent.preload.type.*;
import net.luis.agent.util.Mutable;
import net.luis.agent.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.util.*;

import static net.luis.agent.asm.Types.*;

public class InvokerTransformer extends BaseClassTransformer {
	
	public InvokerTransformer(@NotNull PreloadContext context) {
		super(context);
	}
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @Nullable Class<?> clazz, @NotNull ClassWriter writer) {
		return new InvokerVisitor(writer, this.context, type, () -> this.modified = true, ASMUtils.createTargetsLookup(this.context, INJECT_INTERFACE));
	}
	
	private static class InvokerVisitor extends BaseClassVisitor {
		
		private static final String REPORT_CATEGORY = "Invoker Implementation Error";
		
		private final Map</*Target Class*/String, /*Interfaces*/List<String>> lookup;
		
		private InvokerVisitor(@NotNull ClassWriter writer, @NotNull PreloadContext context, @NotNull Type type, @NotNull Runnable markModified, @NotNull Map</*Target Class*/String, /*Interfaces*/List<String>> lookup) {
			super(writer, context, type, markModified);
			this.lookup = lookup;
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
						} else if (method.is(TypeAccess.PUBLIC)) {
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
			String target = annotation.get("target");
			if (target != null) {
				return target;
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
			if (!targetMethod.is(ifaceMethod.type())) {
				throw CrashReport.create("Invoker method signature does not match target method signature", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Invoker", signature)
					.addDetail("Target Method", targetMethod.getMethodSignature()).exception();
			}
			
			if (!Objects.equals(targetMethod.signature(), ifaceMethod.signature())) {
				throw CrashReport.create("Invoker method signature does not match target method signature", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Invoker", signature)
					.addDetail("Target Method", targetMethod.signature()).exception();
			}
			this.generateInvoker(ifaceMethod, target, targetMethod);
		}
		
		@SuppressWarnings("DuplicatedCode")
		private void generateInvoker(@NotNull MethodData ifaceMethod, @NotNull Type target, @NotNull MethodData targetMethod) {
			MethodVisitor visitor = super.visitMethod(Opcodes.ACC_PUBLIC, ifaceMethod.name(), ifaceMethod.type().getDescriptor(), ifaceMethod.signature(), null);
			this.instrumentMethodAnnotations(visitor, ifaceMethod, true);
			this.instrumentParameterAnnotations(visitor, ifaceMethod);
			visitor.visitCode();
			visitor.visitVarInsn(Opcodes.ALOAD, 0);
			for (int i = 0; i < ifaceMethod.getParameterCount(); i++) {
				visitor.visitVarInsn(ifaceMethod.getParameterType(i).getOpcode(Opcodes.ILOAD), i + 1); // 0 is this
			}
			visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, target.getInternalName(), targetMethod.name(), targetMethod.type().getDescriptor(), false);
			visitor.visitInsn(ifaceMethod.getReturnType().getOpcode(Opcodes.IRETURN));
			visitor.visitLocalVariable("this", target.getDescriptor(), ifaceMethod.signature(), new Label(), new Label(), 0);
			for (int i = 0; i < ifaceMethod.getParameterCount(); i++) {
				visitor.visitLocalVariable("arg" + i, ifaceMethod.getParameterType(i).getDescriptor(), null, new Label(), new Label(), i + 1); // 0 is this
			}
			visitor.visitMaxs(0, 0);
			visitor.visitEnd();
			this.updateClass(ifaceMethod, target);
			this.markModified();
		}
		
		@SuppressWarnings("DuplicatedCode")
		private void updateClass(@NotNull MethodData ifaceMethod, @NotNull Type target) {
			ClassContent content = this.context.getClassContent(target);
			MethodData method = new MethodData(ifaceMethod.name(), ifaceMethod.type(), ifaceMethod.signature(), TypeAccess.PUBLIC, MethodType.METHOD, EnumSet.noneOf(TypeModifier.class), ifaceMethod.annotations(), ifaceMethod.parameters(), new ArrayList<>(), new Mutable<>());
			content.methods().add(method);
		}
	}
}
