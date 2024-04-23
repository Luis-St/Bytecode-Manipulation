package net.luis.preload.data;

import net.luis.preload.type.TypeModifier;
import org.jetbrains.annotations.*;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.Map;

/**
 *
 * @author Luis-St
 *
 */

public interface ASMData {
	
	@NotNull String name();
	
	@NotNull Type type();
	
	@Nullable String signature();
	
	@NotNull List<TypeModifier> modifiers();
	
	@ApiStatus.Internal
	@NotNull Map<Type, AnnotationData> annotations();
	
	default @NotNull List<AnnotationData> getAnnotations() {
		return List.copyOf(this.annotations().values());
	}
	
	default boolean isAnnotatedWith(@NotNull Type type) {
		return this.annotations().containsKey(type);
	}
	
	default @NotNull AnnotationData getAnnotation(@NotNull Type type) {
		return this.annotations().get(type);
	}
}
