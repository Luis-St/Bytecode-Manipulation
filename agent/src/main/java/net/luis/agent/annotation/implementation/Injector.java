package net.luis.agent.annotation.implementation;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 *
 * @author Luis-St
 *
 */

@Retention(RetentionPolicy.CLASS)
@java.lang.annotation.Target(ElementType.METHOD)
public @interface Injector {
	
	@NotNull String method() default "";
	
	@NotNull net.luis.agent.annotation.util.Target target();
	
	boolean restricted() default true;
}
