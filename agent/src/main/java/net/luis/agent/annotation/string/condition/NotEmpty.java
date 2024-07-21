package net.luis.agent.annotation.string.condition;

import java.lang.annotation.*;

/**
 *
 * @author Luis-St
 *
 */
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE /*Local Variable Only*/ })
public @interface NotEmpty {}
