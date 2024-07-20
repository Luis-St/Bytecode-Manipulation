package net.luis.agent.annotation;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 *
 * @author Luis-St
 *
 */

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.CLASS)
public @interface Supports {
	
	Class<?> @NotNull [] value();
	
	boolean inherit() default true;
}
