package net.luis.preload;

import net.luis.preload.data.*;
import net.luis.preload.scanner.ClassContentScanner;
import net.luis.preload.scanner.ClassInfoScanner;
import org.objectweb.asm.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Function;

/**
 *
 * @author Luis-St
 *
 */

public class ClassFileScanner {
	
	public static Map.Entry<ClassInfo, ClassContent> scanClass(Type type) {
		byte[] data = readClass(type);
		ClassInfo info = scanClass(data, type, new ClassInfoScanner(), ClassInfoScanner::getClassInfo);
		ClassContent content = scanClass(data, type, new ClassContentScanner(), ClassContentScanner::getClassContent);
		return Map.entry(info, content);
	}
	
	public static ClassInfo scanClassInfo(Type type) {
		return scanClass(type, new ClassInfoScanner(), ClassInfoScanner::getClassInfo);
	}
	
	public static ClassContent scanClassContent(Type type) {
		return scanClass(type, new ClassContentScanner(), ClassContentScanner::getClassContent);
	}
	
	//region Helper methods
	private static <T extends ClassVisitor, X> X scanClass(Type type, T visitor, Function<T, X> result) {
		return scanClass(readClass(type), type, visitor, result);
	}
	
	private static <T extends ClassVisitor, X> X scanClass(byte[] data, Type type, T visitor, Function<T, X> result) {
		ClassReader reader = new ClassReader(data);
		reader.accept(visitor, 0);
		return result.apply(visitor);
	}
	//endregion
	
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
