package net.luis.agent.annotation;

import net.luis.agent.util.DefaultStringFactory;
import net.luis.agent.util.StringFactory;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 *
 * @author Luis-St
 *
 */

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.CLASS)
public @interface Default {
	
	@NotNull String value() default "";
	
	@NotNull Class<? extends StringFactory> factory() default DefaultStringFactory.class;
}
