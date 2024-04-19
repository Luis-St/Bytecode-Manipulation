package net.luis.preload;

import net.luis.preload.data.AnnotationData;
import net.luis.preload.data.ClassData;
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
		
		
		//ClassFileScanner.scanClass(Type.getType("Lnet/luis/AnnotationExample;"));
		//ClassFileScanner.scanClass(Type.getType("Lnet/luis/ClassExample;"));
		//ClassFileScanner.scanClass(Type.getType("Lnet/luis/EnumExample;"));
		//ClassFileScanner.scanClass(Type.getType("Lnet/luis/InterfaceExample;"));
		//ClassFileScanner.scanClass(Type.getType("Lnet/luis/RecordExample;"));
		//ClassFileScanner.scanClass(Type.getType("Lnet/luis/Main;"));
		
		for (String clazz : classes) {
			System.out.println(clazz);
			System.out.println(Type.getType("L" + clazz.replace(".", "/") + ";"));
			ClassData data = ClassFileScanner.scanClass(Type.getObjectType(clazz.replace(".", "/")));
			System.out.println(data);
			System.out.println();
		}
		return PreloadContext.create(classes, classAnnotations);
	}
}
