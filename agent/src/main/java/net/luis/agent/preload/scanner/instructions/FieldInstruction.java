package net.luis.agent.preload.scanner.instructions;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.MethodVisitor;

/**
 *
 * @author Luis-St
 *
 */

public record FieldInstruction(int opcode, @NotNull String owner, @NotNull String name, @NotNull String descriptor) implements ASMInstruction {
	
	@Override
	public void copyTo(@NotNull MethodVisitor visitor) {
		visitor.visitFieldInsn(this.opcode, this.owner, this.name, this.descriptor);
	}
}
