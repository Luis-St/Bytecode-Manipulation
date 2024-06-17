package net.luis.agent.annotation.string.modification;

import net.luis.agent.util.StripMode;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 *
 * @author Luis-St
 *
 */

@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.METHOD, ElementType.PARAMETER })
public @interface Strip {
	
	@NotNull StripMode value() default StripMode.BOTH;
}
