package net.luis.agent.preload.data;

import net.luis.agent.preload.type.TypeAccess;
import net.luis.agent.preload.type.TypeModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.util.*;

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
	
	//region Copy
	public @NotNull FieldData copy(@NotNull String name) {
		return new FieldData(this.owner, name, this.type, this.signature, this.access, EnumSet.copyOf(this.modifiers), new HashMap<>(this.annotations), null);
	}
	
	public @NotNull FieldData copy(@NotNull Type type) {
		return new FieldData(this.owner, this.name, type, this.signature, this.access, EnumSet.copyOf(this.modifiers), new HashMap<>(this.annotations), null);
	}
	
	public @NotNull FieldData copy(@NotNull TypeAccess access) {
		return new FieldData(this.owner, this.name, this.type, this.signature, access, EnumSet.copyOf(this.modifiers), new HashMap<>(this.annotations), null);
	}
	
	public @NotNull FieldData copy(@NotNull Set<TypeModifier> modifiers) {
		return new FieldData(this.owner, this.name, this.type, this.signature, this.access, modifiers, new HashMap<>(this.annotations), null);
	}
	
	public @NotNull FieldData copy(@NotNull Map<Type, AnnotationData> annotations) {
		return new FieldData(this.owner, this.name, this.type, this.signature, this.access, EnumSet.copyOf(this.modifiers), annotations, null);
	}
	//endregion
}
