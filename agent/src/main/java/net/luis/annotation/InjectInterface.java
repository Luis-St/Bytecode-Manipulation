package net.luis.annotation;

import java.lang.annotation.*;

/**
 *
 * @author Luis-St
 *
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface InjectInterface {
	
	Class<?>[] targets();
}
