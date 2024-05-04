package net.luis.agent.util;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

/**
 *
 * @author Luis-St
 *
 */

public interface StringFactory {
	
	@NotNull Object create(@NotNull Type type, @NotNull String value);
}
