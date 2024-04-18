package net.luis;

/**
 *
 * @author Luis-St
 *
 */

public interface InterfaceExample {
	
	void exampleMethod();
	
	default void exampleDefaultMethod() {
		System.out.println("Default method");
	}
}
