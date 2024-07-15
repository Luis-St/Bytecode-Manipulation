package net.luis.agent.asm;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Files;

/**
 @author Luis-St */

public class ASMUtils {
	
	public static void saveClass(@NotNull File file, byte @NotNull [] data) {
		try {
			Files.deleteIfExists(file.toPath());
			Files.createDirectories(file.getParentFile().toPath());
			Files.write(file.toPath(), data);
		} catch (Exception e) {
			System.err.println("Failed to save class file: " + file.getName());
		}
	}
}
