package net.luis.preload;

import net.luis.preload.data.AnnotationData;

import java.util.*;

public class PreloadContext {
	
	private final List<String> classes;
	private final Map<String, List<AnnotationData>> classAnnotations;
	
	private PreloadContext(List<String> classes, Map<String, List<AnnotationData>> classAnnotations) {
		this.classes = classes;
		this.classAnnotations = classAnnotations;
	}
	
	public static PreloadContext create(List<String> classes, Map<String, List<AnnotationData>> classAnnotations) {
		return new PreloadContext(classes, classAnnotations);
	}
	
	public List<String> getClasses() {
		return this.classes;
	}
	
	public Map<String, List<AnnotationData>> getClassAnnotations() {
		return this.classAnnotations;
	}
	
	public List<AnnotationData> getClassAnnotation(String clazz) {
		return this.classAnnotations.getOrDefault(clazz, new ArrayList<>());
	}
	
	public AnnotationData getAnnotation(String clazz, String annotation) {
		return this.getClassAnnotation(clazz).stream().filter(data -> data.name().equals(annotation)).findFirst().orElse(null);
	}
}
