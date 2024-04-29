package net.luis.asm.transformer.implementation;

import net.luis.preload.PreloadContext;
import net.luis.preload.data.ClassContent;
import net.luis.preload.data.MethodData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.util.*;

public class AssignorImplementationTransformer extends AbstractImplementationTransformer {
	
	public AssignorImplementationTransformer(@NotNull PreloadContext context) {
		super(context);
	}
	
	@Override
	protected @NotNull ImplementationVisitor visit(@NotNull String className, @Nullable Class<?> clazz, @NotNull ClassReader reader, @NotNull ClassWriter writer) {
		return new AssignorVisitor(writer, this.context, this.lookup);
	}
	
	private static class AssignorVisitor extends ImplementationVisitor {
		
		private final Map</*Target Field*/String, /*Assigner Name*/String> assigners = new HashMap<>();
		
		protected AssignorVisitor(@NotNull ClassWriter writer, @NotNull PreloadContext context, @NotNull Map</*Target Class*/String, /*Interfaces*/List<String>> lookup) {
			super(writer, context, lookup);
		}
		
		@Override
		protected @NotNull Type getAnnotationType() {
			return ASSIGNOR;
		}
		
		@Override
		protected void validateMethod(@NotNull Type iface, @NotNull MethodData ifaceMethod, @NotNull Type target, @NotNull ClassContent targetContent) {
			this.baseValidation("@Assignor", iface, ifaceMethod);
		}
	}
}
