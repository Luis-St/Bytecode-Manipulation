package net.luis.agent.asm.base.visitor;

import net.luis.agent.preload.PreloadContext;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;

/**
 *
 * @author Luis-St
 *
 */

public class ContextBasedClassVisitor extends ClassVisitor {
	
	private final Runnable markModified;
	protected final PreloadContext context;
	protected final Type type;
	
	public ContextBasedClassVisitor(@NotNull PreloadContext context, @NotNull Type type, @NotNull Runnable markModified) {
		super(Opcodes.ASM9);
		this.context = context;
		this.type = type;
		this.markModified = markModified;
	}
	
	public ContextBasedClassVisitor(@NotNull ClassVisitor visitor, @NotNull PreloadContext context, @NotNull Type type, @NotNull Runnable markModified) {
		super(Opcodes.ASM9, visitor);
		this.context = context;
		this.type = type;
		this.markModified = markModified;
	}
	
	protected void markModified() {
		this.markModified.run();
	}
}
