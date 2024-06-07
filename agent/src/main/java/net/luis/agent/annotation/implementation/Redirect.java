package net.luis.agent.annotation.implementation;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 *
 * @author Luis-St
 *
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface Redirect {
	
	@NotNull String method() default "";
	
	@NotNull net.luis.agent.annotation.util.Target target();
	
	boolean restricted() default true;
}
