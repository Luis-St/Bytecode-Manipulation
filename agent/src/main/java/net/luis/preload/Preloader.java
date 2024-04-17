package net.luis.preload;

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
		Map<String, Map<String, Object>> classAnnotations = new HashMap<>();
		for (String clazz : classes) {
			classAnnotations.putAll(ClassFileScanner.scanClassAnnotations(clazz));
		}
		return PreloadContext.create(classes, classAnnotations);
	}
}
