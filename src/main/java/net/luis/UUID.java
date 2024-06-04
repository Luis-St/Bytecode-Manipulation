package net.luis;

import org.intellij.lang.annotations.Pattern;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.*;

/**
 *
 * @author Luis-St
 *
 */

@Target({ METHOD, PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Pattern("[\\dA-Fa-f]{8}-[\\dA-Fa-f]{4}-[\\dA-Fa-f]{4}-[\\dA-Fa-f]{4}-[\\dA-Fa-f]{12}")
public @interface UUID {}
