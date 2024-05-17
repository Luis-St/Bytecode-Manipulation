package net.luis.agent.annotation;

import net.luis.agent.util.CaughtAction;
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
	
	Class<? extends Throwable> exceptionType() default Throwable.class;
}
