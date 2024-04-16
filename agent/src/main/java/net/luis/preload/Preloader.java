package net.luis.preload;

import java.util.List;

/**
 *
 * @author Luis-St
 *
 */

public class Preloader {
	
	private final List<String> classes = ClassPathScanner.getClasses();
	private final ClassFileScanner scanner = new ClassFileScanner();
	
	public void preload() {
		System.out.println("Preloading");
		this.scanner.scan("net.luis.Main");
		
		
		
	}
}
