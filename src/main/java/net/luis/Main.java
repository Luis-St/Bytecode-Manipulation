package net.luis;

import net.luis.utils.logging.LoggerConfiguration;

/**
 *
 * @author Luis
 *
 */

public class Main {
	
	public static void main(String[] args) {
		System.out.println("Hello World!");
		
		LoggerConfiguration logger = new LoggerConfiguration("*");
		
		if (logger instanceof MyInterface) {
			System.out.println("LoggerConfiguration is an instance of MyInterface!");
		} else {
			System.out.println("LoggerConfiguration is not an instance of MyInterface!");
		}
		
		
		
		
	}
}
