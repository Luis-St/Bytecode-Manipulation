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

public class AssignorImplementationTransformer extends BaseClassTransformer {
	
	public AssignorImplementationTransformer(@NotNull PreloadContext context) {
		super(context);
	}
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @Nullable Class<?> clazz, @NotNull ClassReader reader, @NotNull ClassWriter writer) {
		return new AssignorImplementationVisitor(writer, this.context, ASMUtils.createTargetsLookup(this.context, INJECT_INTERFACE), () -> this.modified = true);
	}
	
	private static class AssignorImplementationVisitor extends BaseClassVisitor {
		
		private static final String REPORT_CATEGORY = "Assignor Implementation Error";
		
		private final PreloadContext context;
		private final Map</*Target Class*/String, /*Interfaces*/List<String>> lookup;
		private final Runnable markModified;
		private final List<String> unfinal = new ArrayList<>();
		
		protected AssignorImplementationVisitor(@NotNull ClassWriter writer, @NotNull PreloadContext context, @NotNull Map</*Target Class*/String, /*Interfaces*/List<String>> lookup, Runnable markModified) {
			super(writer);
			this.context = context;
			this.lookup = lookup;
			this.markModified = markModified;
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
						} else if (method.is(TypeAccess.PUBLIC, TypeModifier.ABSTRACT)) {
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
			MethodData existingMethod = targetContent.getMethod(ifaceMethod.name(), ifaceMethod.type());
			if (existingMethod != null) {
				throw CrashReport.create("Target class of assignor already has method with same signature", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Assignor", signature)
					.addDetail("Existing Method", existingMethod.getMethodSignature()).exception();
			}
			String accessorTarget = this.getAssignorName(ifaceMethod);
			FieldData targetField = targetContent.getField(accessorTarget);
			if (targetField == null) {
				throw CrashReport.create("Target field for assignor was not found in target class", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Assignor", signature)
					.addDetail("Expected Accessor Target", accessorTarget).exception();
			}
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
			method.visitLocalVariable("arg0", targetField.type().getDescriptor(), null, new Label(), new Label(), 1);
			method.visitMaxs(2, 2);
			method.visitEnd();
			this.updateClass(ifaceMethod, target, targetField);
			this.markModified.run();
		}
		
		@SuppressWarnings("DuplicatedCode")
		private void updateClass(@NotNull MethodData ifaceMethod, @NotNull Type target, @NotNull FieldData targetField) {
			ClassContent content = this.context.getClassContent(target);
			Map<Type, AnnotationData> annotations = new HashMap<>(ifaceMethod.annotations());
			annotations.remove(ASSIGNOR);
			annotations.put(GENERATED, new AnnotationData(GENERATED, new HashMap<>()));
			MethodData method = new MethodData(ifaceMethod.name(), ifaceMethod.type(), ifaceMethod.signature(), TypeAccess.PUBLIC, MethodType.METHOD, EnumSet.noneOf(TypeModifier.class), annotations, ifaceMethod.parameters(), new ArrayList<>(), new Mutable<>());
			content.methods().add(method);
			targetField.modifiers().remove(TypeModifier.FINAL);
		}
		
		@Override
		public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
			if (this.unfinal.contains(name)) {
				access = access & ~Opcodes.ACC_FINAL;
				this.markModified.run();
			}
			return super.visitField(access, name, descriptor, signature, value);
		}
	}
}
