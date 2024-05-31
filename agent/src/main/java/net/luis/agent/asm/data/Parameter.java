package net.luis.agent.asm.data;

import net.luis.agent.asm.ASMUtils;
import net.luis.agent.asm.Types;
import net.luis.agent.asm.type.TypeAccess;
import net.luis.agent.asm.type.TypeModifier;
import net.luis.agent.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.util.*;

/**
 *
 * @author Luis-St
 *
 */

public class Parameter implements ASMData {
	
	private final Method owner;
	private final String name;
	private final Type type;
	private final int index;
	private final Set<TypeModifier> modifiers;
	private final Map<Type, Annotation> annotations;
	
	private Parameter(@NotNull Method owner, @NotNull String name, @NotNull Type type, int index, @NotNull Set<TypeModifier> modifiers, @NotNull Map<Type, Annotation> annotations) {
		this.owner = Objects.requireNonNull(owner);
		this.name = Objects.requireNonNull(name);
		this.type = Objects.requireNonNull(type);
		this.index = index;
		this.modifiers = Objects.requireNonNull(modifiers);
		this.annotations = Objects.requireNonNull(annotations);
	}
	
	public static @NotNull Builder builder(@NotNull Method owner) {
		return new Builder(owner);
	}
	
	public static @NotNull Builder builder(@NotNull Method owner, @NotNull String name, @NotNull Type type) {
		return new Builder(owner, name, type);
	}
	
	public static @NotNull Builder builder(@NotNull Method owner, @NotNull String name, @NotNull Type type, int index) {
		return new Builder(owner, name, type, index);
	}
	
	public static @NotNull Builder builder(@NotNull Parameter parameter) {
		return new Builder(parameter);
	}
	
	//region Getters
	public @NotNull Method getOwner() {
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
		return null;
	}
	
	public int getIndex() {
		return this.index;
	}
	
	@Override
	public @NotNull TypeAccess getAccess() {
		return TypeAccess.PUBLIC;
	}
	
	@Override
	public @NotNull Set<TypeModifier> getModifiers() {
		return this.modifiers;
	}
	
	@Override
	public @NotNull Map<Type, Annotation> getAnnotations() {
		return this.annotations;
	}
	//endregion
	
	//region Functional getters
	public boolean isNamed() {
		return !this.name.equals("arg" + this.index);
	}
	
	public int getLoadIndex() {
		return this.index + (this.owner.is(TypeModifier.STATIC) ? 0 : 1);
	}
	
	@Override
	public @NotNull String getSourceSignature() {
		return this.owner.getOwner().getClassName() + "#" + this.owner.getName() + "#" + this.name + " : " + this.type.getClassName();
	}
	
	public @NotNull String getMessageName() {
		if (this.isNamed()) {
			return Utils.capitalize(Utils.getSeparated(this.name));
		}
		return Utils.capitalize(Utils.getSeparated(Types.getSimpleName(this.type))) + " (parameter #" + this.index + ")";
	}
	//endregion
	
	//region Object overrides
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Parameter that)) return false;
		
		if (this.index != that.index) return false;
		if (!this.owner.getFullSignature().equals(that.owner.getFullSignature())) return false;
		if (!this.name.equals(that.name)) return false;
		if (!this.type.equals(that.type)) return false;
		if (!this.modifiers.equals(that.modifiers)) return false;
		return this.annotations.equals(that.annotations);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.owner.getFullSignature(), this.name, this.type, this.index, this.modifiers, this.annotations);
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
		private Method owner;
		private String name;
		private Type type;
		private int index;
		
		//region Constructors
		private Builder(@NotNull Method owner) {
			this.owner = owner;
		}
		
		private Builder(@NotNull Method owner, @NotNull String name, @NotNull Type type) {
			this.owner = owner;
			this.name = name;
			this.type = type;
		}
		
		private Builder(@NotNull Method owner, @NotNull String name, @NotNull Type type, int index) {
			this.owner = owner;
			this.name = name;
			this.type = type;
			this.index = index;
		}
		
		private Builder(@NotNull Parameter parameter) {
			this.owner = parameter.owner;
			this.name = parameter.name;
			this.type = parameter.type;
			this.index = parameter.index;
			this.modifiers.addAll(parameter.modifiers);
			this.annotations.putAll(parameter.annotations);
		}
		//endregion
		
		public @NotNull Builder owner(@NotNull Method owner) {
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
		
		public @NotNull Builder index(int index) {
			this.index = index;
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
		
		public @NotNull Parameter build() {
			return new Parameter(this.owner, this.name, this.type, this.index, this.modifiers, this.annotations);
		}
	}
	//endregion
}
