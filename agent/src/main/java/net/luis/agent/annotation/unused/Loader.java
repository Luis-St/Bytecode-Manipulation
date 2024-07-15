package net.luis.agent.annotation.unused;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 *
 * @author Luis-St
 *
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Loader {
	
	@NotNull Class<? extends ClassLoader> value();
}
