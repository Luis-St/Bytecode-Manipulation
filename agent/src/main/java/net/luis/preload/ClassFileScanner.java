package net.luis.preload;

import net.luis.asm.base.*;
import org.objectweb.asm.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;
import java.util.function.BiConsumer;

/**
 *
 * @author Luis-St
 *
 */

public class ClassFileScanner {
	
	public Map<String, Map<String, Object>> scanClassAnnotations(String clazz) {
		ClassAnnotationScanner visitor = new ClassAnnotationScanner();
		this.scan(clazz, visitor);
		return visitor.getAnnotations();
	}
	
	private byte[] readClass(String clazz) {
		String path = clazz.replace('.', '/') + ".class";
		InputStream stream = ClassLoader.getSystemResourceAsStream(path);
		if (stream == null) {
			throw new IllegalStateException("Class not found in classpath: " + clazz);
		}
		try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
			int data;
			while (true) {
				data = stream.read();
				if (data == -1) {
					break;
				}
				buffer.write(data);
			}
			return buffer.toByteArray();
		} catch (Exception e) {
			throw new IllegalStateException("Failed to read class file: " + clazz, e);
		}
	}
	
	private void scan(String clazz, ClassVisitor visitor) {
		ClassReader reader = new ClassReader(this.readClass(clazz));
		reader.accept(visitor, 0);
	}
	
	private static class ClassAnnotationScanner extends BaseClassVisitor {
		
		private final Map<String, Map<String, Object>> annotations = new HashMap<>();
		
		@Override
		public AnnotationVisitor visitAnnotation(String annotationDescriptor, boolean visible) {
			Map<String, Object> values = new HashMap<>();
			this.annotations.put(annotationDescriptor, values);
			return new AnnotationScanner(values::put);
		}
		
		public Map<String, Map<String, Object>> getAnnotations() {
			return this.annotations;
		}
	}
	
	//region Annotation scanner
	private static class AnnotationScanner extends BaseAnnotationVisitor {
		
		private final BiConsumer<String, Object> consumer;
		
		private AnnotationScanner(BiConsumer<String, Object> consumer) {
			this.consumer = consumer;
		}
		
		@Override
		public void visit(String parameter, Object value) {
			this.consumer.accept(parameter, value);
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
			};
		}
	}
	//endregion
}
