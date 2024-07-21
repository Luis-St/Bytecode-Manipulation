package net.luis.agent.annotation.string.modification;

import net.luis.agent.annotation.util.ImplicitNotNull;
import net.luis.agent.util.StripMode;
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
public @interface Strip {
	
	@NotNull StripMode value() default StripMode.BOTH;
}
