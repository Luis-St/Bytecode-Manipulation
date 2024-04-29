package net.luis.asm.transformer.implementation;

import net.luis.preload.PreloadContext;
import net.luis.preload.data.ClassContent;
import net.luis.preload.data.MethodData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.util.*;

public class AccessorImplementationTransformer extends AbstractImplementationTransformer {
	
	public AccessorImplementationTransformer(@NotNull PreloadContext context) {
		super(context);
	}
	
	@Override
	protected @NotNull ImplementationVisitor visit(@NotNull String className, @Nullable Class<?> clazz, @NotNull ClassReader reader, @NotNull ClassWriter writer) {
		return new AccessorVisitor(writer, this.context, this.lookup);
	}
	
	private static class AccessorVisitor extends ImplementationVisitor {
		
		private final Map</*Target Field*/String, /*Accessor Name*/String> accessors = new HashMap<>();
		
		protected AccessorVisitor(@NotNull ClassWriter writer, @NotNull PreloadContext context, @NotNull Map</*Target Class*/String, /*Interfaces*/List<String>> lookup) {
			super(writer, context, lookup);
		}
		
		@Override
		protected @NotNull Type getAnnotationType() {
			return ACCESSOR;
		}
		
		@Override
		protected void validateMethod(@NotNull Type iface, @NotNull MethodData ifaceMethod, @NotNull Type target, @NotNull ClassContent targetContent) {
			this.baseValidation("@Accessor", iface, ifaceMethod);
		}
	}
}
