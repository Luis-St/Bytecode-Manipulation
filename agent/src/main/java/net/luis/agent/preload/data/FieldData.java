package net.luis.agent.preload.data;

import net.luis.agent.preload.type.TypeAccess;
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

public record FieldData(@NotNull Type owner, @NotNull String name, @NotNull Type type, @Nullable String signature, @NotNull TypeAccess access, @NotNull Set<TypeModifier> modifiers, @NotNull Map<Type, AnnotationData> annotations,
						@Nullable Object initialValue) implements ASMData {
	
	public @NotNull String getFieldSignature() {
		return this.type + this.name;
	}
}
