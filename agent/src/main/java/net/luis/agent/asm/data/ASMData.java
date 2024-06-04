package net.luis.agent.asm.data;

import net.luis.agent.asm.type.TypeAccess;
import net.luis.agent.asm.type.TypeModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.util.*;

/**
 *
 * @author Luis-St
 *
 */

public interface ASMData {
	
	//region Getters
	@NotNull String getName();
	
	@NotNull Type getType();
	
	@Nullable String getGenericSignature();
	
	@NotNull TypeAccess getAccess();
	
	@NotNull Set<TypeModifier> getModifiers();
	
	@NotNull Map<Type, Annotation> getAnnotations();
	//endregion
	
	//region Functional getters
	default @NotNull Annotation getAnnotation(@NotNull Type type) {
		return this.getAnnotations().get(type);
	}
	
	default @NotNull String getFullSignature() {
		return this.getName() + this.getType();
	}
	
	@NotNull String getSourceSignature(boolean full);
	
	default int getOpcodes() {
		return this.getAccess().getOpcode() | TypeModifier.toOpcodes(this.getModifiers());
	}
	
	default boolean is(@NotNull Type type) {
		return this.getType().equals(type);
	}
	
	default boolean isAny(@NotNull Type... type) {
		return Arrays.stream(type).anyMatch(this::is);
	}
	
	default boolean is(@NotNull TypeAccess access) {
		return this.getAccess() == access;
	}
	
	default boolean is(@NotNull TypeModifier modifier) {
		return this.getModifiers().contains(modifier);
	}
	
	default boolean is(@NotNull TypeAccess access, @NotNull TypeModifier... modifiers) {
		return this.getAccess() == access && Arrays.stream(modifiers).allMatch(this.getModifiers()::contains);
	}
	
	default boolean isAnnotatedWith(@NotNull Type type) {
		return this.getAnnotations().containsKey(type);
	}
	
	default boolean isAnnotatedWithAny(@NotNull Type... type) {
		return Arrays.stream(type).anyMatch(this::isAnnotatedWith);
	}
	//endregion
}
