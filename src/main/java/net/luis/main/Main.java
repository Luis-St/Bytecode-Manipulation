package net.luis.main;

import net.luis.agent.Agent;
import net.luis.override.INumber;

/**
 *
 * @author Luis
 *
 */

public class Main {
	
	public static void main(String[] args) {
		System.out.println("Hello World!");
		Number i = 1;
		if (i instanceof INumber number) {
			System.out.println("Number is an instance of INumber");
			System.out.println(number.pow(2));
			System.out.println(number.sqrt());
		}
	}
}
