package net.luis.asm.transformer;

import net.luis.asm.ASMUtils;
import net.luis.asm.base.BaseClassTransformer;
import net.luis.asm.base.visitor.BaseClassVisitor;
import net.luis.preload.PreloadContext;
import net.luis.preload.data.AnnotationData;
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
	
	private final Map<String, List<String>> targets;
	
	public InterfaceInjectionTransformer(Map<String, List<String>> targets) {
		this.targets = targets;
	}
	
	public static InterfaceInjectionTransformer create(PreloadContext context) {
		Map<String, List<String>> targets = new HashMap<>();
		/*for (Map.Entry<String, List<AnnotationData>> entry : context.getClassAnnotations().entrySet()) {
			for (AnnotationData data : entry.getValue()) {
				if ("Lnet/luis/annotation/InjectInterface;".equals(data.type().getDescriptor())) {
					List<Type> types = data.get("targets");
					for (Type target : types) {
						targets.computeIfAbsent(target.getClassName().replace(".", "/"), k -> new ArrayList<>()).add(entry.getKey());
					}
				}
			}
		}*/
		return new InterfaceInjectionTransformer(targets);
	}
	
	@Override
	protected ClassVisitor visit(@NotNull String className, @Nullable Class<?> clazz, @NotNull ClassReader reader, @NotNull ClassWriter writer) {
		return new BaseClassVisitor(writer) {
			
			@Override
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				if (targets.containsKey(name)) {
					Set<String> list = interfaces == null ? new HashSet<>() : ASMUtils.newSet(interfaces);
					for (String iface : targets.get(name)) {
						iface = iface.replace(".", "/");
						list.add(iface);
					}
					interfaces = list.toArray(String[]::new);
				}
				super.visit(version, access, name, signature, superName, interfaces);
			}
		};
	}
}
