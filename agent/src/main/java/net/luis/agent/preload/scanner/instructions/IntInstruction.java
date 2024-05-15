package net.luis.agent.preload.scanner.instructions;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.MethodVisitor;

/**
 *
 * @author Luis-St
 *
 */

public record IntInstruction(int opcode, int operand) implements ASMInstruction {
	
	@Override
	public void copyTo(@NotNull MethodVisitor visitor) {
		visitor.visitIntInsn(this.opcode, this.operand);
	}
}
