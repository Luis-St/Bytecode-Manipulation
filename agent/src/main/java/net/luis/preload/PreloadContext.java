package net.luis.preload;

import java.util.List;
import java.util.Map;

public class PreloadContext {
	
	private final List<String> classes;
	private final Map<String, Map<String, Object>> classAnnotations;
	
	private PreloadContext(List<String> classes, Map<String, Map<String, Object>> classAnnotations) {
		this.classes = classes;
		this.classAnnotations = classAnnotations;
	}
	
	public static PreloadContext create(List<String> classes, Map<String, Map<String, Object>> classAnnotations) {
		return new PreloadContext(classes, classAnnotations);
	}
	
	public List<String> getClasses() {
		return this.classes;
	}
	
	public Map<String, Map<String, Object>> getClassAnnotations() {
		return this.classAnnotations;
	}
	
	public Map<String, Object> getClassAnnotation(String clazz) {
		return this.classAnnotations.get(clazz);
	}
}
