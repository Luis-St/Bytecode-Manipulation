package net.luis.agent.annotation.unused.math;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.lang.annotation.*;

/**
 *
 * @author Luis-St
 *
 */

@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE /*Local Variable Only*/ })
public @interface Clamp {
	
	@NotNull String value(); // Formated as "min:max" -> if "*:max" then min is <Type>.MIN_VALUE, if "min:*" then max is <Type>.MAX_VALUE
	
	double min() default Double.MIN_VALUE;
	
	double max() default Double.MAX_VALUE;
}
