package net.luis.agent.annotation.unused.math;

import net.luis.agent.util.RoundingMode;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 *
 * @author Luis-St
 *
 */

@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE /*Local Variable Only*/ })
public @interface Round { // Generate roundTo(int decimals) function
	
	@NotNull RoundingMode value() default RoundingMode.ROUND;
	
	int decimals() default 1;
}
