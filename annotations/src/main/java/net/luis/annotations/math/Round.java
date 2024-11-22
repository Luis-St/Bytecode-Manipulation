package net.luis.annotations.math;

import net.luis.annotations.configuration.RoundingMode;
import net.luis.annotations.util.ImplicitNotNull;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 *
 * @author Luis-St
 *
 */

@ImplicitNotNull
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE /*Local Variable Only*/ })
public @interface Round {
	
	@NotNull RoundingMode mode() default RoundingMode.ROUND;
	
	// Floor & Ceil -> Not used
	// Round -> Decimals to round to
	// Other Floor & Ceil -> Divisor
	long value() default 1;
}
