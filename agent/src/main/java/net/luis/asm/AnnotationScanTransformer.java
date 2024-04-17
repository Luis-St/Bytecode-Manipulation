package net.luis.asm;

import org.objectweb.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.*;

/**
 *
 * @author Luis-St
 *
 */

public class AnnotationScanTransformer implements ClassFileTransformer {
	
	private final Map<String, Map<String, Object>> scanData = new HashMap<>();
	
	@Override
	public byte[] transform(ClassLoader loader, String name, Class<?> clazz, ProtectionDomain domain, byte[] buffer) {
		ClassReader reader = new ClassReader(buffer);
		ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
		AnnotationScanner scanner = new AnnotationScanner(writer);
		reader.accept(scanner, ClassReader.EXPAND_FRAMES);
		Map<String, Object> scanData = scanner.getScanData();
		if (!scanData.isEmpty()) {
			this.scanData.put(name, scanner.getScanData());
			System.out.println("Annotation scan data for " + name + ": " + scanData.size());
		}
		return writer.toByteArray();
	}
	
	private static class AnnotationScanner extends ClassVisitor {
		
		private final Map<String, Object> scanData = new HashMap<>();
		
		private AnnotationScanner(ClassWriter writer) {
			super(Opcodes.ASM9, writer);
		}
		
		public Map<String, Object> getScanData() {
			return this.scanData;
		}
		
		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			return new AnnotationVisitor(Opcodes.ASM9, super.visitAnnotation(descriptor, visible)) {
				
				@Override
				public void visit(String name, Object value) {
					AnnotationScanner.this.scanData.put(name, value);
					super.visit(name, value);
				}
				
				@Override
				public AnnotationVisitor visitArray(String name) {
					List<Object> values = new ArrayList<>();
					AnnotationScanner.this.scanData.put(name, values);
					return new AnnotationVisitor(Opcodes.ASM9, super.visitArray(name)) {
						
						@Override
						public void visit(String name, Object value) {
							values.add(value);
							super.visit(name, value);
						}
					};
				}
			};
		}
	}
}
