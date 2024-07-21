package net.luis.agent.annotation.string.modification;

import net.luis.agent.annotation.util.ImplicitNotNull;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 *
 * @author Luis-St
 *
 */

@ImplicitNotNull
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE /*Local Variable Only*/ })
public @interface Substring {
	
	@NotNull String value() default "0:*"; // Format: start:end, start:* (start:<length>), *:end (0:end)
}
