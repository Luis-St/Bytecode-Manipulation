package net.luis.asm.transformer;

import net.luis.annotation.InjectInterface;
import net.luis.asm.ASMUtils;
import net.luis.asm.base.BaseClassTransformer;
import net.luis.asm.base.visitor.BaseClassVisitor;
import net.luis.preload.ClassDataPredicate;
import net.luis.preload.PreloadContext;
import net.luis.preload.data.AnnotationData;
import net.luis.preload.data.ClassInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.util.*;

/**
 *
 * @author Luis-St
 *
 */

@SuppressWarnings("UnqualifiedFieldAccess")
public class InterfaceInjectionTransformer extends BaseClassTransformer {
	
	private static final Type INJECT_INTERFACE = Type.getType(InjectInterface.class);
	
	private final Map<String, List<String>> targets;
	
	private InterfaceInjectionTransformer(Map<String, List<String>> targets) {
		this.targets = targets;
	}
	
	public static InterfaceInjectionTransformer create(PreloadContext context) {
		Map<String, List<String>> targets = new HashMap<>();
		
		context.stream().filter(ClassDataPredicate.annotatedWith(INJECT_INTERFACE)).forEach((info, content) -> {
			for (AnnotationData data : info.annotations()) {
				if (INJECT_INTERFACE.equals(data.type())) {
					List<Type> types = data.get("targets");
					for (Type target : types) {
						targets.computeIfAbsent(target.getInternalName(), k -> new ArrayList<>()).add(info.type().getInternalName());
					}
				}
			}
		});
		System.out.println(targets);
		return new InterfaceInjectionTransformer(targets);
	}
	
	@Override
	protected ClassVisitor visit(@NotNull String className, @Nullable Class<?> clazz, @NotNull ClassReader reader, @NotNull ClassWriter writer) {
		return new BaseClassVisitor(writer) {
			
			@Override
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				if (targets.containsKey(name)) {
					Set<String> newInterfaces = interfaces == null ? new HashSet<>() : ASMUtils.newSet(interfaces);
					newInterfaces.addAll(targets.getOrDefault(name, new ArrayList<>()));
					interfaces = newInterfaces.toArray(String[]::new);
				}
				super.visit(version, access, name, signature, superName, interfaces);
			}
		};
	}
}
