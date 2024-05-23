package net.luis.agent.asm.base.visitor;

import net.luis.agent.asm.Instrumentations;
import net.luis.agent.preload.PreloadContext;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;

/**
 *
 * @author Luis-St
 *
 */

public class BaseClassVisitor extends ClassVisitor implements Instrumentations {
	
	private final Runnable markModified;
	protected final PreloadContext context;
	protected final Type type;
	
	public BaseClassVisitor(@NotNull PreloadContext context, @NotNull Type type, @NotNull Runnable markModified) {
		super(Opcodes.ASM9);
		this.context = context;
		this.type = type;
		this.markModified = markModified;
	}
	
	public BaseClassVisitor(@NotNull ClassVisitor visitor, @NotNull PreloadContext context, @NotNull Type type, @NotNull Runnable markModified) {
		super(Opcodes.ASM9, visitor);
		this.context = context;
		this.type = type;
		this.markModified = markModified;
	}
	
	protected void markModified() {
		this.markModified.run();
	}
}
