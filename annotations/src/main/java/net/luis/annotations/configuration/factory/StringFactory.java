package net.luis.annotations.configuration.factory;

import net.luis.utils.io.reader.ScopedStringReader;
import org.jetbrains.annotations.NotNull;

/**
 *
 * @author Luis-St
 *
 */

public interface StringFactory {
	
	@NotNull Object create(@NotNull String type, @NotNull ActualType actual, @NotNull ScopedStringReader reader);
}
