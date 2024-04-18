package net.luis.asm.base.visitor;

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
	
	protected BaseClassVisitor(ClassVisitor classVisitor) {
		super(Opcodes.ASM9, classVisitor);
	}
}
