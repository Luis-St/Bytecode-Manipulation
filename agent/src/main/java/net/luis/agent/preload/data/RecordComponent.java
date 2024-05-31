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

public class RecordComponent implements ASMData {
	
	private final Type owner;
	private final String name;
	private final Type type;
	private final String genericSignature;
	private final Map<Type, Annotation> annotations;
	
	private RecordComponent(@NotNull Type owner, @NotNull String name, @NotNull Type type, @Nullable String genericSignature, @NotNull Map<Type, Annotation> annotations) {
		this.owner = Objects.requireNonNull(owner);
		this.name = Objects.requireNonNull(name);
		this.type = Objects.requireNonNull(type);
		this.genericSignature = genericSignature;
		this.annotations = Objects.requireNonNull(annotations);
	}
	
	public static @NotNull Builder builder(@NotNull Type owner) {
		return new Builder(owner);
	}
	
	public static @NotNull Builder builder(@NotNull Type owner, @NotNull String name, @NotNull Type type) {
		return new Builder(owner, name, type);
	}
	
	public static @NotNull Builder builder(@NotNull RecordComponent recordComponent) {
		return new Builder(recordComponent);
	}
	
	//region Getters
	public @NotNull Type getOwner() {
		return this.owner;
	}
	
	@Override
	public @NotNull String getName() {
		return this.name;
	}
	
	@Override
	public @NotNull Type getType() {
		return this.type;
	}
	
	@Override
	public @Nullable String getGenericSignature() {
		return this.genericSignature;
	}
	
	@Override
	public @NotNull TypeAccess getAccess() {
		return TypeAccess.PUBLIC;
	}
	
	@Override
	public @NotNull Set<TypeModifier> getModifiers() {
		return EnumSet.noneOf(TypeModifier.class);
	}
	
	@Override
	public @NotNull Map<Type, Annotation> getAnnotations() {
		return this.annotations;
	}
	//endregion
	
	//region Functional getters
	@Override
	public @NotNull String getSourceSignature() {
		return this.owner.getClassName() + "#" + this.name + " : " + this.type.getClassName();
	}
	//endregion
	
	//region Object overrides
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof RecordComponent that)) return false;
		
		if (!this.owner.equals(that.owner)) return false;
		if (!this.name.equals(that.name)) return false;
		if (!this.type.equals(that.type)) return false;
		if (!Objects.equals(this.genericSignature, that.genericSignature)) return false;
		return this.annotations.equals(that.annotations);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.owner, this.name, this.type, this.genericSignature, this.annotations);
	}
	
	@Override
	public String toString() {
		return this.getSourceSignature();
	}
	//endregion
	
	//region Builder
	public static class Builder {
		
		private final Map<Type, Annotation> annotations = new HashMap<>();
		private Type owner;
		private String name;
		private Type type;
		private String genericSignature;
		
		//region Constructors
		private Builder(@NotNull Type owner) {
			this.owner = owner;
		}
		
		private Builder(@NotNull Type owner, @NotNull String name, @NotNull Type type) {
			this.owner = owner;
			this.name = name;
			this.type = type;
		}
		
		private Builder(@NotNull RecordComponent recordComponent) {
			this.owner = recordComponent.owner;
			this.name = recordComponent.name;
			this.type = recordComponent.type;
			this.annotations.putAll(recordComponent.annotations);
		}
		//endregion
		
		public @NotNull Builder owner(@NotNull Type owner) {
			this.owner = owner;
			return this;
		}
		
		public @NotNull Builder name(@NotNull String name) {
			this.name = name;
			return this;
		}
		
		public @NotNull Builder type(@NotNull Type type) {
			this.type = type;
			return this;
		}
		
		public @NotNull Builder genericSignature(@Nullable String genericSignature) {
			this.genericSignature = genericSignature;
			return this;
		}
		
		//region Annotations
		public @NotNull Builder annotations(@NotNull Map<Type, Annotation> annotations) {
			this.annotations.clear();
			this.annotations.putAll(annotations);
			return this;
		}
		
		public @NotNull Builder clearAnnotations() {
			this.annotations.clear();
			return this;
		}
		
		public @NotNull Builder addAnnotation(@NotNull Type type, @NotNull Annotation annotation) {
			this.annotations.put(type, annotation);
			return this;
		}
		
		public @NotNull Builder removeAnnotation(@NotNull Type type) {
			this.annotations.remove(type);
			return this;
		}
		//endregion
		
		public @NotNull RecordComponent build() {
			return new RecordComponent(this.owner, this.name, this.type, this.genericSignature, this.annotations);
		}
	}
	//endregion
}
