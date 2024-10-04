package net.luis.agent.annotation.math;

import net.luis.agent.annotation.util.ImplicitNotNull;
import net.luis.agent.util.TrigonometricOperation;
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
public @interface Trig {
	
	@NotNull TrigonometricOperation value();
	
	boolean degrees() default false;
}
