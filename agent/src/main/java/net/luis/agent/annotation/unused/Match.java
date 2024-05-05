package net.luis.agent.annotation.unused;

import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

/**
 *
 * @author Luis-St
 *
 */

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.CLASS)
public @interface Match { // Placeholder for the @Pattern annotation from JetBrains Annotations
	
	@Language("RegExp")
	@NotNull String value();
}
