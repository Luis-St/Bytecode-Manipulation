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

public record ParameterData(@NotNull String name, @NotNull Type type, int index, @NotNull Set<TypeModifier> modifiers, @NotNull Map<Type, AnnotationData> annotations) implements ASMData {
	
	@Override
	public @Nullable String signature() {
		return null;
	}
	
	public boolean isNamed() {
		return !this.name.isBlank() && !this.name.equals("arg" + this.index);
	}
}
