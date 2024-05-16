package net.luis.agent.asm.base.visitor;

import net.luis.agent.asm.Instrumentations;
import net.luis.agent.preload.PreloadContext;
import net.luis.agent.preload.data.ClassContent;
import net.luis.agent.preload.data.MethodData;
import net.luis.agent.preload.type.TypeModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.util.Objects;

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
