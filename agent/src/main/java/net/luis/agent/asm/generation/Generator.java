package net.luis.agent.asm.generation;

import net.luis.agent.asm.ASMUtils;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.File;

/**
 *
 * @author Luis-St
 *
 */

public abstract class Generator {
	
	protected static final String PACKAGE = "net/luis/agent/generated/";
	protected static final int CLASS_VERSION = 65; // Java 21
	
	protected final String name;
	
	protected Generator(@NotNull String name) {
		this.name = name;
	}
	
	public @NotNull String getName() {
		return this.name;
	}
	
	public abstract void generate(@NotNull ClassVisitor cv);
}
