package net.luis.agent;

import net.luis.agent.preload.data.ClassData;
import net.luis.agent.preload.scanner.ClassFileScanner;
import net.luis.agent.preload.scanner.ClassPathScanner;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

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
	
	private final List<Type> classes = ClassPathScanner.getClasses().stream().filter(type -> !type.getDescriptor().contains("module-info")).collect(Collectors.toList());
	private final Map<Type, ClassData> cache = new HashMap<>();
	
	public static @NotNull AgentContext get() {
		return INSTANCE;
	}
	
	public @NotNull List<Type> getClasses() {
		return this.classes;
	}
	
	public @NotNull ClassData getClassData(@NotNull Type type) {
		return this.cache.computeIfAbsent(type, ClassFileScanner::scanClass);
	}
	
	public @NotNull Stream<ClassData> stream() {
		return this.getClasses().stream().map(this::getClassData);
	}
}
