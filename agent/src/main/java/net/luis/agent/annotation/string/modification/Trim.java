package net.luis.agent.annotation.string.modification;

import java.lang.annotation.*;

/**
 *
 * @author Luis-St
 *
 */

@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE /*Local Variable Only*/ })
public @interface Trim {}
