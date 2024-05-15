package net.luis.agent.asm.base.visitor;

import net.luis.agent.asm.Constants;
import net.luis.agent.asm.Instrumentations;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 *
 * @author Luis-St
 *
 */

public class BaseMethodVisitor extends MethodVisitor implements Instrumentations, Constants {
	
	protected BaseMethodVisitor() {
		super(Opcodes.ASM9);
	}
	
	protected BaseMethodVisitor(@NotNull MethodVisitor visitor) {
		super(Opcodes.ASM9, visitor);
	}
	
	@Override
	public @NotNull MethodVisitor getDelegate() {
		return super.getDelegate();
	}
}
