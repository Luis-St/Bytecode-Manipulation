package net.luis.agent.annotation.string.modification;

import net.luis.agent.annotation.util.ImplicitNotNull;
import org.intellij.lang.annotations.Language;
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
public @interface Replace {
	
	@NotNull String value() default ""; // Formated as "target -> replacement"
	
	@Language("RegExp")
	@NotNull String regex() default "";
	
	@NotNull String replacement() default "";
	
	boolean all() default true;
}
