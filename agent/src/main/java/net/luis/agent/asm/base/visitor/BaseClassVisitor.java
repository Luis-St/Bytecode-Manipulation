package net.luis.agent.asm.base.visitor;

import net.luis.agent.preload.PreloadContext;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

/**
 *
 * @author Luis-St
 *
 */

public abstract class BaseClassVisitor extends ClassVisitor {
	
	private final Runnable markedModified;
	protected final PreloadContext context;
	
	protected BaseClassVisitor(@NotNull PreloadContext context, @NotNull Runnable markedModified) {
		super(Opcodes.ASM9);
		this.context = context;
		this.markedModified = markedModified;
	}
	
	protected BaseClassVisitor(@NotNull ClassVisitor visitor, @NotNull PreloadContext context, @NotNull Runnable markedModified) {
		super(Opcodes.ASM9, visitor);
		this.context = context;
		this.markedModified = markedModified;
	}
	
	protected void markModified() {
		this.markedModified.run();
	}
}
