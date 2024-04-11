package net.luis.asm;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class AnnotationBasedClassTransformer implements ClassFileTransformer {
	
	@Override
	public byte @Nullable [] transform(ClassLoader loader, String name, Class<?> clazz, ProtectionDomain domain, byte[] buffer) throws IllegalClassFormatException {
		ClassReader reader = new ClassReader(buffer);
		ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
		ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
			@Override
			public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
				System.out.println("Annotation: " + desc);
				return super.visitAnnotation(desc, visible);
			}
		};
		reader.accept(visitor, ClassReader.EXPAND_FRAMES);
		return writer.toByteArray();
	}
}
