package net.luis.agent.annotation;

import net.luis.agent.util.AccessModifier;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 *
 * @author Luis-St
 *
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface DefaultConstructor {
	
	@NotNull AccessModifier value();
	
	boolean instancable() default true;
}
