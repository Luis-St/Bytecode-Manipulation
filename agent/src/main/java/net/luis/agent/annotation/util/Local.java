package net.luis.agent.annotation.util;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Target;
import java.lang.annotation.*;

/**
 *
 * @author Luis-St
 *
 */

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.CLASS)
public @interface Local {
	
	@NotNull String value() default "";
}
