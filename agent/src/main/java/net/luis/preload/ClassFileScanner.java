package net.luis.preload;

import net.luis.preload.data.*;
import net.luis.preload.scanner.ClassScanner;
import org.objectweb.asm.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.function.Function;

/**
 *
 * @author Luis-St
 *
 */

public class ClassFileScanner {
	
	public static ClassData scanClass(Type type) {
		return scanClass(type, new ClassScanner(), ClassScanner::getClassData);
	}
	
	public static ClassContentData scanContentClass(Type type) {
		return scanClass(type, new ClassScanner(), ClassScanner::getContentData);
	}
	
	private static <T extends ClassVisitor, X> X scanClass(Type type, T visitor, Function<T, X> result) {
		ClassReader reader = new ClassReader(readClass(type));
		reader.accept(visitor, 0);
		return result.apply(visitor);
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
