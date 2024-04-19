package net.luis.preload;

import net.luis.preload.data.AnnotationData;
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
		Map<String, List<AnnotationData>> classAnnotations = new HashMap<>();
		
		
		ClassFileScanner.scanClass(Type.getType("Lnet/luis/AnnotationExample;"));
		ClassFileScanner.scanClass(Type.getType("Lnet/luis/ClassExample;"));
		ClassFileScanner.scanClass(Type.getType("Lnet/luis/EnumExample;"));
		ClassFileScanner.scanClass(Type.getType("Lnet/luis/InterfaceExample;"));
		ClassFileScanner.scanClass(Type.getType("Lnet/luis/RecordExample;"));
		ClassFileScanner.scanClass(Type.getType("Lnet/luis/Main;"));
		
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
