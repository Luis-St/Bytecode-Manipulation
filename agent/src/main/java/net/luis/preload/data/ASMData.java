package net.luis.preload.data;

import net.luis.preload.type.TypeModifier;
import org.jetbrains.annotations.*;
import org.objectweb.asm.Type;

import java.util.*;

/**
 *
 * @author Luis-St
 *
 */

public interface ASMData {
	
	@NotNull String name();
	
	@NotNull Type type();
	
	@Nullable String signature();
	
	@Unmodifiable @NotNull Set<TypeModifier> modifiers();
	
	default boolean hasModifier(@NotNull TypeModifier modifier) {
		return this.modifiers().contains(modifier);
	}
	
	//region Annotations
	@ApiStatus.Internal
	@Unmodifiable @NotNull Map<Type, AnnotationData> annotations();
	
	default @Unmodifiable @NotNull List<AnnotationData> getAnnotations() {
		return List.copyOf(this.annotations().values());
	}
	
	default boolean isAnnotatedWith(@Nullable Type type) {
		return type != null && this.annotations().containsKey(type);
	}
	
	default @NotNull AnnotationData getAnnotation(@NotNull Type type) {
		return this.annotations().get(type);
	}
	//endregion
}
