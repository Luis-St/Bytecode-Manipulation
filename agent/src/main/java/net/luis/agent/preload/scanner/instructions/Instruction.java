package net.luis.agent.preload.scanner.instructions;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.MethodVisitor;

/**
 *
 * @author Luis-St
 *
 */

public record Instruction(int opcode) implements ASMInstruction {
	
	@Override
	public void copyTo(@NotNull MethodVisitor visitor) {
		visitor.visitInsn(this.opcode);
	}
}
