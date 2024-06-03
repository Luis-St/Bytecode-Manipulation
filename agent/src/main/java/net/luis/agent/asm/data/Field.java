package net.luis.agent.asm.data;

import net.luis.agent.asm.type.TypeAccess;
import net.luis.agent.asm.type.TypeModifier;
import net.luis.agent.util.Mutable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.util.*;

/**
 *
 * @author Luis-St
 *
 */

public class Field implements ASMData {
	
	private final Type owner;
	private final String name;
	private final Type type;
	private final String genericSignature;
	private final TypeAccess access;
	private final Set<TypeModifier> modifiers;
	private final Map<Type, Annotation> annotations;
	private final Mutable<Object> initialValue;
	
	private Field(@NotNull Type owner, @NotNull String name, @NotNull Type type, @Nullable String genericSignature, @NotNull TypeAccess access, @NotNull Set<TypeModifier> modifiers, @NotNull Map<Type, Annotation> annotations,
				  @NotNull Mutable<Object> initialValue) {
		this.owner = Objects.requireNonNull(owner);
		this.name = Objects.requireNonNull(name);
		this.type = Objects.requireNonNull(type);
		this.genericSignature = genericSignature;
		this.access = Objects.requireNonNull(access);
		this.modifiers = Objects.requireNonNull(modifiers);
		this.annotations = Objects.requireNonNull(annotations);
		this.initialValue = Objects.requireNonNull(initialValue);
	}
	
	public static @NotNull Field of(@NotNull Type owner, @NotNull String name, @NotNull Type type, @Nullable String genericSignature, int access) {
		return builder(owner, name, type).genericSignature(genericSignature).access(TypeAccess.fromAccess(access)).modifiers(TypeModifier.fromFieldAccess(access)).build();
	}
	
	public static @NotNull Builder builder(@NotNull Type owner) {
		return new Builder(owner);
	}
	
	public static @NotNull Builder builder(@NotNull Type owner, @NotNull String name, @NotNull Type type) {
		return new Builder(owner, name, type);
	}
	
	public static @NotNull Builder builder(@NotNull Type owner, @NotNull String name, @NotNull Type type, @NotNull TypeAccess access) {
		return new Builder(owner, name, type, access);
	}
	
	public static @NotNull Builder builder(@NotNull Field field) {
		return new Builder(field);
	}
	
	//region Getters
	public @NotNull Type getOwner() {
		return this.owner;
	}
	
	public @NotNull String getName() {
		return this.name;
	}
	
	public @NotNull Type getType() {
		return this.type;
	}
	
	public @Nullable String getGenericSignature() {
		return this.genericSignature;
	}
	
	public @NotNull TypeAccess getAccess() {
		return this.access;
	}
	
	public @NotNull Set<TypeModifier> getModifiers() {
		return this.modifiers;
	}
	
	@Override
	public @NotNull Map<Type, Annotation> getAnnotations() {
		return this.annotations;
	}
	
	public @NotNull Mutable<Object> getInitialValue() {
		return this.initialValue;
	}
	//endregion
	
	//region Functional getters
	@Override
	public @NotNull String getSourceSignature() {
		return this.owner.getClassName() + "#" + this.name + " : " + this.type.getClassName();
	}
	
	public boolean is(@NotNull String owner, @NotNull String name, @NotNull String descriptor) {
		return this.is(Type.getObjectType(owner), name, Type.getType(descriptor));
	}
	
	public boolean is(@NotNull Type owner, @NotNull String name, @NotNull Type descriptor) {
		return this.owner.equals(owner) && this.name.equals(name) && this.type.equals(descriptor);
	}
	//endregion
	
	//region Object overrides
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Field field)) return false;
		
		if (!this.owner.equals(field.owner)) return false;
		if (!this.name.equals(field.name)) return false;
		if (!this.type.equals(field.type)) return false;
		if (!Objects.equals(this.genericSignature, field.genericSignature)) return false;
		if (this.access != field.access) return false;
		if (!this.modifiers.equals(field.modifiers)) return false;
		if (!this.annotations.equals(field.annotations)) return false;
		return this.initialValue.equals(field.initialValue);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.owner, this.name, this.type, this.genericSignature, this.access, this.modifiers, this.annotations, this.initialValue);
	}
	
	@Override
	public String toString() {
		return this.getSourceSignature();
	}
	//endregion
	
	//region Builder
	public static class Builder {
		
		private final Set<TypeModifier> modifiers = EnumSet.noneOf(TypeModifier.class);
		private final Map<Type, Annotation> annotations = new HashMap<>();
		private final Mutable<Object> initialValue = new Mutable<>();
		private Type owner;
		private String name;
		private Type type;
		private String genericSignature;
		private TypeAccess access;
		
		//region Constructors
		private Builder(@NotNull Type owner) {
			this.owner = owner;
		}
		
		private Builder(@NotNull Type owner, @NotNull String name, @NotNull Type type) {
			this.owner = owner;
			this.name = name;
			this.type = type;
		}
		
		private Builder(@NotNull Type owner, @NotNull String name, @NotNull Type type, @NotNull TypeAccess access) {
			this.owner = owner;
			this.name = name;
			this.type = type;
			this.access = access;
		}
		
		private Builder(@NotNull Field field) {
			this.owner = field.owner;
			this.name = field.name;
			this.type = field.type;
			this.genericSignature = field.genericSignature;
			this.access = field.access;
			this.modifiers.addAll(field.modifiers);
			this.annotations.putAll(field.annotations);
			this.initialValue.set(field.initialValue.get());
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
		
		public @NotNull Builder access(@NotNull TypeAccess access) {
			this.access = access;
			return this;
		}
		
		//region Modifiers
		public @NotNull Builder modifiers(@NotNull Set<TypeModifier> modifiers) {
			this.modifiers.clear();
			this.modifiers.addAll(modifiers);
			return this;
		}
		
		public @NotNull Builder clearModifiers() {
			this.modifiers.clear();
			return this;
		}
		
		public @NotNull Builder addModifier(@NotNull TypeModifier modifier) {
			this.modifiers.add(modifier);
			return this;
		}
		
		public @NotNull Builder removeModifier(@NotNull TypeModifier modifier) {
			this.modifiers.remove(modifier);
			return this;
		}
		//endregion
		
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
		
		public @NotNull Builder initialValue(@Nullable Object initialValue) {
			this.initialValue.set(initialValue);
			return this;
		}
		
		public @NotNull Field build() {
			return new Field(this.owner, this.name, this.type, this.genericSignature, this.access, this.modifiers, this.annotations, this.initialValue);
		}
	}
	//endregion
}
