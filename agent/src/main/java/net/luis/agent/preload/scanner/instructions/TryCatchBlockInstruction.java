package net.luis.agent.preload.scanner.instructions;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

/**
 *
 * @author Luis-St
 *
 */

public record TryCatchBlockInstruction(@NotNull Label start, @NotNull Label end, @NotNull Label handler, @NotNull String type) implements ASMInstruction {
	
	@Override
	public void copyTo(@NotNull MethodVisitor visitor) {
		visitor.visitTryCatchBlock(this.start, this.end, this.handler, this.type);
	}
}
