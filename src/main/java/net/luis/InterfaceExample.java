package net.luis;

/**
 *
 * @author Luis-St
 *
 */

public interface InterfaceExample {
	
	void exampleMethod(String arg1, @AnnotationExample int arg2, boolean arg3, @AnnotationExample double arg4);
	
	default void exampleDefaultMethod() {
		System.out.println("Default method");
	}
}
