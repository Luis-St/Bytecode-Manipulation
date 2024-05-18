package net.luis.agent.annotation;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Luis-St
 *
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface Scheduled {
	
	long initialDelay() default 0;
	
	long value();
	
	@NotNull TimeUnit unit() default TimeUnit.MILLISECONDS;
	
	boolean fixedRate() default true;
}
