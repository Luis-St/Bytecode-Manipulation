package net.luis.agent.preload.data;

import net.luis.agent.preload.type.TypeModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.util.Map;
import java.util.Set;

/**
 *
 * @author Luis-St
 *
 */

public record RecordComponentData(@NotNull String name, @NotNull Type type, @Nullable String signature, @NotNull Map<Type, AnnotationData> annotations) implements ASMData {
	
	@Override
	public @NotNull Set<TypeModifier> modifiers() {
		return Set.of();
	}
}
