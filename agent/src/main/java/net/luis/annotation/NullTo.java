package net.luis.annotation;

import java.lang.annotation.*;

/**
 *
 * @author Luis-St
 *
 */

@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
public @interface NullTo {}
