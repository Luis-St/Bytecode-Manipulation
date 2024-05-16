package net.luis.agent.asm.base.visitor;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

/**
 *
 * @author Luis-St
 *
 */

public class BaseAnnotationVisitor extends AnnotationVisitor {
	
	public BaseAnnotationVisitor() {
		super(Opcodes.ASM9);
	}
	
	public BaseAnnotationVisitor(@NotNull AnnotationVisitor visitor) {
		super(Opcodes.ASM9, visitor);
	}
}
