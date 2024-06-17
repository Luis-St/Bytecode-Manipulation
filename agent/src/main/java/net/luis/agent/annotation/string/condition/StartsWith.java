package net.luis.agent.annotation.string.condition;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 *
 * @author Luis-St
 *
 */

@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.METHOD, ElementType.PARAMETER })
public @interface StartsWith {
	
	@NotNull String value();
	
	int offset() default 0;
}
