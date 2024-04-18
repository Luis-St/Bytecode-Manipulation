package net.luis.preload;

import net.luis.preload.data.AnnotationData;

import java.util.*;

/**
 *
 * @author Luis-St
 *
 */

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
	
	public List<AnnotationData> getClassAnnotations(String clazz) {
		return this.classAnnotations.getOrDefault(clazz, new ArrayList<>());
	}
	
	public AnnotationData getClassAnnotation(String clazz, String annotationDescriptor) {
		return this.getClassAnnotations(clazz).stream().filter(data -> data.descriptor().equals(annotationDescriptor)).findFirst().orElse(null);
	}
}
