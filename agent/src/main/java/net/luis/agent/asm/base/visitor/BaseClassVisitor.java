package net.luis.agent.asm.base.visitor;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

/**
 *
 * @author Luis-St
 *
 */

public abstract class BaseClassVisitor extends ClassVisitor {
	
	protected BaseClassVisitor() {
		super(Opcodes.ASM9);
	}
	
	protected BaseClassVisitor(ClassVisitor visitor) {
		super(Opcodes.ASM9, visitor);
	}
}
