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
	
	public BaseClassVisitor(@NotNull Runnable markModified) {
		super(Opcodes.ASM9);
		this.markModified = markModified;
	}
	
	public BaseClassVisitor(@NotNull ClassVisitor visitor, @NotNull Runnable markModified) {
		super(Opcodes.ASM9, visitor);
		this.markModified = markModified;
	}
	
	protected void markModified() {
		this.markModified.run();
	}
}
