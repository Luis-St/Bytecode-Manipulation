package net.luis.agent.asm.base.visitor;

import net.luis.agent.asm.Instrumentations;
import net.luis.agent.preload.PreloadContext;
import net.luis.agent.preload.data.MethodData;
import net.luis.agent.preload.data.ParameterData;
import net.luis.agent.preload.type.TypeModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;

/**
 *
 * @author Luis-St
 *
 */

public class ContextBasedMethodVisitor extends BaseMethodVisitor {
	
	protected final PreloadContext context;
	protected final Type type;
	protected final MethodData method;
	
	public ContextBasedMethodVisitor(@NotNull PreloadContext context, @NotNull Type type, @NotNull MethodData method, @NotNull Runnable markModified) {
		super(markModified);
		this.context = context;
		this.type = type;
		this.method = method;
	}
	
	public ContextBasedMethodVisitor(@NotNull MethodVisitor visitor, @NotNull PreloadContext context, @NotNull Type type, @NotNull MethodData method, @NotNull Runnable markModified) {
		super(visitor, markModified);
		this.context = context;
		this.type = type;
		this.method = method;
	}
	
	protected void visitVarInsn(int opcode, @NotNull ParameterData parameter) {
		this.mv.visitVarInsn(opcode, this.method.is(TypeModifier.STATIC) ? parameter.index() : parameter.index() + 1);
	}
}
