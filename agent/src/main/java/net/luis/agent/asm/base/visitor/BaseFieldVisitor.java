package net.luis.agent.asm.base.visitor;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

/**
 *
 * @author Luis-St
 *
 */

public class BaseFieldVisitor extends FieldVisitor {
	
	protected BaseFieldVisitor() {
		super(Opcodes.ASM9);
	}
	
	protected BaseFieldVisitor(@NotNull FieldVisitor visitor) {
		super(Opcodes.ASM9, visitor);
	}
}
