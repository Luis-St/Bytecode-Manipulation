package net.luis.agent.asm.base.visitor;

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
	
	protected BaseAnnotationVisitor(AnnotationVisitor annotationVisitor) {
		super(Opcodes.ASM9, annotationVisitor);
	}
}
