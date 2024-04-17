package net.luis.preload;

import net.luis.asm.ASMHelper;
import net.luis.asm.base.*;
import net.luis.preload.data.AnnotationData;
import org.objectweb.asm.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *
 * @author Luis-St
 *
 */

public class ClassFileScanner {
	
	public static List<AnnotationData> scanClassAnnotations(String clazz) {
		ClassAnnotationScanner visitor = new ClassAnnotationScanner();
		scan(clazz, visitor);
		return visitor.getAnnotations();
	}
	
	private static void scan(String clazz, ClassVisitor visitor) {
		ClassReader reader = new ClassReader(readClass(clazz));
		reader.accept(visitor, 0);
	}
	
	private static byte[] readClass(String clazz) {
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
	
	private static class ClassAnnotationScanner extends BaseClassVisitor {
		
		private final List<AnnotationData> annotations = new ArrayList<>();
		
		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			Map<String, Object> values = new HashMap<>();
			AnnotationData data = new AnnotationData(descriptor.substring(descriptor.lastIndexOf("/") + 1, descriptor.length() - 1), descriptor, values);
			this.annotations.add(data);
			return new AnnotationScanner(values::put);
		}
		
		public List<AnnotationData>  getAnnotations() {
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
			switch (value) {
				case boolean[] a -> this.consumer.accept(parameter, ASMHelper.asList(a));
				case byte[] a -> this.consumer.accept(parameter, ASMHelper.asList(a));
				case short[] a -> this.consumer.accept(parameter,ASMHelper.asList(a));
				case int[] a -> this.consumer.accept(parameter, ASMHelper.asList(a));
				case long[] a -> this.consumer.accept(parameter, ASMHelper.asList(a));
				case float[] a -> this.consumer.accept(parameter, ASMHelper.asList(a));
				case double[] a -> this.consumer.accept(parameter, ASMHelper.asList(a));
				case char[] a -> this.consumer.accept(parameter, ASMHelper.asList(a));
				default -> this.consumer.accept(parameter, value);
			}
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
		
		@Override
		public void visitEnum(String name, String descriptor, String value) {
			this.consumer.accept(name, value);
		}
	}
	//endregion
}
