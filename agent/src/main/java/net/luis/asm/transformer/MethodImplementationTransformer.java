package net.luis.asm.transformer;

import net.luis.annotation.InjectInterface;
import net.luis.asm.base.BaseClassTransformer;
import net.luis.preload.ClassDataPredicate;
import net.luis.preload.PreloadContext;
import net.luis.preload.data.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.util.*;

/**
 *
 * @author Luis-St
 *
 */

public class MethodImplementationTransformer extends BaseClassTransformer {
	
	private static final Type INJECT_INTERFACE = Type.getType(InjectInterface.class);
	
	private final PreloadContext context;
	private final Map</*Target Class*/String, /*Interfaces*/List<String>> targets;
	
	public MethodImplementationTransformer(@NotNull PreloadContext context) {
		this.context = context;
		this.targets = ASMUtils.createTargetsLookup(context, INJECT_INTERFACE);
	}
	
	@Override
	protected ClassVisitor visit(@NotNull String className, @Nullable Class<?> clazz, @NotNull ClassReader reader, @NotNull ClassWriter writer) {
		return null;
	}
}
