package net.luis.agent.asm.generation;

import net.luis.agent.asm.ASMUtils;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author Luis-St
 */

public class GenerationLoader {
	
	private static final Method DEFINE_CLASS;
	
	public void loadClass(@NotNull Map<Type, byte[]> generated, @NotNull Generator generator) {
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		generator.generate(writer);
		byte[] bytes = writer.toByteArray();
		
		Type type = Type.getObjectType(generator.getName().replace(".", "/"));
		ASMUtils.saveClass(new File("generated/" + type.getInternalName() + ".class"), bytes);
		this.defineClass(type.getClassName(), bytes);
		System.out.println("Generated class: " + type.getClassName());
		generated.put(type, bytes);
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
