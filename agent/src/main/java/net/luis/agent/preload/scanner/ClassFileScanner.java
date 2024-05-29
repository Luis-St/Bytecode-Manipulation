package net.luis.agent.preload.scanner;

import net.luis.agent.preload.data.ClassData;
import org.jetbrains.annotations.NotNull;
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
	
	public static @NotNull ClassData scanClass(@NotNull Type type) {
		return scanClass(type, new ClassScanner(), ClassScanner::getClassData);
	}
	
	public static <T extends ClassVisitor, X> void scanClass(@NotNull Type type, @NotNull T visitor) {
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
