package net.luis.asm;

import com.google.common.collect.Lists;
import net.luis.annotation.InterfaceInjection;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.List;

/**
 *
 * @author Luis-St
 *
 */

public class InterfaceInjectionTransformer implements ClassFileTransformer {
	
	private final String iface;
	private final List<String> targets;
	
	public InterfaceInjectionTransformer(String iface, List<String> targets) {
		this.iface = iface;
		this.targets = targets;
	}
	
	@Override
	public byte @Nullable [] transform(ClassLoader loader, String name, Class<?> clazz, ProtectionDomain domain, byte[] buffer) throws IllegalClassFormatException {
		System.out.println("Transforming: " + name);
		if (clazz != null) {
			System.out.println("Skipping: " + clazz);
		}
		ClassReader reader = new ClassReader(buffer);
		ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
		ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
			
			@Override
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				
				if (InterfaceInjectionTransformer.this.targets.contains(name)) {
					System.out.println("Target class: " + name);
					List<String> list = Lists.newArrayList(interfaces);
					if (!list.contains(InterfaceInjectionTransformer.this.iface)) {
						list.add(InterfaceInjectionTransformer.this.iface);
						System.out.println("Added interface: " + InterfaceInjectionTransformer.this.iface);
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
