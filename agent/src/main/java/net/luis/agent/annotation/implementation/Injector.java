package net.luis.agent.annotation.implementation;

import net.luis.agent.util.InjectMode;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 *
 * @author Luis-St
 *
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface Injector {
	
	@NotNull String method() default "";
	
	@NotNull String target();
	
	int ordinal() default 0;
	
	@NotNull InjectMode mode() default InjectMode.BEFORE;
}
