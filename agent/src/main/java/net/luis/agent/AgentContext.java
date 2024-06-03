package net.luis.agent;

import net.luis.agent.asm.data.Class;
import net.luis.agent.asm.scanner.ClassFileScanner;
import net.luis.agent.asm.scanner.ClassPathScanner;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

import java.lang.instrument.Instrumentation;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author Luis-St
 *
 */

public class AgentContext {
	
	private static final AgentContext INSTANCE = new AgentContext();
	
	private final List<Type> classes = ClassPathScanner.getClasses().stream().filter(type -> !type.getDescriptor().contains("module-info") && !type.getDescriptor().contains("package-info")).collect(Collectors.toList());
	private final List<Type> generated = new ArrayList<>();
	private final Map<Type, Class> cache = new HashMap<>();
	
	public static @NotNull AgentContext get() {
		return INSTANCE;
	}
	
	public void initialize(@NotNull Map<Type, byte[]> generated) {
		long start = System.currentTimeMillis();
		this.classes.forEach(type -> this.cache.put(type, ClassFileScanner.scanClass(type)));
		System.out.println("Loaded " + this.classes.size() + " classes");
		this.generated.addAll(generated.keySet());
		this.generated.forEach(type -> this.cache.put(type, ClassFileScanner.scanGeneratedClass(type, generated.get(type))));
		System.out.println("Loaded " + this.generated.size() + " generated classes");
		System.out.println("Initialized context in " + (System.currentTimeMillis() - start) + "ms");
	}
	
	public @NotNull List<Type> getClasses() {
		return this.classes;
	}
	
	public @NotNull List<Type> getGenerated() {
		return this.generated;
	}
	
	public @NotNull Class getClass(@NotNull Type type) {
		Class clazz = ClassFileScanner.scanClass(type);
		if (this.cache.containsKey(type)) {
			return this.cache.get(type);
		} else {
			this.cache.put(type, clazz);
			return clazz;
		}
	}
	
	public @NotNull Stream<Class> stream() {
		return this.getClasses().stream().map(this::getClass);
	}
}
