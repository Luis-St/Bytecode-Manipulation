package net.luis.agent.preload.scanner.instructions;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.MethodVisitor;

/**
 *
 * @author Luis-St
 *
 */

public record FrameInstruction(int type, int numLocal, Object @NotNull [] local, int numStack, Object @NotNull [] stack) implements ASMInstruction {
	
	@Override
	public void copyTo(@NotNull MethodVisitor visitor) {
		visitor.visitFrame(this.type, this.numLocal, this.local, this.numStack, this.stack);
	}
}
