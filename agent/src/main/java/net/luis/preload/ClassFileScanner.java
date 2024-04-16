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
	
	private byte[] readClass(String clazz) {
		String path = clazz.replace('.', '/') + ".class";
		InputStream stream = ClassLoader.getSystemResourceAsStream(path);
		if (stream == null) {
			throw new IllegalStateException("Class not found in classpath: " + clazz);
		}
		System.out.println("Reading class file: " + clazz);
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
	
	public void scan(String clazz) {
		System.out.println("Scanning " + clazz);
		ClassReader reader = new ClassReader(this.readClass(clazz));
		ClassVisitor visitor = new Visitor();
		reader.accept(visitor, 0);
	}
	
	private static class Visitor extends BaseClassVisitor {
		
		private final Map<String, Map<String, Object>> classAnnotations = new HashMap<>();
		private final Map<String, Map<String, Object>> fieldAnnotations = new HashMap<>();
		private final Map<String, Map<String, Object>> methodAnnotations = new HashMap<>();
		
		@Override
		public AnnotationVisitor visitAnnotation(String annotationDescriptor, boolean visible) {
			Map<String, Object> values = new HashMap<>();
			this.classAnnotations.put(annotationDescriptor, values);
			return new AnnotationScanner(values::put);
		}
		
		@Override
		public FieldVisitor visitField(int access, String name, String fieldDescriptor, String signature, Object value) {
			Map<String, Object> values = new HashMap<>();
			this.fieldAnnotations.put(name, values);
			return new BaseFieldVisitor() {
				
				@Override
				public AnnotationVisitor visitAnnotation(String annotationDescriptor, boolean visible) {
					return new AnnotationScanner(values::put);
				}
			};
		}
		
		@Override
		public MethodVisitor visitMethod(int access, String name, String methodDescriptor, String signature, String[] exceptions) {
			Map<String, Object> values = new HashMap<>();
			this.methodAnnotations.put(name, values);
			return new BaseMethodVisitor() {
				
				@Override
				public AnnotationVisitor visitAnnotation(String annotationDescriptor, boolean visible) {
					return new AnnotationScanner(values::put);
				}
			};
		}
		
		@Override
		public void visitAttribute(Attribute attribute) {
			super.visitAttribute(attribute);
		}
	}
	
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
}
