package net.luis.preload.scanner;

import net.luis.asm.ASMUtils;
import net.luis.asm.base.visitor.BaseAnnotationVisitor;
import org.objectweb.asm.AnnotationVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 *
 * @author Luis-St
 *
 */

public class AnnotationScanner extends BaseAnnotationVisitor {
	
	private final BiConsumer<String, Object> consumer;
	
	public AnnotationScanner(BiConsumer<String, Object> consumer) {
		this.consumer = consumer;
	}
	
	@Override
	public void visit(String parameter, Object value) {
		switch (value) {
			case boolean[] a -> this.consumer.accept(parameter, ASMUtils.asList(a));
			case byte[] a -> this.consumer.accept(parameter, ASMUtils.asList(a));
			case short[] a -> this.consumer.accept(parameter, ASMUtils.asList(a));
			case int[] a -> this.consumer.accept(parameter, ASMUtils.asList(a));
			case long[] a -> this.consumer.accept(parameter, ASMUtils.asList(a));
			case float[] a -> this.consumer.accept(parameter, ASMUtils.asList(a));
			case double[] a -> this.consumer.accept(parameter, ASMUtils.asList(a));
			case char[] a -> this.consumer.accept(parameter, ASMUtils.asList(a));
			default -> this.consumer.accept(parameter, value);
		}
	}
	
	@Override
	public void visitEnum(String name, String descriptor, String value) {
		this.consumer.accept(name, value);
	}
	
	@Override
	public AnnotationVisitor visitArray(String parameter) {
		List<Object> values = new ArrayList<>();
		this.consumer.accept(parameter, values);
		return new BaseAnnotationVisitor() {
			
			@Override
			public void visit(String name, Object value) {
				values.add(value);
			}
			
			@Override
			public void visitEnum(String name, String descriptor, String value) {
				values.add(value);
			}
		};
	}
}
