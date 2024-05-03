package net.luis.agent.annotation.unused;

import org.jetbrains.annotations.NotNull;

/**
 *
 * @author Luis-St
 *
 */

public interface StringFactory<T> {
	
	@NotNull T create(@NotNull String value); // Maybe static and only indication interface
}
