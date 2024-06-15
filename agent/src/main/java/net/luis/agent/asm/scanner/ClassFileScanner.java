package net.luis.agent.asm.scanner;

import net.luis.agent.annotation.RestrictedAccess;
import net.luis.agent.asm.data.Class;
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
	
	public static @NotNull Class scanClass(@NotNull Type type) {
		return scanClass(type, new ClassScanner(), ClassScanner::get);
	}
	
	public static <T extends ClassVisitor> void scanClass(@NotNull Type type, @NotNull T visitor) {
		scanClass(readClass(type), visitor, Function.identity());
	}
	
	@RestrictedAccess("net.luis.agent.AgentContext#initialize")
	public static @NotNull Class scanGeneratedClass(byte @NotNull [] bytes) {
		return scanClass(bytes, new ClassScanner(), ClassScanner::get);
	}
	
	//region Helper methods
	private static <T extends ClassVisitor, X> @NotNull X scanClass(@NotNull Type type, @NotNull T visitor, @NotNull Function<T, X> result) {
		return scanClass(readClass(type), visitor, result);
	}
	
	private static <T extends ClassVisitor, X> @NotNull X scanClass(byte @NotNull [] bytes, @NotNull T visitor, @NotNull Function<T, X> result) {
		ClassReader reader = new ClassReader(bytes);
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
