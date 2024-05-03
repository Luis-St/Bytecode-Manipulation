package net.luis.asm.transformer.implementation;

import net.luis.asm.ASMUtils;
import net.luis.asm.base.BaseClassTransformer;
import net.luis.asm.base.visitor.BaseClassVisitor;
import net.luis.asm.report.CrashReport;
import net.luis.preload.PreloadContext;
import net.luis.preload.data.*;
import net.luis.preload.type.TypeAccess;
import net.luis.preload.type.TypeModifier;
import net.luis.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.util.*;

import static net.luis.asm.Types.*;

public class AssignorImplementationTransformer extends BaseClassTransformer {
	
	private final PreloadContext context;
	
	public AssignorImplementationTransformer(@NotNull PreloadContext context) {
		this.context = context;
	}
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull String className, @Nullable Class<?> clazz, @NotNull ClassReader reader, @NotNull ClassWriter writer) {
		return new AssignorImplementationVisitor(writer, this.context, ASMUtils.createTargetsLookup(this.context, INJECT_INTERFACE), () -> this.modified = true);
	}
	
	private static class AssignorImplementationVisitor extends BaseClassVisitor {
		
		private static final String REPORT_CATEGORY = "Assignor Implementation Error";
		
		private final PreloadContext context;
		private final Map</*Target Class*/String, /*Interfaces*/List<String>> lookup;
		private final Runnable markedModified;
		private final List<String> unfinal = new ArrayList<>();
		
		protected AssignorImplementationVisitor(@NotNull ClassWriter writer, @NotNull PreloadContext context, @NotNull Map</*Target Class*/String, /*Interfaces*/List<String>> lookup, Runnable markedModified) {
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
						if (method.isAnnotatedWith(ASSIGNOR)) {
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
		
		private @NotNull String getAssignorName(@NotNull MethodData ifaceMethod) {
			AnnotationData annotation = ifaceMethod.getAnnotation(ASSIGNOR);
			if (annotation.has("target", String.class)) {
				return annotation.get("target");
			}
			String methodName = ifaceMethod.name();
			if (methodName.startsWith("set")) {
				return Utils.uncapitalize(methodName.substring(3));
			} else if (methodName.startsWith("assign")) {
				return Utils.uncapitalize(methodName.substring(6));
			}
			return methodName;
		}
		
		private void validateMethod(@NotNull Type iface, @NotNull MethodData ifaceMethod, @NotNull Type target, @NotNull ClassContent targetContent) {
			//System.out.println("Validating Assignor - " + ifaceMethod.name() + " - " + iface.getInternalName());
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
			if (!Type.VOID_TYPE.equals(ifaceMethod.getReturnType())) {
				throw CrashReport.create("Method annotated with @Assignor must return void", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Assignor", signature).exception();
			}
			if (ifaceMethod.getParameterCount() != 1) {
				throw CrashReport.create("Method annotated with @Assignor must have exactly one parameter", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Assignor", signature).exception();
			}
			if (ifaceMethod.getExceptionCount() > 0) {
				throw CrashReport.create("Method annotated with @Assignor must not throw exceptions", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Assignor", signature)
					.addDetail("Exceptions", ifaceMethod.exceptions()).exception();
			}
			MethodData existingMethod = targetContent.getMethod(ifaceMethod.name(), ifaceMethod.type());
			if (existingMethod != null) {
				throw CrashReport.create("Target class of assignor already has method with same signature", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Assignor", signature)
					.addDetail("Existing Method", existingMethod.getMethodSignature()).exception();
			}
			String accessorTarget = this.getAssignorName(ifaceMethod);
			if (!targetContent.hasField(accessorTarget)) {
				throw CrashReport.create("Target field for assignor was not found in target class", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Assignor", signature)
					.addDetail("Expected Accessor Target", accessorTarget).exception();
			}
			FieldData targetField = targetContent.getField(accessorTarget);
			if (targetField.access() == TypeAccess.PUBLIC && !targetField.is(TypeModifier.FINAL)) {
				throw CrashReport.create("Target field for assignor is public and not final, no assignor required", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Assignor", signature)
					.addDetail("Accessor Target", accessorTarget).exception();
			}
			if (!Objects.equals(targetField.type(), ifaceMethod.getParameterType(0))) {
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
		
		private void generateAssignor(@NotNull MethodData ifaceMethod, @NotNull Type target, @NotNull FieldData targetField) {
			if (targetField.is(TypeModifier.FINAL)) {
				this.unfinal.add(targetField.name());
			}
			//System.out.println("Generating Assignor");
			MethodVisitor method = super.visitMethod(Opcodes.ACC_PUBLIC, ifaceMethod.name(), ifaceMethod.type().getDescriptor(), ifaceMethod.signature(), null);
			ASMUtils.addMethodAnnotations(method, ifaceMethod);
			ASMUtils.addParameterAnnotations(method, ifaceMethod);
			method.visitCode();
			method.visitVarInsn(Opcodes.ALOAD, 0);
			method.visitVarInsn(Opcodes.ALOAD, 1);
			method.visitFieldInsn(Opcodes.PUTFIELD, target.getInternalName(), targetField.name(), targetField.type().getDescriptor());
			method.visitInsn(Opcodes.RETURN);
			method.visitLocalVariable("this", target.getDescriptor(), targetField.signature(), new Label(), new Label(), 0);
			method.visitLocalVariable(targetField.name(), targetField.type().getDescriptor(), null, new Label(), new Label(), 1);
			method.visitMaxs(2, 2);
			method.visitEnd();
			this.markedModified.run();
		}
		
		@Override
		public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
			if (this.unfinal.contains(name)) {
				access = access & ~Opcodes.ACC_FINAL;
				this.markedModified.run();
			}
			return super.visitField(access, name, descriptor, signature, value);
		}
	}
}
