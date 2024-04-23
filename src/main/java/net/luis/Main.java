package net.luis;

import net.luis.utils.logging.LoggerConfiguration;

/**
 *
 * @author Luis
 *
 */

@Test(
	testInt = -1,
	testString = "Hello World!",
	testBoolean = true,
	testDouble = 0.0,
	testClass = Main.class,
	testIntArray = {1, 2, 3},
	testStringArray = {"Hello", "World", "!"},
	testClassArray = {Main.class},
	testAnnotation = @AnnotationExample("test")
)
public final class Main {
	
	public static void main(String[] args) {
		System.out.println("Hello World!");
		
		LoggerConfiguration logger = new LoggerConfiguration("*");
		
		if (logger instanceof MyInterface my) {
			System.out.println("LoggerConfiguration is an instance of MyInterface!");
			System.out.println(my.build().getName());
		} else {
			System.out.println("LoggerConfiguration is not an instance of MyInterface!");
		}
	}
}
