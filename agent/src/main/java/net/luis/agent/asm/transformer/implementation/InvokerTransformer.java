package net.luis.agent.asm.transformer.implementation;

import net.luis.agent.AgentContext;
import net.luis.agent.asm.ASMUtils;
import net.luis.agent.asm.Instrumentations;
import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.ContextBasedClassVisitor;
import net.luis.agent.asm.data.Class;
import net.luis.agent.asm.data.*;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.asm.type.TypeAccess;
import net.luis.agent.asm.type.TypeModifier;
import net.luis.agent.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.util.*;

import static net.luis.agent.asm.Types.*;

public class InvokerTransformer extends BaseClassTransformer {
	
	private final Map</*Target Class*/String, /*Interfaces*/List<String>> lookup = ASMUtils.createTargetsLookup(INJECT_INTERFACE);
	
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
		
		private final Map</*Target Class*/String, /*Interfaces*/List<String>> lookup;
		
		private InvokerVisitor(@NotNull ClassWriter writer, @NotNull Type type, @NotNull Runnable markModified, @NotNull Map</*Target Class*/String, /*Interfaces*/List<String>> lookup) {
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
				AgentContext context = AgentContext.get();
				Class targetClass = context.getClass(Type.getObjectType(name));
				for (Type iface : this.lookup.get(name).stream().map(Type::getObjectType).toList()) {
					Class ifaceClass = context.getClass(iface);
					for (Method method : ifaceClass.getMethods().values()) {
						if (method.isAnnotatedWith(INVOKER)) {
							this.validateMethod(method, targetClass);
						} else if (method.is(TypeAccess.PUBLIC)) {
							if (method.getAnnotations().isEmpty()) {
								throw createReport("Found method without annotation, does not know how to implement", iface, method.getSourceSignature()).exception();
							} else if (method.getAnnotations().values().stream().map(Annotation::getType).noneMatch(IMPLEMENTATION_ANNOTATIONS::contains)) {
								throw createReport("Found method without valid annotation, does not know how to implement", iface, method.getSourceSignature()).exception();
							}
						}
					}
				}
			}
		}
		
		private void validateMethod(@NotNull Method ifaceMethod, @NotNull Class targetClass) {
			String signature = ifaceMethod.getSourceSignature();
			//region Base validation
			if (!ifaceMethod.is(TypeAccess.PUBLIC)) {
				throw CrashReport.create("Method annotated with @Invoker must be public", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Invoker", signature).exception();
			}
			if (ifaceMethod.is(TypeModifier.STATIC)) {
				throw CrashReport.create("Method annotated with @Invoker must not be static", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Invoker", signature).exception();
			}
			if (!ifaceMethod.is(TypeModifier.ABSTRACT)) {
				throw CrashReport.create("Method annotated with @Invoker must not be default implemented", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Invoker", signature).exception();
			}
			//endregion
			Method existingMethod = targetClass.getMethod(ifaceMethod.getFullSignature());
			if (existingMethod != null) {
				throw CrashReport.create("Target class of invoker already has method with same signature", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Invoker", signature)
					.addDetail("Existing Method", existingMethod.getSourceSignature()).exception();
			}
			String invokerTarget = this.getInvokerName(ifaceMethod);
			List<Method> possibleTargets = ASMUtils.getBySignature(invokerTarget, targetClass);
			if (possibleTargets.isEmpty()) {
				throw CrashReport.create("Could not find target method for invoker", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Invoker", signature).addDetail("Target", invokerTarget)
					.addDetail("Possible Targets", targetClass.getMethods(this.getRawInvokerName(invokerTarget)).stream().map(Method::getSourceSignature).toList()).exception();
			}
			if (possibleTargets.size() > 1) {
				throw CrashReport.create("Found multiple possible targets for invoker", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Invoker", signature).addDetail("Target", invokerTarget)
					.addDetail("Possible Targets", possibleTargets.stream().map(Method::getSourceSignature).toList()).exception();
			}
			Method targetMethod = possibleTargets.getFirst();
			if (targetMethod.is(TypeAccess.PUBLIC)) {
				throw CrashReport.create("Target method of invoker is public, no invoker required", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Invoker", signature)
					.addDetail("Target Method", targetMethod.getSourceSignature()).exception();
			}
			if (!targetMethod.is(ifaceMethod.getType())) {
				throw CrashReport.create("Invoker method signature does not match target method signature", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Invoker", signature)
					.addDetail("Target Method", targetMethod.getSourceSignature()).exception();
			}
			
			if (!Objects.equals(targetMethod.getGenericSignature(), ifaceMethod.getGenericSignature())) {
				throw CrashReport.create("Invoker method signature does not match target method signature", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Invoker", signature)
					.addDetail("Target Method", targetMethod.getGenericSignature()).exception();
			}
			this.generateInvoker(ifaceMethod, targetMethod);
		}
		
		private void generateInvoker(@NotNull Method ifaceMethod, @NotNull Method targetMethod) {
			MethodVisitor visitor = super.visitMethod(Opcodes.ACC_PUBLIC, ifaceMethod.getName(), ifaceMethod.getType().getDescriptor(), ifaceMethod.getGenericSignature(), null);
			Label start = new Label();
			Label end = new Label();
			Instrumentations.instrumentMethodAnnotations(visitor, ifaceMethod);
			Instrumentations.instrumentParameterAnnotations(visitor, ifaceMethod);
			visitor.visitCode();
			visitor.visitLabel(start);
			visitor.visitVarInsn(Opcodes.ALOAD, 0);
			for (int i = 0; i < ifaceMethod.getParameterCount(); i++) {
				visitor.visitVarInsn(ifaceMethod.getParameter(i).getType().getOpcode(Opcodes.ILOAD), i + 1);
			}
			visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, targetMethod.getOwner().getInternalName(), targetMethod.getName(), targetMethod.getType().getDescriptor(), false);
			visitor.visitInsn(ifaceMethod.getReturnType().getOpcode(Opcodes.IRETURN));
			visitor.visitLabel(end);
			visitor.visitLocalVariable("this", targetMethod.getOwner().getDescriptor(), ifaceMethod.getGenericSignature(), start, end, 0);
			for (int i = 0; i < ifaceMethod.getParameterCount(); i++) {
				visitor.visitLocalVariable("generated$InvokerTransformer$Temp" + (i + 1), ifaceMethod.getParameter(i).getType().getDescriptor(), null, start, end, i + 1);
			}
			visitor.visitMaxs(0, 0);
			visitor.visitEnd();
			this.updateClass(ifaceMethod, targetMethod.getOwner());
			this.markModified();
		}
		
		//region Helper methods
		private @NotNull String getInvokerName(@NotNull Method ifaceMethod) {
			Annotation annotation = ifaceMethod.getAnnotation(INVOKER);
			String target = annotation.get("target");
			if (target != null) {
				return target;
			}
			String methodName = ifaceMethod.getName();
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
		
		private void updateClass(@NotNull Method ifaceMethod, @NotNull Type target) {
			Class data = AgentContext.get().getClass(target);
			data.getMethods().put(ifaceMethod.getFullSignature(), Method.builder(ifaceMethod).modifiers(EnumSet.noneOf(TypeModifier.class)).build());
		}
		//endregion
	}
}
