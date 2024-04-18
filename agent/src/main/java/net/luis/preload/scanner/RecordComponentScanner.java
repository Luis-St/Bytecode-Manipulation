package net.luis.preload.scanner;

import net.luis.asm.base.visitor.BaseRecordComponentVisitor;
import org.objectweb.asm.AnnotationVisitor;

/**
 *
 * @author Luis-St
 *
 */

public class RecordComponentScanner extends BaseRecordComponentVisitor {
	
	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		return super.visitAnnotation(descriptor, visible);
	}
}
