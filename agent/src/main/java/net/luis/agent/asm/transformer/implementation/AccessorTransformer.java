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

public class AccessorTransformer extends BaseClassTransformer {
	
	private final Map</*Target Class*/String, /*Interfaces*/List<String>> lookup = ASMUtils.createTargetsLookup(INJECT_INTERFACE);
	
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
		
		private AccessorVisitor(@NotNull ClassWriter writer, @NotNull Type type, @NotNull Runnable markModified, @NotNull Map</*Target Class*/String, /*Interfaces*/List<String>> lookup) {
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
						if (method.isAnnotatedWith(ACCESSOR)) {
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
				throw CrashReport.create("Method annotated with @Accessor must be public", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Accessor", signature).exception();
			}
			if (ifaceMethod.is(TypeModifier.STATIC)) {
				throw CrashReport.create("Method annotated with @Accessor must not be static", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Accessor", signature).exception();
			}
			if (!ifaceMethod.is(TypeModifier.ABSTRACT)) {
				throw CrashReport.create("Method annotated with @Accessor must not be default implemented", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Accessor", signature).exception();
			}
			//endregion
			if (ifaceMethod.returns(Type.VOID_TYPE)) {
				throw CrashReport.create("Method annotated with @Accessor has void return type", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Accessor", signature).exception();
			}
			if (ifaceMethod.getParameterCount() > 0) {
				throw CrashReport.create("Method annotated with @Accessor must not have parameters", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Accessor", signature).exception();
			}
			if (ifaceMethod.getExceptionCount() > 0) {
				throw CrashReport.create("Method annotated with @Accessor must not throw exceptions", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Accessor", signature)
					.addDetail("Exceptions", ifaceMethod.getExceptions()).exception();
			}
			Method existingMethod = targetClass.getMethod(ifaceMethod.getFullSignature());
			if (existingMethod != null) {
				throw CrashReport.create("Target class of accessor already has method with same signature", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Accessor", signature)
					.addDetail("Existing Method", existingMethod.getSourceSignature()).exception();
			}
			String accessorTarget = this.getAccessorName(ifaceMethod);
			Field targetField = targetClass.getField(accessorTarget);
			if (targetField == null) {
				throw CrashReport.create("Target field for accessor was not found in target class", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Accessor", signature)
					.addDetail("Expected Accessor Target", accessorTarget).exception();
			}
			if (targetField.is(TypeAccess.PUBLIC)) {
				throw CrashReport.create("Target field for accessor is public, no accessor required", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Accessor", signature)
					.addDetail("Accessor Target", accessorTarget).exception();
			}
			if (!ifaceMethod.returns(targetField.getType())) {
				throw CrashReport.create("Accessor return type does not match target field type", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Accessor", signature)
					.addDetail("Accessor Target", accessorTarget).addDetail("Expected Type", targetField.getType()).addDetail("Actual Type", ifaceMethod.getReturnType()).exception();
			}
			String fieldSignature = targetField.getGenericSignature();
			if (fieldSignature != null && !fieldSignature.isBlank()) {
				String accessorSignature = this.getReturnTypeSignature(ifaceMethod);
				if (!Objects.equals(fieldSignature, accessorSignature)) {
					throw CrashReport.create("Accessor signature does not match target field signature", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Accessor", signature)
						.addDetail("Accessor Target", accessorTarget).addDetail("Expected Signature", fieldSignature).addDetail("Actual Signature", accessorSignature).exception();
				}
			}
			this.generateAccessor(ifaceMethod, targetField);
		}
		
		private void generateAccessor(@NotNull Method ifaceMethod, @NotNull Field targetField) {
			MethodVisitor visitor = super.visitMethod(Opcodes.ACC_PUBLIC, ifaceMethod.getName(), ifaceMethod.getType().getDescriptor(), ifaceMethod.getGenericSignature(), null);
			Label start = new Label();
			Label end = new Label();
			Instrumentations.instrumentMethodAnnotations(visitor, ifaceMethod);
			Instrumentations.instrumentParameterAnnotations(visitor, ifaceMethod);
			visitor.visitCode();
			visitor.visitLabel(start);
			visitor.visitVarInsn(Opcodes.ALOAD, 0);
			visitor.visitFieldInsn(Opcodes.GETFIELD, targetField.getOwner().getInternalName(), targetField.getName(), targetField.getType().getDescriptor());
			visitor.visitInsn(ifaceMethod.getReturnType().getOpcode(Opcodes.IRETURN));
			visitor.visitLabel(end);
			visitor.visitLocalVariable("this", targetField.getOwner().getDescriptor(), targetField.getGenericSignature(), start, end, 0);
			visitor.visitMaxs(0, 0);
			visitor.visitEnd();
			this.updateClass(ifaceMethod, targetField.getOwner());
			this.markModified();
		}
		
		//region Helper methods
		private @NotNull String getAccessorName(@NotNull Method ifaceMethod) {
			Annotation annotation = ifaceMethod.getAnnotation(ACCESSOR);
			String target = annotation.get("target");
			if (target != null) {
				return target;
			}
			String methodName = ifaceMethod.getName();
			if (methodName.startsWith("get")) {
				return Utils.uncapitalize(methodName.substring(3));
			} else if (methodName.startsWith("access")) {
				return Utils.uncapitalize(methodName.substring(6));
			}
			return methodName;
		}
		
		private @NotNull String getReturnTypeSignature(@NotNull Method method) {
			String signature = method.getGenericSignature();
			if (signature == null || signature.isEmpty()) {
				return "";
			}
			int index = signature.indexOf(')');
			return signature.substring(index + 1);
		}
		
		private void updateClass(@NotNull Method ifaceMethod, @NotNull Type target) {
			Class data = AgentContext.get().getClass(target);
			data.getMethods().put(ifaceMethod.getFullSignature(), Method.builder(ifaceMethod).modifiers(EnumSet.of(TypeModifier.ABSTRACT)).build());
		}
		//endregion
	}
}
