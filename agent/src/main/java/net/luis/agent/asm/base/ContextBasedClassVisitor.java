package net.luis.agent.asm.base;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;

/**
 *
 * @author Luis-St
 *
 */

public class ContextBasedClassVisitor extends ClassVisitor {
	
	private final Runnable markModified;
	protected final Type type;
	
	public ContextBasedClassVisitor(@NotNull Type type, @NotNull Runnable markModified) {
		super(Opcodes.ASM9);
		this.type = type;
		this.markModified = markModified;
	}
	
	public ContextBasedClassVisitor(@NotNull ClassVisitor visitor, @NotNull Type type, @NotNull Runnable markModified) {
		super(Opcodes.ASM9, visitor);
		this.type = type;
		this.markModified = markModified;
	}
	
	protected void markModified() {
		this.markModified.run();
	}
}
