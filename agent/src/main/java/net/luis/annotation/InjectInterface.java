package net.luis.annotation;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 *
 * @author Luis-St
 *
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface InjectInterface {
	
	@NotNull Class<?>[] targets();
}
