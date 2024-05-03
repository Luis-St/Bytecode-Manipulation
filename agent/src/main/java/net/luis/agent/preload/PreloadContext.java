package net.luis.agent.preload;

import net.luis.agent.preload.data.ClassContent;
import net.luis.agent.preload.data.ClassInfo;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 * @author Luis-St
 *
 */

public class PreloadContext {
	
	private final List<Type> classes = ClassPathScanner.getClasses();
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
	
	public @NotNull List<ClassInfo> getClassInfos() {
		return this.getClasses().stream().map(this::getClassInfo).toList();
	}
	
	public @NotNull List<ClassContent> getClassContents() {
		return this.getClasses().stream().map(this::getClassContent).toList();
	}
	
	public Map<Type, Map.Entry<ClassInfo, ClassContent>> getClassData() {
		return this.getClasses().stream().filter(type -> !type.getDescriptor().contains("module-info")).collect(Collectors.toMap(Function.identity(), type -> Map.entry(this.getClassInfo(type), this.getClassContent(type))));
	}
	
	public @NotNull ClassDataStream stream() {
		return new ClassDataStream(this.getClassData().values().stream());
	}
}
