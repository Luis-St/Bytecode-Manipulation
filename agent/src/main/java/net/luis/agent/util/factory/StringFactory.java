package net.luis.agent.util.factory;

import net.luis.agent.asm.signature.ActualType;
import net.luis.utils.io.reader.ScopedStringReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *
 * @author Luis-St
 *
 */

public interface StringFactory {
	
	@NotNull Object create(@NotNull String type, @NotNull ActualType actual, @NotNull ScopedStringReader reader);
}
