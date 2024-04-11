package net.luis.asm.transformer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

/**
 *
 * @author Luis-St
 *
 */

public abstract class ConditionBasedClassTransformer<T> implements ClassFileTransformer {
	
	private final ClassFileTransformer transformer;
	
	public ConditionBasedClassTransformer(ClassFileTransformer transformer) {
		this.transformer = transformer;
	}
	
	@Override
	public final byte @Nullable [] transform(@NotNull ClassLoader loader, @NotNull String name, @NotNull Class<?> clazz, @NotNull ProtectionDomain domain, byte @NotNull [] buffer) {
		
		
		
		return null;
	}
	
	public abstract byte @Nullable [] transform(@NotNull String name, @NotNull Class<?> clazz, @NotNull T value, byte @NotNull [] buffer);
}
