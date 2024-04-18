package net.luis.preload.scanner;

import net.luis.asm.base.visitor.BaseFieldVisitor;
import org.objectweb.asm.AnnotationVisitor;

/**
 *
 * @author Luis-St
 *
 */

public class FieldScanner extends BaseFieldVisitor {
	
	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		return super.visitAnnotation(descriptor, visible);
	}
}
