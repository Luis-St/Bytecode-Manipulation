package net.luis.agent.preload.scanner.instructions;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.MethodVisitor;

/**
 *
 * @author Luis-St
 *
 */

public record MultiNewArrayInstruction(@NotNull String descriptor, int numDimensions) implements ASMInstruction {
	
	@Override
	public void copyTo(@NotNull MethodVisitor visitor) {
		visitor.visitMultiANewArrayInsn(this.descriptor, this.numDimensions);
	}
}
