package net.luis.agent.preload.data;

import net.luis.agent.preload.type.TypeAccess;
import net.luis.agent.preload.type.TypeModifier;
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
	
	//region Type
	@NotNull Type type();
	
	default boolean is(@NotNull Type type) {
		return this.type().equals(type);
	}
	
	default boolean isAny(@NotNull Type... type) {
		return Arrays.stream(type).anyMatch(this::is);
	}
	//endregion
	
	@Nullable String signature();
	
	//region Access & Modifiers
	@NotNull TypeAccess access();
	
	default boolean is(@NotNull TypeAccess access) {
		return this.access() == access;
	}
	
	@NotNull Set<TypeModifier> modifiers();
	
	default boolean is(@NotNull TypeModifier modifier) {
		return this.modifiers().contains(modifier);
	}
	
	default boolean is(@NotNull TypeAccess access, @NotNull TypeModifier... modifiers) {
		return this.access() == access && Arrays.stream(modifiers).allMatch(this.modifiers()::contains);
	}
	//endregion
	
	//region Annotations
	@ApiStatus.Internal
	@NotNull Map<Type, AnnotationData> annotations();
	
	default @Unmodifiable @NotNull List<AnnotationData> getAnnotations() {
		return List.copyOf(this.annotations().values());
	}
	
	default boolean isAnnotatedWith(@Nullable Type type) {
		return type != null && this.annotations().containsKey(type);
	}
	
	default boolean isAnnotatedWithAny(@NotNull Type... type) {
		return type != null && Arrays.stream(type).anyMatch(this::isAnnotatedWith);
	}
	
	default @NotNull AnnotationData getAnnotation(@NotNull Type type) {
		return this.annotations().get(type);
	}
	//endregion
}
