package net.luis.agent.annotation.string.modification;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 *
 * @author Luis-St
 *
 */

@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.METHOD, ElementType.PARAMETER })
public @interface Replace {
	
	@NotNull String value() default "";
	
	@NotNull String regex() default "";
	
	@NotNull String replacement() default "";
	
	boolean all() default false;
}
