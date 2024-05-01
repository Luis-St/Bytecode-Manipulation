package net.luis.asm.transformer.implementation;

import net.luis.asm.report.CrashReport;
import net.luis.preload.PreloadContext;
import net.luis.preload.data.*;
import net.luis.preload.type.TypeAccess;
import net.luis.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.util.*;

public class AccessorImplementationTransformer extends AbstractImplementationTransformer {
	
	public AccessorImplementationTransformer(@NotNull PreloadContext context) {
		super(context);
	}
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull String className, @Nullable Class<?> clazz, @NotNull ClassReader reader, @NotNull ClassWriter writer) {
		return new AccessorVisitor(writer, this.context, this.lookup);
	}
	
	private static class AccessorVisitor extends ImplementationVisitor {
		
		private static final String REPORT_CATEGORY = "Accessor Implementation Error";
		private final Map</*Target Field*/String, Map.Entry<FieldData, MethodData>> accessors = new HashMap<>();
		
		protected AccessorVisitor(@NotNull ClassWriter writer, @NotNull PreloadContext context, @NotNull Map</*Target Class*/String, /*Interfaces*/List<String>> lookup) {
			super(writer, context, lookup);
		}
		
		@Override
		protected @NotNull Type getAnnotationType() {
			return ACCESSOR;
		}
		
		private @NotNull String getAccessorName(@NotNull MethodData ifaceMethod) {
			AnnotationData annotation = ifaceMethod.getAnnotation(this.getAnnotationType());
			if (annotation.has("target", String.class)) {
				return annotation.get("target");
			}
			String methodName = ifaceMethod.name();
			if (methodName.startsWith("get")) {
				return Utils.uncapitalize(methodName.substring(3));
			} else if (methodName.startsWith("access")) {
				return Utils.uncapitalize(methodName.substring(6));
			}
			return methodName;
		}
		
		@Override
		protected void validateMethod(@NotNull Type iface, @NotNull MethodData ifaceMethod, @NotNull Type target, @NotNull ClassContent targetContent) {
			System.out.println("Validating Accessor - " + ifaceMethod.name() + " - " + iface.getInternalName());
			this.baseValidation("@Accessor", iface, ifaceMethod);
			if (ifaceMethod.getReturnType() == Type.VOID_TYPE) {
				throw CrashReport.create("Accessor method has void return type", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Accessor", ifaceMethod.getMethodSignature()).exception();
			}
			if (ifaceMethod.getParameterCount() > 0) {
				throw CrashReport.create("Accessor method has parameters", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Accessor", ifaceMethod.getMethodSignature()).exception();
			}
			String accessorTarget = this.getAccessorName(ifaceMethod);
			if (!targetContent.hasField(accessorTarget)) {
				throw CrashReport.create("Accessor target field not found", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Accessor", ifaceMethod.getMethodSignature())
					.addDetail("Expected Accessor Target", accessorTarget).exception();
			}
			FieldData targetField = targetContent.getField(accessorTarget);
			if (targetField.access() == TypeAccess.PUBLIC) {
				throw CrashReport.create("Accessor target field is public", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Accessor", ifaceMethod.getMethodSignature())
					.addDetail("Accessor Target", accessorTarget).exception();
			}
			if (targetField.type() != ifaceMethod.getReturnType()) {
				throw CrashReport.create("Accessor target field type mismatch", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Accessor", ifaceMethod.getMethodSignature())
					.addDetail("Accessor Target", accessorTarget).addDetail("Expected Type", targetField.type()).addDetail("Actual Type", ifaceMethod.getReturnType()).exception();
			}
			this.generateAccessor(ifaceMethod, target, targetField);
		}
		
		private void generateAccessor(@NotNull MethodData ifaceMethod, @NotNull Type target, @NotNull FieldData targetField) {
			/*MethodVisitor method = super.visitMethod(Opcodes.ACC_PUBLIC, ifaceMethod.name(), ifaceMethod.type().getDescriptor(), null, null);
			method.visitCode();
			method.visitVarInsn(Opcodes.ALOAD, 0);
			method.visitFieldInsn(Opcodes.GETFIELD, target.getDescriptor(), targetField.name(), targetField.type().getDescriptor());
			method.visitInsn(Opcodes.ARETURN);
			method.visitMaxs(1, 1);
			method.visitEnd();*/
		}
	}
}
