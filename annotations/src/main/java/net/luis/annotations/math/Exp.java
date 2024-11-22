package net.luis.annotations.math;

import net.luis.annotations.configuration.ExponentialOperation;
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
public @interface Exp {
	
	@NotNull ExponentialOperation value() default ExponentialOperation.EXP;
}
