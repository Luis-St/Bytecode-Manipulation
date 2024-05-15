package net.luis.agent.preload.scanner.instructions;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

/**
 *
 * @author Luis-St
 *
 */

public record JumpInstruction(int opcode, @NotNull Label label) implements ASMInstruction {
	
	@Override
	public void copyTo(@NotNull MethodVisitor visitor) {
		visitor.visitJumpInsn(this.opcode, this.label);
	}
}
