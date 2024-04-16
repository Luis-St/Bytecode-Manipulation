package net.luis.preload;

import java.util.List;

/**
 *
 * @author Luis-St
 *
 */

public class Preloader {
	
	private final List<String> classes = ClassPathScanner.getClasses();
	private final AnnotationScanner scanner = new AnnotationScanner();
	
	public void preload() {
		System.out.println("Preloading");
		this.scanner.scan("net.luis.Main");
		
		
		
	}
}
