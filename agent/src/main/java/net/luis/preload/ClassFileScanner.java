package net.luis.preload;

import net.luis.preload.data.AnnotationData;
import net.luis.preload.scanner.ClassScanner;
import org.objectweb.asm.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Luis-St
 *
 */

public class ClassFileScanner {
	
	public static List<AnnotationData> scanClassAnnotations(Type type) {
		scan(type, new ClassScanner());
		return new ArrayList<>();
	}
	
	private static void scan(Type type, ClassVisitor visitor) {
		ClassReader reader = new ClassReader(readClass(type));
		reader.accept(visitor, 0);
	}
	
	private static byte[] readClass(Type type) {
		String path = type.getInternalName() + ".class";
		InputStream stream = ClassLoader.getSystemResourceAsStream(path);
		if (stream == null) {
			throw new IllegalStateException("Class not found in classpath: " + type.getClassName());
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
			throw new IllegalStateException("Failed to read class file: " + type.getClassName(), e);
		}
	}
}
