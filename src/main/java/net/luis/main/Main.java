package net.luis.main;

import net.luis.agent.Agent;
import net.luis.override.INumber;

import java.util.List;

/**
 *
 * @author Luis
 *
 */

public class Main {
	
	public static void main(String[] args) throws ClassNotFoundException {
		System.out.println("Hello World!");
		
		Main.class.getClassLoader().loadClass("java.lang.Number");
		Number number = Integer.valueOf(10);
		System.out.println(number);
		if (number instanceof INumber iNumber) {
			System.out.println("Number is an instance of INumber");
			System.out.println(iNumber.pow(2));
			System.out.println(iNumber.sqrt());
		}
	}
}
