package net.luis.agent.preload.scanner.instructions;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.MethodVisitor;

/**
 *
 * @author Luis-St
 *
 */

public record MethodInstruction(int opcode, @NotNull String owner, @NotNull String name, @NotNull String descriptor, boolean isInterface) implements ASMInstruction {
	
	@Override
	public void copyTo(@NotNull MethodVisitor visitor) {
		visitor.visitMethodInsn(this.opcode, this.owner, this.name, this.descriptor, this.isInterface);
	}
}
