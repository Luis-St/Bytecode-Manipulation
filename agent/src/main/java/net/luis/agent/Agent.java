package net.luis.agent;

import net.luis.agent.asm.data.Class;
import net.luis.agent.asm.scanner.ClassFileScanner;
import net.luis.agent.asm.scanner.ClassPathScanner;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.stream.Stream;

/**
 *
 * @author Luis-St
 *
 */

public class Agent {
	
	private static final List<Type> classes = ClassPathScanner.getClasses().stream().filter(type -> !type.getDescriptor().contains("module-info") && !type.getDescriptor().contains("package-info")).toList();
	private static final List<Type> generated = new ArrayList<>();
	private static final Map<Type, Class> cache = new HashMap<>();
	
	public static void initialize(@NotNull Map<Type, byte[]> generatedLookup) {
		long start = System.currentTimeMillis();
		classes.forEach(type -> cache.put(type, ClassFileScanner.scanClass(type)));
		System.out.println("Loaded " + classes.size() + " classes");
		generated.addAll(generatedLookup.keySet());
		generated.forEach(type -> cache.put(type, ClassFileScanner.scanGeneratedClass(generatedLookup.get(type))));
		System.out.println("Loaded " + generated.size() + " generated classes");
		System.out.println("Initialized agent in " + (System.currentTimeMillis() - start) + "ms");
	}
	
	public static @NotNull List<Type> getGenerated() {
		return generated;
	}
	
	public static @NotNull Class getClass(@NotNull Type type) {
		Class clazz = ClassFileScanner.scanClass(type);
		if (cache.containsKey(type)) {
			return cache.get(type);
		} else {
			cache.put(type, clazz);
			return clazz;
		}
	}
	
	public static @NotNull Stream<Class> stream() {
		return classes.stream().map(Agent::getClass);
	}
}
