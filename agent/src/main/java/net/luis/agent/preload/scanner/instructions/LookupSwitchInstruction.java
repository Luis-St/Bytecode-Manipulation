package net.luis.agent.preload.scanner.instructions;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

/**
 *
 * @author Luis-St
 *
 */

public record LookupSwitchInstruction(@NotNull Label dflt, int[] keys, Label @NotNull [] labels) implements ASMInstruction {
	
	@Override
	public void copyTo(@NotNull MethodVisitor visitor) {
		visitor.visitLookupSwitchInsn(this.dflt, this.keys, this.labels);
	}
}
