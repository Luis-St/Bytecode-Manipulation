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

public class AccessorImplementationTransformer extends BaseClassTransformer {
	
	public AccessorImplementationTransformer(@NotNull PreloadContext context) {
		super(context);
	}
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @Nullable Class<?> clazz, @NotNull ClassReader reader, @NotNull ClassWriter writer) {
		return new AccessorImplementationVisitor(writer, this.context, type, () -> this.modified = true, ASMUtils.createTargetsLookup(this.context, INJECT_INTERFACE));
	}
	
	private static class AccessorImplementationVisitor extends BaseClassVisitor {
		
		private static final String REPORT_CATEGORY = "Accessor Implementation Error";
		
		private final Map</*Target Class*/String, /*Interfaces*/List<String>> lookup;
		
		private AccessorImplementationVisitor(@NotNull ClassWriter writer, @NotNull PreloadContext context, @NotNull Type type, @NotNull Runnable markModified, @NotNull Map</*Target Class*/String, /*Interfaces*/List<String>> lookup) {
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
						if (method.isAnnotatedWith(ACCESSOR)) {
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
		
		private @NotNull String getAccessorName(@NotNull MethodData ifaceMethod) {
			AnnotationData annotation = ifaceMethod.getAnnotation(ACCESSOR);
			String target = annotation.get("target");
			if (target != null) {
				return target;
			}
			String methodName = ifaceMethod.name();
			if (methodName.startsWith("get")) {
				return Utils.uncapitalize(methodName.substring(3));
			} else if (methodName.startsWith("access")) {
				return Utils.uncapitalize(methodName.substring(6));
			}
			return methodName;
		}
		
		private void validateMethod(@NotNull Type iface, @NotNull MethodData ifaceMethod, @NotNull Type target, @NotNull ClassContent targetContent) {
			String signature = ifaceMethod.getMethodSignature();
			//region Base validation
			if (ifaceMethod.access() != TypeAccess.PUBLIC) {
				throw CrashReport.create("Method annotated with @Accessor must be public", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Accessor", signature).exception();
			}
			if (ifaceMethod.is(TypeModifier.STATIC)) {
				throw CrashReport.create("Method annotated with @Accessor must not be static", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Accessor", signature).exception();
			}
			if (!ifaceMethod.is(TypeModifier.ABSTRACT)) {
				throw CrashReport.create("Method annotated with @Accessor must not be default implemented", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Accessor", signature).exception();
			}
			//endregion
			if (ifaceMethod.returns(Type.VOID_TYPE)) {
				throw CrashReport.create("Method annotated with @Accessor has void return type", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Accessor", signature).exception();
			}
			if (ifaceMethod.getParameterCount() > 0) {
				throw CrashReport.create("Method annotated with @Accessor must not have parameters", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Accessor", signature).exception();
			}
			if (ifaceMethod.getExceptionCount() > 0) {
				throw CrashReport.create("Method annotated with @Accessor must not throw exceptions", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Accessor", signature)
					.addDetail("Exceptions", ifaceMethod.exceptions()).exception();
			}
			MethodData existingMethod = targetContent.getMethod(ifaceMethod.name(), ifaceMethod.type());
			if (existingMethod != null) {
				throw CrashReport.create("Target class of accessor already has method with same signature", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Accessor", signature)
					.addDetail("Existing Method", existingMethod.getMethodSignature()).exception();
			}
			String accessorTarget = this.getAccessorName(ifaceMethod);
			FieldData targetField = targetContent.getField(accessorTarget);
			if (targetField == null) {
				throw CrashReport.create("Target field for accessor was not found in target class", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Accessor", signature)
					.addDetail("Expected Accessor Target", accessorTarget).exception();
			}
			if (targetField.access() == TypeAccess.PUBLIC) {
				throw CrashReport.create("Target field for accessor is public, no accessor required", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Accessor", signature)
					.addDetail("Accessor Target", accessorTarget).exception();
			}
			if (!ifaceMethod.returns(targetField.type())) {
				throw CrashReport.create("Accessor return type does not match target field type", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Accessor", signature)
					.addDetail("Accessor Target", accessorTarget).addDetail("Expected Type", targetField.type()).addDetail("Actual Type", ifaceMethod.getReturnType()).exception();
			}
			String fieldSignature = targetField.signature();
			if (fieldSignature != null && !fieldSignature.isBlank()) {
				String accessorSignature = ASMUtils.getReturnTypeSignature(ifaceMethod);
				if (!Objects.equals(fieldSignature, accessorSignature)) {
					throw CrashReport.create("Accessor signature does not match target field signature", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Accessor", signature)
						.addDetail("Accessor Target", accessorTarget).addDetail("Expected Signature", fieldSignature).addDetail("Actual Signature", accessorSignature).exception();
				}
			}
			this.generateAccessor(ifaceMethod, target, targetField);
		}
		
		@SuppressWarnings("DuplicatedCode")
		private void generateAccessor(@NotNull MethodData ifaceMethod, @NotNull Type target, @NotNull FieldData targetField) {
			MethodVisitor visitor = super.visitMethod(Opcodes.ACC_PUBLIC, ifaceMethod.name(), ifaceMethod.type().getDescriptor(), ifaceMethod.signature(), null);
			this.instrumentMethodAnnotations(visitor, ifaceMethod, true);
			this.instrumentParameterAnnotations(visitor, ifaceMethod);
			visitor.visitCode();
			visitor.visitVarInsn(Opcodes.ALOAD, 0);
			visitor.visitFieldInsn(Opcodes.GETFIELD, target.getInternalName(), targetField.name(), targetField.type().getDescriptor());
			visitor.visitInsn(Opcodes.ARETURN);
			visitor.visitLocalVariable("this", target.getDescriptor(), targetField.signature(), new Label(), new Label(), 0);
			visitor.visitMaxs(0, 0);
			visitor.visitEnd();
			this.updateClass(ifaceMethod, target);
			this.markModified();
		}
		
		private void updateClass(@NotNull MethodData ifaceMethod, @NotNull Type target) {
			ClassContent content = this.context.getClassContent(target);
			MethodData method = new MethodData(ifaceMethod.name(), ifaceMethod.type(), ifaceMethod.signature(), TypeAccess.PUBLIC, MethodType.METHOD, EnumSet.noneOf(TypeModifier.class), ifaceMethod.annotations(), new ArrayList<>(), new ArrayList<>(), new Mutable<>());
			content.methods().add(method);
		}
	}
}
