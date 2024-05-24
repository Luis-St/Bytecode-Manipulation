package net.luis.agent.asm.base.visitor;

import net.luis.agent.preload.PreloadContext;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;

/**
 *
 * @author Luis-St
 *
 */

public class ContextBasedClassVisitor extends BaseClassVisitor {
	
	protected final PreloadContext context;
	protected final Type type;
	
	public ContextBasedClassVisitor(@NotNull PreloadContext context, @NotNull Type type, @NotNull Runnable markModified) {
		super(markModified);
		this.context = context;
		this.type = type;
	}
	
	public ContextBasedClassVisitor(@NotNull ClassVisitor visitor, @NotNull PreloadContext context, @NotNull Type type, @NotNull Runnable markModified) {
		super(visitor, markModified);
		this.context = context;
		this.type = type;
	}
}
