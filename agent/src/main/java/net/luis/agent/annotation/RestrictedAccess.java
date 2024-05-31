package net.luis.agent.annotation;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 *
 * @author Luis-St
 *
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface RestrictedAccess {
	
	@NotNull String[] value();
	
	boolean pattern() default false;
}
