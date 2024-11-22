package net.luis.annotations;

import net.luis.annotations.util.ImplicitNotNull;
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
