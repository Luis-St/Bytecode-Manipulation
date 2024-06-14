package net.luis.agent.util.factory;

import net.luis.utils.io.reader.ScopedStringReader;
import org.jetbrains.annotations.NotNull;

/**
 *
 * @author Luis-St
 *
 */

public interface StringFactory {
	
	@NotNull Object create(@NotNull String type, @NotNull ScopedStringReader reader);
}
