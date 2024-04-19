package net.luis.preload;

import net.luis.preload.data.AnnotationScanData;
import org.objectweb.asm.Type;

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
		Map<String, List<AnnotationScanData>> classAnnotations = new HashMap<>();
		
		
		ClassFileScanner.scanClassAnnotations(Type.getType("Lnet/luis/AnnotationExample;"));
		ClassFileScanner.scanClassAnnotations(Type.getType("Lnet/luis/ClassExample;"));
		ClassFileScanner.scanClassAnnotations(Type.getType("Lnet/luis/EnumExample;"));
		ClassFileScanner.scanClassAnnotations(Type.getType("Lnet/luis/InterfaceExample;"));
		ClassFileScanner.scanClassAnnotations(Type.getType("Lnet/luis/RecordExample;"));
		
		ClassFileScanner.scanClassAnnotations(Type.getType("Lnet/luis/Main;"));
		
		/*for (String clazz : classes) {
			List<AnnotationData> annotations = ClassFileScanner.scanClassAnnotations(clazz);
			if (annotations.isEmpty()) {
				continue;
			}
			classAnnotations.put(clazz, annotations);
		}*/
		return PreloadContext.create(classes, classAnnotations);
	}
}
