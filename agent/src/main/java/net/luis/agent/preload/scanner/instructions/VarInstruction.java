package net.luis.agent.preload.scanner.instructions;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.MethodVisitor;

/**
 *
 * @author Luis-St
 *
 */

public record VarInstruction(int opcode, int varIndex) implements ASMInstruction {
	
	@Override
	public void copyTo(@NotNull MethodVisitor visitor) {
		visitor.visitVarInsn(this.opcode, this.varIndex);
	}
}
