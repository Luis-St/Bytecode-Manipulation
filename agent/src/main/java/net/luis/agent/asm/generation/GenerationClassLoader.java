package net.luis.agent.asm.generation;

import net.luis.agent.asm.ASMUtils;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassWriter;

import java.io.File;

/**
 * @author Luis-St
 */

public class GenerationClassLoader extends ClassLoader {
	
	private static final String PACKAGE = "net.luis.agent.generated.";
	
	public void loadClass(@NotNull Generator generator) {
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		generator.generate(writer);
		byte[] bytes = writer.toByteArray();
		
		String name = generator.getName();
		ASMUtils.saveClass(new File("generated/net/luis/agent/generated/" + name + ".class"), bytes);
		this.defineClass(name, bytes);
		System.out.println("Generated class: " + PACKAGE.replace("/", ".") + name);
	}
	
	private void defineClass(@NotNull String simpleName, byte[] bytes) {
		this.defineClass(PACKAGE + simpleName, bytes, 0, bytes.length);
	}
}
