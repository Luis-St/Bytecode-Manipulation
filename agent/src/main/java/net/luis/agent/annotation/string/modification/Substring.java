package net.luis.agent.annotation.string.modification;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 *
 * @author Luis-St
 *
 */

@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE /*Local Variable Only*/ })
public @interface Substring {
	
	@NotNull String value() default "0:*"; // Format: start:end, start:* (start:<length>), *:end (0:end)
}
