package net.luis.agent.preload.scanner.instructions;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;

/**
 *
 * @author Luis-St
 *
 */

public record InvokeDynamicInstruction(@NotNull String name, @NotNull String descriptor, @NotNull Handle handle, Object @NotNull ... arguments) implements ASMInstruction {
	
	@Override
	public void copyTo(@NotNull MethodVisitor visitor) {
		visitor.visitInvokeDynamicInsn(this.name, this.descriptor, this.handle, this.arguments);
	}
}
