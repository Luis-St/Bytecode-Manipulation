package net.luis.agent.annotation.util;

import net.luis.agent.util.TargetMode;
import net.luis.agent.util.TargetType;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@java.lang.annotation.Target({})
@Retention(RetentionPolicy.CLASS)
public @interface Target {
	
	@NotNull String value() default "";
	
	@NotNull TargetType type();
	
	@NotNull TargetMode mode() default TargetMode.BEFORE;
	
	int ordinal() default 0;
	
	int offset() default 0;
}
