package net.luis.agent.annotation.util;

import net.luis.agent.util.TargetType;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

@Retention(RetentionPolicy.CLASS)
@java.lang.annotation.Target({})
public @interface Target {
	
	@NotNull String value();
	
	@NotNull TargetType type();
}
