package net.luis.agent.preload.scanner;

import net.luis.agent.preload.data.ClassContent;
import net.luis.agent.preload.data.ClassInfo;
import org.jetbrains.annotations.NotNull;
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
	
	public static @NotNull Map.Entry<ClassInfo, ClassContent> scanClass(@NotNull Type type) {
		byte[] data = readClass(type);
		ClassInfo info = scanClass(data, type, new ClassInfoScanner(), ClassInfoScanner::getClassInfo);
		ClassContent content = scanClass(data, type, new ClassContentScanner(), ClassContentScanner::getClassContent);
		return Map.entry(info, content);
	}
	
	public static @NotNull ClassInfo scanClassInfo(@NotNull Type type) {
		return scanClass(type, new ClassInfoScanner(), ClassInfoScanner::getClassInfo);
	}
	
	public static @NotNull ClassContent scanClassContent(@NotNull Type type) {
		return scanClass(type, new ClassContentScanner(), ClassContentScanner::getClassContent);
	}
	
	public static <T extends ClassVisitor, X> void scanClassCustom(@NotNull Type type, @NotNull T visitor) {
		scanClass(readClass(type), type, visitor, Function.identity());
	}
	
	//region Helper methods
	private static <T extends ClassVisitor, X> @NotNull X scanClass(@NotNull Type type, @NotNull T visitor, @NotNull Function<T, X> result) {
		return scanClass(readClass(type), type, visitor, result);
	}
	
	private static <T extends ClassVisitor, X> @NotNull X scanClass(byte @NotNull [] data, Type type, @NotNull T visitor, @NotNull Function<T, X> result) {
		ClassReader reader = new ClassReader(data);
		reader.accept(visitor, 0);
		return result.apply(visitor);
	}
	//endregion
	
	private static byte @NotNull [] readClass(@NotNull Type type) {
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
