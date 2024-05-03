package net.luis.agent.asm.base.visitor;

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
	
	protected BaseFieldVisitor(FieldVisitor fieldVisitor) {
		super(Opcodes.ASM9, fieldVisitor);
	}
}
