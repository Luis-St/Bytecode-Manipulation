package net.luis.agent.preload.scanner.instructions;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

/**
 *
 * @author Luis-St
 *
 */

public record TableSwitchInstruction(int min, int max, @NotNull Label dflt, Label @NotNull ... labels) implements ASMInstruction {
	
	@Override
	public void copyTo(@NotNull MethodVisitor visitor) {
		visitor.visitTableSwitchInsn(this.min, this.max, this.dflt, this.labels);
	}
}
