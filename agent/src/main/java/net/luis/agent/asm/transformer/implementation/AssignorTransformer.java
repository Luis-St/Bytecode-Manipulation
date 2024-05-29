package net.luis.agent.asm.transformer.implementation;

import net.luis.agent.asm.ASMUtils;
import net.luis.agent.asm.Instrumentations;
import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.visitor.ContextBasedClassVisitor;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.AgentContext;
import net.luis.agent.preload.data.*;
import net.luis.agent.preload.type.TypeAccess;
import net.luis.agent.preload.type.TypeModifier;
import net.luis.agent.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.util.*;

import static net.luis.agent.asm.Types.*;

public class AssignorTransformer extends BaseClassTransformer {
	
	private final Map</*Target Class*/String, /*Interfaces*/List<String>> lookup = ASMUtils.createTargetsLookup(AgentContext.get(), INJECT_INTERFACE);
	
	//region Type filtering
	@Override
	protected boolean shouldIgnoreClass(@NotNull Type type) {
		return !this.lookup.containsKey(type.getInternalName());
	}
	//endregion
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @Nullable Class<?> clazz, @NotNull ClassWriter writer) {
		return new AssignorVisitor(writer, type, () -> this.modified = true, this.lookup);
	}
	
	private static class AssignorVisitor extends ContextBasedClassVisitor {
		
		private static final String REPORT_CATEGORY = "Assignor Implementation Error";
		
		private final Map</*Target Class*/String, /*Interfaces*/List<String>> lookup;
		private final List<String> unfinal = new ArrayList<>();
		
		private AssignorVisitor(@NotNull ClassWriter writer, @NotNull Type type, @NotNull Runnable markModified, @NotNull Map</*Target Class*/String, /*Interfaces*/List<String>> lookup) {
			super(writer, type, markModified);
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
				AgentContext context = AgentContext.get();
				Type target = Type.getObjectType(name);
				ClassData targetData = context.getClassData(target);
				for (Type iface : this.lookup.get(name).stream().map(Type::getObjectType).toList()) {
					ClassData ifaceData = context.getClassData(iface);
					for (MethodData method : ifaceData.methods()) {
						if (method.isAnnotatedWith(ASSIGNOR)) {
							this.validateMethod(iface, method, target, targetData);
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
		
		private @NotNull String getAssignorName(@NotNull MethodData ifaceMethod) {
			AnnotationData annotation = ifaceMethod.getAnnotation(ASSIGNOR);
			String target = annotation.get("target");
			if (target != null) {
				return target;
			}
			String methodName = ifaceMethod.name();
			if (methodName.startsWith("set")) {
				return Utils.uncapitalize(methodName.substring(3));
			} else if (methodName.startsWith("assign")) {
				return Utils.uncapitalize(methodName.substring(6));
			}
			return methodName;
		}
		
		private void validateMethod(@NotNull Type iface, @NotNull MethodData ifaceMethod, @NotNull Type target, @NotNull ClassData targetData) {
			String signature = ifaceMethod.getMethodSignature();
			//region Base validation
			if (ifaceMethod.access() != TypeAccess.PUBLIC) {
				throw CrashReport.create("Method annotated with @Assignor must be public", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Assignor", signature).exception();
			}
			if (ifaceMethod.is(TypeModifier.STATIC)) {
				throw CrashReport.create("Method annotated with @Assignor must not be static", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Assignor", signature).exception();
			}
			if (!ifaceMethod.is(TypeModifier.ABSTRACT)) {
				throw CrashReport.create("Method annotated with @Assignor must not be default implemented", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Assignor", signature).exception();
			}
			//endregion
			if (!ifaceMethod.returns(Type.VOID_TYPE)) {
				throw CrashReport.create("Method annotated with @Assignor must return void", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Assignor", signature).exception();
			}
			if (ifaceMethod.getParameterCount() != 1) {
				throw CrashReport.create("Method annotated with @Assignor must have exactly one parameter", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Assignor", signature).exception();
			}
			if (ifaceMethod.getExceptionCount() > 0) {
				throw CrashReport.create("Method annotated with @Assignor must not throw exceptions", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Assignor", signature)
					.addDetail("Exceptions", ifaceMethod.exceptions()).exception();
			}
			MethodData existingMethod = targetData.getMethod(ifaceMethod.name(), ifaceMethod.type());
			if (existingMethod != null) {
				throw CrashReport.create("Target class of assignor already has method with same signature", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Assignor", signature)
					.addDetail("Existing Method", existingMethod.getMethodSignature()).exception();
			}
			String accessorTarget = this.getAssignorName(ifaceMethod);
			FieldData targetField = targetData.getField(accessorTarget);
			if (targetField == null) {
				throw CrashReport.create("Target field for assignor was not found in target class", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Assignor", signature)
					.addDetail("Expected Accessor Target", accessorTarget).exception();
			}
			if (targetField.access() == TypeAccess.PUBLIC && !targetField.is(TypeModifier.FINAL)) {
				throw CrashReport.create("Target field for assignor is public and not final, no assignor required", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Assignor", signature)
					.addDetail("Accessor Target", accessorTarget).exception();
			}
			if (!targetField.is(ifaceMethod.getParameterType(0))) {
				throw CrashReport.create("Assignor parameter type does not match target field type", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Assignor", signature)
					.addDetail("Accessor Target", accessorTarget).addDetail("Expected Type", targetField.type()).addDetail("Actual Type", ifaceMethod.getReturnType()).exception();
			}
			String fieldSignature = targetField.signature();
			if (fieldSignature != null && !fieldSignature.isBlank()) {
				String assignorSignature = ASMUtils.getParameterTypesSignature(ifaceMethod);
				if (!Objects.equals(fieldSignature, assignorSignature)) {
					throw CrashReport.create("Assignor signature does not match target field signature", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Assignor", signature)
						.addDetail("Accessor Target", accessorTarget).addDetail("Expected Signature", fieldSignature).addDetail("Actual Signature", assignorSignature).exception();
				}
			}
			this.generateAssignor(ifaceMethod, target, targetField);
		}
		
		@SuppressWarnings("DuplicatedCode")
		private void generateAssignor(@NotNull MethodData ifaceMethod, @NotNull Type target, @NotNull FieldData targetField) {
			if (targetField.is(TypeModifier.FINAL)) {
				this.unfinal.add(targetField.name());
			}
			MethodVisitor visitor = super.visitMethod(Opcodes.ACC_PUBLIC, ifaceMethod.name(), ifaceMethod.type().getDescriptor(), ifaceMethod.signature(), null);
			Label start = new Label();
			Label end = new Label();
			Instrumentations.instrumentMethodAnnotations(visitor, ifaceMethod);
			Instrumentations.instrumentParameterAnnotations(visitor, ifaceMethod);
			visitor.visitCode();
			visitor.visitLabel(start);
			visitor.visitVarInsn(Opcodes.ALOAD, 0);
			visitor.visitVarInsn(ifaceMethod.parameters().getFirst().type().getOpcode(Opcodes.ILOAD), 1);
			visitor.visitFieldInsn(Opcodes.PUTFIELD, target.getInternalName(), targetField.name(), targetField.type().getDescriptor());
			visitor.visitInsn(Opcodes.RETURN);
			visitor.visitLabel(end);
			visitor.visitLocalVariable("this", target.getDescriptor(), targetField.signature(), start, end, 0);
			visitor.visitLocalVariable("generated$AssignorTransformer$Temp" + 1, targetField.type().getDescriptor(), null, start, end, 1);
			visitor.visitMaxs(0, 0);
			visitor.visitEnd();
			this.updateClass(ifaceMethod, target, targetField);
			this.markModified();
		}
		
		private void updateClass(@NotNull MethodData ifaceMethod, @NotNull Type target, @NotNull FieldData targetField) {
			ClassData data = AgentContext.get().getClassData(target);
			data.methods().add(ifaceMethod.copy(EnumSet.noneOf(TypeModifier.class)));
			targetField.modifiers().remove(TypeModifier.FINAL);
		}
		
		@Override
		public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
			if (this.unfinal.contains(name)) {
				access = access & ~Opcodes.ACC_FINAL;
				this.markModified();
			}
			return super.visitField(access, name, descriptor, signature, value);
		}
	}
}
