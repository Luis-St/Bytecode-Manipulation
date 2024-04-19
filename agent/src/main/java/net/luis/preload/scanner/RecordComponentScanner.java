package net.luis.preload.scanner;

import net.luis.asm.base.visitor.BaseRecordComponentVisitor;
import net.luis.preload.data.AnnotationScanData;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 *
 * @author Luis-St
 *
 */

public class RecordComponentScanner extends BaseRecordComponentVisitor {
	
	private final Consumer<AnnotationScanData> consumer;
	
	public RecordComponentScanner(Consumer<AnnotationScanData> consumer) {
		this.consumer = consumer;
	}
	
	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		Map<String, Object> values = new HashMap<>();
		this.consumer.accept(new AnnotationScanData(Type.getType(descriptor), values));
		return new AnnotationScanner(values::put);
	}
}
