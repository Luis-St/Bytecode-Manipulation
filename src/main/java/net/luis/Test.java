package net.luis;

/**
 *
 * @author Luis-St
 *
 */

public @interface Test {
	
	int testInt() default 0;
	
	String testString() default "";
	
	boolean testBoolean() default false;
	
	double testDouble() default 0.0;
	
	Class<?> testClass() default Object.class;
	
	int[] testIntArray() default {};
	
	String[] testStringArray() default {};
	
	Class<?>[] testClassArray() default {};
	
	AnnotationExample testAnnotation() default @AnnotationExample;
}
