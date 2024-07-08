package net.luis.agent.annotation.implementation;

import net.luis.agent.util.ModifyTarget;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 *
 * @author Luis-St
 *
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface Modify {
	
	@NotNull String method() default "";
	
	@NotNull String value() default "";
	
	@NotNull ModifyTarget target();
	
	int ordinal() default 0;
	
	boolean restricted() default true;
}
