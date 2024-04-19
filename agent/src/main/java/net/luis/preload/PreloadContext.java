package net.luis.preload;

import net.luis.preload.data.AnnotationScanData;

import java.util.*;

/**
 *
 * @author Luis-St
 *
 */

public class PreloadContext {
	
	private final List<String> classes;
	private final Map<String, List<AnnotationScanData>> classAnnotations;
	
	private PreloadContext(List<String> classes, Map<String, List<AnnotationScanData>> classAnnotations) {
		this.classes = classes;
		this.classAnnotations = classAnnotations;
	}
	
	public static PreloadContext create(List<String> classes, Map<String, List<AnnotationScanData>> classAnnotations) {
		return new PreloadContext(classes, classAnnotations);
	}
	
	public List<String> getClasses() {
		return this.classes;
	}
	
	public Map<String, List<AnnotationScanData>> getClassAnnotations() {
		return this.classAnnotations;
	}
	
	public List<AnnotationScanData> getClassAnnotations(String clazz) {
		return this.classAnnotations.getOrDefault(clazz, new ArrayList<>());
	}
	
	public AnnotationScanData getClassAnnotation(String clazz, String annotationDescriptor) {
		return this.getClassAnnotations(clazz).stream().filter(data -> data.type().getDescriptor().equals(annotationDescriptor)).findFirst().orElse(null);
	}
}
