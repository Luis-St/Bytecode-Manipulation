package net.luis.asm.base;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
	public final byte @Nullable [] transform(@NotNull ClassLoader loader, @NotNull String name, @NotNull Class<?> clazz, @NotNull ProtectionDomain domain, byte @NotNull [] buffer) {
		ClassReader reader = new ClassReader(buffer);
		ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
		ClassVisitor visitor = this.createVisitor(name, clazz, reader, writer);
		reader.accept(visitor, ClassReader.EXPAND_FRAMES);
		return writer.toByteArray();
	}
	
	public abstract ClassVisitor createVisitor(@NotNull String name, @NotNull Class<?> clazz, @NotNull ClassReader reader, @NotNull ClassWriter writer);
}