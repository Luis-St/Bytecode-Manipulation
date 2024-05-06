package net.luis.agent.asm.base.visitor;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

/**
 *
 * @author Luis-St
 *
 */

public abstract class BaseAnnotationVisitor extends AnnotationVisitor {
	
	protected BaseAnnotationVisitor() {
		super(Opcodes.ASM9);
	}
	
	protected BaseAnnotationVisitor(@NotNull AnnotationVisitor visitor) {
		super(Opcodes.ASM9, visitor);
	}
}
