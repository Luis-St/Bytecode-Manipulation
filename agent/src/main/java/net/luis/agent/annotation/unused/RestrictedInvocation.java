package net.luis.agent.annotation.unused;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 *
 * @author Luis-St
 *
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface RestrictedInvocation {
	
	@NotNull Class<?>[] value();
}
