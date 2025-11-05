package net.luis.agent.asm.scanner;

import net.luis.agent.annotation.RestrictedAccess;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.function.Function;

/**
 *
 * @author Luis-St
 *
 */

public class ClassFileScanner {

	public static @NotNull ClassNode scanClass(@NotNull Type type) {
		return scanClass(readClass(type));
	}

	public static <T extends ClassVisitor> void scanClass(@NotNull Type type, @NotNull T visitor) {
		scanClass(readClass(type), visitor, Function.identity());
	}

	@RestrictedAccess("net.luis.agent.AgentContext#initialize")
	public static @NotNull ClassNode scanGeneratedClass(byte @NotNull [] bytes) {
		return scanClass(bytes);
	}

	//region Helper methods
	private static @NotNull ClassNode scanClass(byte @NotNull [] bytes) {
		ClassReader reader = new ClassReader(bytes);
		ClassNode classNode = new ClassNode(Opcodes.ASM9);
		reader.accept(classNode, 0);
		return classNode;
	}

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
