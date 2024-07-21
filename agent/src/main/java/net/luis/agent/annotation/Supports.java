package net.luis.agent.annotation;

import net.luis.agent.annotation.util.ImplicitNotNull;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 *
 * @author Luis-St
 *
 */

@ImplicitNotNull
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.CLASS)
public @interface Supports {
	
	Class<?> @NotNull [] value();
	
	boolean inherit() default true;
}
