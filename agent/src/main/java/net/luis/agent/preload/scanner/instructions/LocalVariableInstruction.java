package net.luis.agent.preload.scanner.instructions;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

/**
 *
 * @author Luis-St
 *
 */

public record LocalVariableInstruction(@NotNull String name, @NotNull String descriptor, @NotNull String signature, @NotNull Label start, @NotNull Label end, int index) implements ASMInstruction {
	
	@Override
	public void copyTo(@NotNull MethodVisitor visitor) {
		visitor.visitLocalVariable(this.name, this.descriptor, this.signature, this.start, this.end, this.index);
	}
}
