package net.luis.asm.transformer;

import net.luis.preload.PreloadContext;
import net.luis.preload.data.AnnotationData;
import org.objectweb.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.*;

/**
 *
 * @author Luis-St
 *
 */

public class InterfaceInjectionTransformer implements ClassFileTransformer {
	
	private final Map<String, List<String>> targets;
	
	public InterfaceInjectionTransformer(Map<String, List<String>> targets) {
		this.targets = targets;
	}
	
	public static InterfaceInjectionTransformer create(PreloadContext context) {
		Map<String, List<String>> targets = new HashMap<>();
		for (Map.Entry<String, List<AnnotationData>> entry : context.getClassAnnotations().entrySet()) {
			for (AnnotationData data : entry.getValue()) {
				if ("Lnet/luis/annotation/InterfaceInjection;".equals(data.descriptor())) {
					List<Type> types = data.get("targets");
					for (Type target : types) {
						targets.computeIfAbsent(target.getClassName().replace(".", "/"), k -> new ArrayList<>()).add(entry.getKey());
					}
				}
			}
		}
		targets.forEach((k, v) -> System.out.println(k + " -> " + v));
		return new InterfaceInjectionTransformer(targets);
	}
	
	@Override
	public byte[] transform(ClassLoader loader, String name, Class<?> clazz, ProtectionDomain domain, byte[] buffer) throws IllegalClassFormatException {
		ClassReader reader = new ClassReader(buffer);
		ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
		ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
			
			@Override
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				if (targets.containsKey(name)) {
					List<String> list = new ArrayList<>(Arrays.asList(interfaces));
					for (String iface : targets.get(name)) {
						iface = iface.replace(".", "/");
						list.add(iface);
					}
					interfaces = list.toArray(String[]::new);
				}
				super.visit(version, access, name, signature, superName, interfaces);
			}
		};
		reader.accept(visitor, ClassReader.EXPAND_FRAMES);
		return writer.toByteArray();
	}
}
