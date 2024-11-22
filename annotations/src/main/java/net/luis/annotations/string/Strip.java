package net.luis.annotations.string;

import net.luis.annotations.configuration.StripMode;
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
public @interface Strip {
	
	@NotNull StripMode value() default StripMode.BOTH;
}
