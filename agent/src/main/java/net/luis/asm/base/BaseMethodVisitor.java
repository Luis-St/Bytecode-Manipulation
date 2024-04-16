package net.luis.asm.base;

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
	
	protected BaseMethodVisitor(MethodVisitor methodVisitor) {
		super(Opcodes.ASM9, methodVisitor);
	}
}
