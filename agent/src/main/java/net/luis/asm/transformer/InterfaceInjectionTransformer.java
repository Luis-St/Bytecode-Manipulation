package net.luis.asm.transformer;

import net.luis.annotation.InjectInterface;
import net.luis.asm.ASMUtils;
import net.luis.asm.base.BaseClassTransformer;
import net.luis.asm.base.visitor.BaseClassVisitor;
import net.luis.asm.exception.InterfaceInjectionError;
import net.luis.preload.PreloadContext;
import net.luis.preload.type.ClassType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.util.*;

/**
 *
 * @author Luis-St
 *
 */

public class InterfaceInjectionTransformer extends BaseClassTransformer {
	
	private static final Type INJECT_INTERFACE = Type.getType(InjectInterface.class);
	
	private final Map</*Target Class*/String, /*Interfaces*/List<String>> targets;
	
	public InterfaceInjectionTransformer(@NotNull PreloadContext context) {
		this.targets = ASMUtils.createTargetsLookup(context, INJECT_INTERFACE);
	}
	
	@Override
	@SuppressWarnings("UnqualifiedFieldAccess")
	protected ClassVisitor visit(@NotNull String className, @Nullable Class<?> clazz, @NotNull ClassReader reader, @NotNull ClassWriter writer) {
		return new BaseClassVisitor(writer) {
			
			@Override
			public void visit(int version, int access, @NotNull String name, @Nullable String signature, @Nullable String superClass, String @Nullable [] interfaces) {
				if (targets.containsKey(name)) {
					ClassType type = ClassType.fromAccess(access);
					if (type == ClassType.ANNOTATION) {
						throw new InterfaceInjectionError("Cannot inject interfaces into an annotation class");
					} else if (type == ClassType.INTERFACE) {
						throw new InterfaceInjectionError("Cannot inject interfaces into an interface class");
					}
					Set<String> newInterfaces = interfaces == null ? new HashSet<>() : ASMUtils.newSet(interfaces);
					newInterfaces.addAll(targets.getOrDefault(name, new ArrayList<>()));
					interfaces = newInterfaces.toArray(String[]::new);
				}
				super.visit(version, access, name, signature, superClass, interfaces);
			}
		};
	}
}
