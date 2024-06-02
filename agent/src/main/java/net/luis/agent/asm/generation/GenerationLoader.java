package net.luis.agent.asm.generation;

import net.luis.agent.asm.ASMUtils;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.lang.reflect.Method;

/**
 * @author Luis-St
 */

public class GenerationLoader {
	
	private static final Method DEFINE_CLASS;
	
	public void loadClass(@NotNull Generator generator) {
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		generator.generate(writer);
		byte[] bytes = writer.toByteArray();
		
		String name = generator.getName();
		ASMUtils.saveClass(new File("generated/" + name + ".class"), bytes);
		this.defineClass(name, bytes);
		System.out.println("Generated class: " + name);
	}
	
	private void defineClass(@NotNull String name, byte[] bytes) {
		try {
			DEFINE_CLASS.invoke(ClassLoader.getSystemClassLoader(), name, bytes, 0, bytes.length);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	static {
		try {
			DEFINE_CLASS = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
			DEFINE_CLASS.setAccessible(true);
		} catch (NoSuchMethodException e) {
			throw new ExceptionInInitializerError(e);
		}
	}
}
