package net.luis.agent.preload.scanner.instructions;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.MethodVisitor;

/**
 *
 * @author Luis-St
 *
 */

public interface ASMInstruction {
	
	void copyTo(@NotNull MethodVisitor visitor);
}
