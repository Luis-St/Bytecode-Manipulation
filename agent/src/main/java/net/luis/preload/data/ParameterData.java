package net.luis.preload.data;

import net.luis.preload.type.TypeModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.util.List;

/**
 *
 * @author Luis-St
 *
 */

public record ParameterData(@NotNull String name, @NotNull Type type, int index, @NotNull List<TypeModifier> modifiers, @NotNull List<AnnotationData> annotations) implements ASMData {
	
	@Override
	public @Nullable String signature() {
		return null;
	}
}
