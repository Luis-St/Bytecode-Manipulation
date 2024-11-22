package net.luis.annotations;

import net.luis.annotations.configuration.CaughtAction;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 *
 * @author Luis-St
 *
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface Caught {
	
	@NotNull CaughtAction value() default CaughtAction.NOTHING;
	
	@NotNull Class<? extends Throwable> exceptionType() default Throwable.class;
}
