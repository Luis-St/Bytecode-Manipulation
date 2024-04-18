package net.luis.preload.scanner;

import net.luis.asm.base.visitor.BaseMethodVisitor;
import org.objectweb.asm.*;

/**
 *
 * @author Luis-St
 *
 */

public class MethodScanner extends BaseMethodVisitor {
	
	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		return super.visitAnnotation(descriptor, visible);
	}
	
	@Override
	public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
		return super.visitParameterAnnotation(parameter, descriptor, visible);
	}
	
	@Override
	public void visitParameter(String name, int access) {
		super.visitParameter(name, access);
	}
}
