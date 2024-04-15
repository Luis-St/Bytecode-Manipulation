package net.luis.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface InterfaceInjection {
	
	Class<?>[] targets();
}
