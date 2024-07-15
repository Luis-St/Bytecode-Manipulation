package net.luis.agent.annotation.unused;

import java.lang.annotation.*;

/**
 *
 * @author Luis-St
 *
 */

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.CLASS)
public @interface SetterAccessOnly {}
