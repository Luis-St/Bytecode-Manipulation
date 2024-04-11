package net.luis.main;

import net.luis.overrides.ITestInterface;
import net.luis.utils.logging.LoggerConfiguration;

/**
 *
 * @author Luis
 *
 */

public class Main {
	
	public static void main(String[] args) {
		System.out.println("Hello World!");
		
		LoggerConfiguration configuration = new LoggerConfiguration("*");
		if (configuration instanceof ITestInterface test) {
			System.out.println("LoggerConfiguration is an instance of ITestInterface");
			System.out.println(test.pow(2));
			System.out.println(test.sqrt());
		}
	}
}
