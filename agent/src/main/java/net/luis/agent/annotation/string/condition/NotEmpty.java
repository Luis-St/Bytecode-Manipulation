package net.luis.agent.annotation.string.condition;

import java.lang.annotation.*;

/**
 *
 * @author Luis-St
 *
 */
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.METHOD, ElementType.PARAMETER })
public @interface NotEmpty {}
