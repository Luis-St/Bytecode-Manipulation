package net.luis.agent.asm.base.visitor;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 *
 * @author Luis-St
 *
 */

public class BaseMethodVisitor extends MethodVisitor {
	
	protected BaseMethodVisitor() {
		super(Opcodes.ASM9);
	}
	
	protected BaseMethodVisitor(@NotNull MethodVisitor visitor) {
		super(Opcodes.ASM9, visitor);
	}
}
