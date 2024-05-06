package net.luis.agent.asm.base.visitor;

import net.luis.agent.preload.PreloadContext;
import net.luis.agent.preload.data.MethodData;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

/**
 *
 * @author Luis-St
 *
 */

public abstract class ModificationMethodVisitor extends BaseMethodVisitor {
	
	private final Runnable markModified;
	protected final PreloadContext context;
	protected final Type type;
	protected final MethodData method;
	
	protected ModificationMethodVisitor(@NotNull PreloadContext context, @NotNull Type type, @NotNull MethodData method, @NotNull Runnable markModified) {
		this.context = context;
		this.type = type;
		this.method = method;
		this.markModified = markModified;
	}
	
	protected ModificationMethodVisitor(@NotNull MethodVisitor visitor, @NotNull PreloadContext context, @NotNull Type type, @NotNull MethodData method, @NotNull Runnable markModified) {
		super(visitor);
		this.context = context;
		this.type = type;
		this.method = method;
		this.markModified = markModified;
	}
	
	protected void markModified() {
		this.markModified.run();
	}
}
