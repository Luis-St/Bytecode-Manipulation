package net.luis.agent.asm.base.visitor;

import net.luis.agent.asm.Instrumentations;
import net.luis.agent.preload.PreloadContext;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

/**
 *
 * @author Luis-St
 *
 */

public abstract class BaseClassVisitor extends ClassVisitor implements Instrumentations {
	
	private final Runnable markModified;
	protected final PreloadContext context;
	
	protected BaseClassVisitor(@NotNull PreloadContext context, @NotNull Runnable markModified) {
		super(Opcodes.ASM9);
		this.context = context;
		this.markModified = markModified;
	}
	
	protected BaseClassVisitor(@NotNull ClassVisitor visitor, @NotNull PreloadContext context, @NotNull Runnable markModified) {
		super(Opcodes.ASM9, visitor);
		this.context = context;
		this.markModified = markModified;
	}
	
	protected void markModified() {
		this.markModified.run();
	}
}
