package net.luis.asm.transformer.implementation;

import net.luis.preload.PreloadContext;
import net.luis.preload.data.ClassContent;
import net.luis.preload.data.MethodData;
import net.luis.preload.type.TypeAccess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.util.List;
import java.util.Map;

public class ImplementedValidationTransformer extends AbstractImplementationTransformer {
	
	public ImplementedValidationTransformer(@NotNull PreloadContext context) {
		super(context);
	}
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull String className, @Nullable Class<?> clazz, @NotNull ClassReader reader, @NotNull ClassWriter writer) {
		return new ImplementedVisitor(writer, this.context, this.lookup);
	}
	
	private static class ImplementedVisitor extends ImplementationVisitor {
		
		protected ImplementedVisitor(@NotNull ClassWriter writer, @NotNull PreloadContext context, @NotNull Map</*Target Class*/String, /*Interfaces*/List<String>> lookup) {
			super(writer, context, lookup);
		}
		
		@Override
		protected @NotNull Type getAnnotationType() {
			return IMPLEMENTED;
		}
		
		@Override
		protected void validateMethod(@NotNull Type iface, @NotNull MethodData ifaceMethod, @NotNull Type target, @NotNull ClassContent targetContent) {
			System.out.println("Validating Implemented - " + ifaceMethod.name() + " - " + iface.getInternalName());
			this.baseValidation("@Implemented", iface, ifaceMethod);
			if (!targetContent.hasMethod(ifaceMethod.name(), ifaceMethod.type())) {
				throw createReport("Method annotated with @Implemented must be implemented in target class", iface, ifaceMethod.getMethodSignature())
					.addDetailBefore("Interface", "Target Class", target).exception();
			}
			MethodData targetMethod = targetContent.getMethod(ifaceMethod.name(), ifaceMethod.type());
			if (targetMethod.access() != TypeAccess.PUBLIC) {
				throw createReport("Method annotated with @Implemented must be public in target class", iface, ifaceMethod.getMethodSignature())
					.addDetailBefore("Interface", "Target Class", target)
					.addDetailBefore("Interface", "Target Method", targetMethod.getMethodSignature()).exception();
			}
		}
	}
}
