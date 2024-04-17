package net.luis.asm.base;

import org.objectweb.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

/**
 *
 * @author Luis-St
 *
 */

public abstract class BaseClassTransformer implements ClassFileTransformer {
	
	@Override
	public final byte[] transform(ClassLoader loader, String name, Class<?> clazz, ProtectionDomain domain, byte[] buffer) {
		ClassReader reader = new ClassReader(buffer);
		ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
		ClassVisitor visitor = this.createVisitor(name, clazz, reader, writer);
		reader.accept(visitor, ClassReader.EXPAND_FRAMES);
		return writer.toByteArray();
	}
	
	public abstract ClassVisitor createVisitor(String name, Class<?> clazz, ClassReader reader, ClassWriter writer);
}
