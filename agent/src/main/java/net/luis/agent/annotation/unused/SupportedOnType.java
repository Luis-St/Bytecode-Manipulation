package net.luis.agent.annotation.unused;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 *
 * @author Luis-St
 *
 */

@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface SupportedOnType {
	
	@NotNull Class<?>[] value() default {};
}
