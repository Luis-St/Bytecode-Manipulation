package net.luis.asm;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.lang.instrument.*;
import java.security.ProtectionDomain;
import java.util.List;

/**
 *
 * @author Luis-St
 *
 */

public class InterfaceInjectionScanner implements ClassFileTransformer {
	
	private final Instrumentation inst;
	
	public InterfaceInjectionScanner(Instrumentation inst) {
		this.inst = inst;
	}
	
	@Override
	public byte @Nullable [] transform(ClassLoader loader, String name, Class<?> clazz, ProtectionDomain domain, byte[] buffer) throws IllegalClassFormatException {
		if (clazz != null) {
			System.out.println("Skipping: " + clazz);
			return null;
		}
		ClassReader reader = new ClassReader(buffer);
		ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
		ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
			
			final List<String> targets = Lists.newArrayList();
			
			@Override
			public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
				if ("Lnet/luis/annotation/InterfaceInjection;".equals(descriptor)) {
					System.out.println("Annotation descriptor: " + name);
					return new AnnotationVisitor(Opcodes.ASM9, super.visitAnnotation(descriptor, visible)) {
						
						@Override
						public AnnotationVisitor visitArray(String name) {
							System.out.println("Array name: " + name);
							return new AnnotationVisitor(Opcodes.ASM9, super.visitArray(name)) {
								
								@Override
								public void visit(String name, Object value) {
									System.out.println("Array value: " + value);
									targets.add(value.toString());
									super.visit(name, value);
								}
							};
						}
					};
				}
				return super.visitAnnotation(descriptor, visible);
			}
			
			@Override
			public void visitEnd() {
				super.visitEnd();
				for (String target : this.targets) {
					inst.addTransformer(new InterfaceInjectionTransformer(name, Lists.newArrayList(target)), true);
					String name = target.replace("/", ".").substring(1, target.length() - 1);
					try {
						System.out.println("Retransforming class: " + name);
						inst.retransformClasses(Class.forName(name));
					} catch (Exception e) {
						System.out.println("Failed to retransform class: " + name);
						e.printStackTrace();
					}
				}
			}
		};
		reader.accept(visitor, ClassReader.EXPAND_FRAMES);
		return writer.toByteArray();
	}
}
