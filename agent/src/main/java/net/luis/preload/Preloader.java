package net.luis.preload;

import net.luis.preload.data.AnnotationData;

import java.util.*;

/**
 *
 * @author Luis-St
 *
 */

public class Preloader {
	
	public static PreloadContext preload() {
		System.out.println("Preloading");
		List<String> classes = ClassPathScanner.getClasses();
		Map<String, List<AnnotationData>> classAnnotations = new HashMap<>();
		for (String clazz : classes) {
			List<AnnotationData> annotations = ClassFileScanner.scanClassAnnotations(clazz);
			if (annotations.isEmpty()) {
				continue;
			}
			classAnnotations.put(clazz, annotations);
		}
		return PreloadContext.create(classes, classAnnotations);
	}
}
