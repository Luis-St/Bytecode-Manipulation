package net.luis.agent.preload;

import net.luis.agent.preload.data.ClassContent;
import net.luis.agent.preload.data.ClassInfo;
import net.luis.agent.preload.scanner.ClassFileScanner;
import net.luis.agent.preload.scanner.ClassPathScanner;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * @author Luis-St
 *
 */

public class PreloadContext {
	
	private final List<Type> classes = ClassPathScanner.getClasses().stream().filter(type -> !type.getDescriptor().contains("module-info")).collect(Collectors.toList());
	private final Map<Type, ClassInfo> infoCache = new HashMap<>();
	private final Map<Type, ClassContent> contentCache = new HashMap<>();
	
	public @NotNull List<Type> getClasses() {
		return this.classes;
	}
	
	public @NotNull ClassInfo getClassInfo(@NotNull Type type) {
		return this.infoCache.computeIfAbsent(type, ClassFileScanner::scanClassInfo);
	}
	
	public @NotNull ClassContent getClassContent(@NotNull Type type) {
		return this.contentCache.computeIfAbsent(type, ClassFileScanner::scanClassContent);
	}
	
	public @NotNull ClassDataStream stream() {
		return new ClassDataStream(this.getClasses().stream().map(type -> Map.entry(this.getClassInfo(type), this.getClassContent(type))));
	}
}
