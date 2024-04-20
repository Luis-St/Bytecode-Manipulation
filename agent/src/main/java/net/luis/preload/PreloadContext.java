package net.luis.preload;

import net.luis.asm.ASMUtils;
import net.luis.preload.data.*;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.function.Supplier;

/**
 *
 * @author Luis-St
 *
 */

public class PreloadContext {
	
	private final Map<Type, Supplier<ClassInfo>> classes;
	
	private PreloadContext(List<Type> classes) {
		this.classes = classes.stream().collect(HashMap::new, (map, type) -> map.put(type, ASMUtils.memorize(() -> ClassFileScanner.scanClassInfo(type))), HashMap::putAll);
	}
	
	public static PreloadContext create() {
		return new PreloadContext(ClassPathScanner.getClasses());
	}
	
	public List<Type> getClasses() {
		return this.classes.keySet().stream().toList();
	}
	
	public ClassInfo getClassInfo(Type type) {
		return this.classes.get(type).get();
	}
	
	public ClassContent getClassContent(Type type) {
		return this.getClassInfo(type).getClassContent();
	}
}
