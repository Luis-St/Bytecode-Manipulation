package net.luis.preload;

import net.luis.asm.ASMUtils;
import net.luis.preload.data.ClassContent;
import net.luis.preload.data.ClassInfo;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
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
	
	public List<Type> getClasses() {
		return this.classes;
	}
	
	public ClassInfo getClassInfo(Type type) {
		return this.infoCache.computeIfAbsent(type, ClassFileScanner::scanClassInfo);
	}
	
	public ClassContent getClassContent(Type type) {
		return this.contentCache.computeIfAbsent(type, ClassFileScanner::scanClassContent);
	}
	
	public List<ClassInfo> getClassInfos() {
		return this.getClasses().stream().map(this::getClassInfo).toList();
	}
	
	public List<ClassContent> getClassContents() {
		return this.getClasses().stream().map(this::getClassContent).toList();
	}
	
	public Map<Type, Map.Entry<ClassInfo, ClassContent>> getClassData() {
		return this.getClasses().stream().collect(Collectors.toMap(Function.identity(), type -> Map.entry(this.getClassInfo(type), this.getClassContent(type))));
	}
	
	public ClassDataStream stream() {
		return new ClassDataStream(this.getClassData().values().stream());
	}
}
