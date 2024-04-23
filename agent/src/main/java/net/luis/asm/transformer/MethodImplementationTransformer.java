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
	
	
	public static MethodImplementationTransformer create(@NotNull PreloadContext context) {
		Map<ClassInfo, ClassContent> targets = new HashMap<>();
		
		
		
//		context.stream().filter(ClassDataPredicate.annotatedWith(INJECT_INTERFACE)).forEach((info, content) -> {
//
//			for (AnnotationData data : info.annotations()) {
//				if (INJECT_INTERFACE.equals(data.type())) {
//					List<Type> types = data.get("targets");
//					for (Type target : types) {
//						ClassInfo targetInfo = context.getClassInfo(target);
//					}
//				}
//			}
//		});
		
		
		return new MethodImplementationTransformer();
	}
	
	@Override
	protected ClassVisitor visit(@NotNull String className, @Nullable Class<?> clazz, @NotNull ClassReader reader, @NotNull ClassWriter writer) {
		return null;
	}
}
