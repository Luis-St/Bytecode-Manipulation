package net.luis.agent.asm.base.visitor;

import net.luis.agent.preload.PreloadContext;
import net.luis.agent.preload.data.MethodData;
import net.luis.agent.preload.data.ParameterData;
import net.luis.agent.preload.type.TypeModifier;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

/**
 *
 * @author Luis-St
 *
 */

public class ContextBasedMethodVisitor extends BaseMethodVisitor {
	
	protected final PreloadContext context;
	protected final MethodData method;
	
	public ContextBasedMethodVisitor(@NotNull PreloadContext context, @NotNull MethodData method, @NotNull Runnable markModified) {
		super(markModified);
		this.context = context;
		this.method = method;
	}
	
	public ContextBasedMethodVisitor(@NotNull MethodVisitor visitor, @NotNull PreloadContext context, @NotNull MethodData method, @NotNull Runnable markModified) {
		super(visitor, markModified);
		this.context = context;
		this.method = method;
	}
	
	protected void visitVarInsn(int opcode, @NotNull ParameterData parameter) {
		this.mv.visitVarInsn(opcode, this.method.is(TypeModifier.STATIC) ? parameter.index() : parameter.index() + 1);
	}
}
