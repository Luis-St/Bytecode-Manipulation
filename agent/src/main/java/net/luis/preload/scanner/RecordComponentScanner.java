package net.luis.preload.scanner;

import net.luis.asm.ASMHelper;
import net.luis.asm.base.visitor.BaseRecordComponentVisitor;
import net.luis.preload.data.AnnotationData;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 *
 * @author Luis-St
 *
 */

public class RecordComponentScanner extends BaseRecordComponentVisitor {
	
	private final List<AnnotationData> componentAnnotations = ASMHelper.newList();
	
	private final Consumer<AnnotationData> consumer;
	
	public RecordComponentScanner(Consumer<AnnotationData> consumer) {
		this.consumer = consumer;
	}
	
	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		Map<String, Object> values = new HashMap<>();
		AnnotationData data = new AnnotationData(Type.getType(descriptor), values);
		this.componentAnnotations.add(data);
		System.out.println("Annotation: " + descriptor);
		return new AnnotationScanner(values::put);
	}
}
