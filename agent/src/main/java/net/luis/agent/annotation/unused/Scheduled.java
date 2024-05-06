package net.luis.agent.annotation.unused;

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
	
	long delay();
	
	TimeUnit unit() default TimeUnit.MILLISECONDS;
	
	boolean fixedRate() default true;
}
