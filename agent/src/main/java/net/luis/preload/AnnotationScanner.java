package net.luis.preload;

import net.luis.asm.base.BaseAnnotationVisitor;
import net.luis.asm.base.BaseClassVisitor;
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

public class AnnotationScanner {
	
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
		ClassVisitor visitor = new ClassVisitor();
		reader.accept(visitor, 0);
		System.out.println("Annotation scan data for " + clazz + ": " + visitor.getScanData().size());
	}
	
	//region Class visitor for scanning annotations
	private static class ClassVisitor extends BaseClassVisitor {
		
		private final Map<String, Object> scanData = new HashMap<>();
		
		public Map<String, Object> getScanData() {
			return this.scanData;
		}
		
		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			return new AnnotationVisitor(Opcodes.ASM9, super.visitAnnotation(descriptor, visible)) {
				
				@Override
				public void visit(String parameter, Object value) {
					ClassVisitor.this.scanData.put(parameter, value);
				}
				
				@Override
				public AnnotationVisitor visitArray(String parameter) {
					List<Object> values = new ArrayList<>();
					ClassVisitor.this.scanData.put(parameter, values);
					return new AnnotationVisitor(Opcodes.ASM9) {
						
						@Override
						public void visit(String name, Object value) {
							values.add(value);
						}
					};
				}
			};
		}
		
		@Override
		public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
			
			
			
			
			
			
			
			return super.visitField(access, name, descriptor, signature, value);
		}
		
		@Override
		public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
			
			
			
			
			
			return super.visitMethod(access, name, descriptor, signature, exceptions);
		}
		
		@Override
		public void visitAttribute(Attribute attribute) {
			super.visitAttribute(attribute);
		}
	}
	
	private static class AnnotationVisitor extends BaseAnnotationVisitor {
		
		private final BiConsumer<String, Object> consumer;
		
		private AnnotationVisitor(BiConsumer<String, Object> consumer) {
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
