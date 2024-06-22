package net.luis.agent.annotation.string.modification;

import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 *
 * @author Luis-St
 *
 */

@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE /*Local Variable Only*/ })
public @interface Replace {
	
	@NotNull String value() default ""; // Formated as "target -> replacement"
	
	@Language("RegExp")
	@NotNull String regex() default "";
	
	@NotNull String replacement() default "";
	
	boolean all() default true;
}
