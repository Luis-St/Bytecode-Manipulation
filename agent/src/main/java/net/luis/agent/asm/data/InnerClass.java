package net.luis.agent.asm.data;

import net.luis.agent.asm.Types;
import net.luis.agent.asm.type.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.util.*;

/**
 *
 * @author Luis-St
 *
 */

public class InnerClass {
	
	private final Type owner;
	private final String name;
	private final Type type;
	private final TypeAccess access;
	private final InnerClassType classType;
	private final Set<TypeModifier> modifiers;
	
	private InnerClass(@NotNull Type owner, @Nullable String name, @NotNull Type type, @NotNull TypeAccess access, @NotNull InnerClassType classType, @NotNull Set<TypeModifier> modifiers) {
		this.owner = Objects.requireNonNull(owner);
		this.name = name;
		this.type = Objects.requireNonNull(type);
		this.access = classType == InnerClassType.INNER ? Objects.requireNonNull(access) : TypeAccess.PRIVATE;
		this.classType = Objects.requireNonNull(classType);
		this.modifiers = classType == InnerClassType.INNER ? Objects.requireNonNull(modifiers) : EnumSet.noneOf(TypeModifier.class);
	}
	
	public static @NotNull InnerClass of(@NotNull Type owner, @Nullable String name, @NotNull Type type, @Nullable String outerName, @Nullable String innerName, int access) {
		return builder(owner, name, type).classType(InnerClassType.fromNames(outerName, innerName)).access(TypeAccess.fromAccess(access)).modifiers(TypeModifier.fromAccess(access)).build();
	}
	
	public static @NotNull Builder builder(@NotNull Type owner) {
		return new Builder(owner);
	}
	
	public static @NotNull Builder builder(@NotNull Type owner, @Nullable String name, @NotNull Type type) {
		return new Builder(owner, name, type);
	}
	
	public static @NotNull Builder builder(@NotNull InnerClass innerClass) {
		return new Builder(innerClass);
	}
	
	//region Getters
	public @NotNull Type getOwner() {
		return this.owner;
	}
	
	public @Nullable String getName() {
		return this.name;
	}
	
	public @NotNull Type getType() {
		return this.type;
	}
	
	public @NotNull TypeAccess getAccess() {
		return this.access;
	}
	
	public @NotNull InnerClassType getClassType() {
		return this.classType;
	}
	
	public @NotNull Set<TypeModifier> getModifiers() {
		return this.modifiers;
	}
	//endregion
	
	//region Functional getters
	public @NotNull String getSourceSignature(boolean full) {
		return full ? this.type.getClassName() : Types.getSimpleName(this.type);
	}
	//endregion
	
	//region Object overrides
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof InnerClass that)) return false;
		
		if (!this.owner.equals(that.owner)) return false;
		if (!Objects.equals(this.name, that.name)) return false;
		if (!this.type.equals(that.type)) return false;
		if (this.access != that.access) return false;
		if (this.classType != that.classType) return false;
		return this.modifiers.equals(that.modifiers);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.owner, this.name, this.type, this.access, this.classType, this.modifiers);
	}
	
	@Override
	public String toString() {
		return this.getSourceSignature(true);
	}
	//endregion
	
	//region Builder
	public static class Builder {
		
		private final Set<TypeModifier> modifiers = EnumSet.noneOf(TypeModifier.class);
		private Type owner;
		private String name;
		private Type type;
		private TypeAccess access;
		private InnerClassType classType;
		
		//region Constructors
		private Builder(@NotNull Type owner) {
			this.owner = owner;
		}
		
		private Builder(@NotNull Type owner, @Nullable String name, @NotNull Type type) {
			this.owner = owner;
			this.name = name;
			this.type = type;
		}
		
		private Builder(@NotNull InnerClass innerClass) {
			this.owner = innerClass.owner;
			this.name = innerClass.name;
			this.type = innerClass.type;
			this.access = innerClass.access;
			this.classType = innerClass.classType;
			this.modifiers.addAll(innerClass.modifiers);
		}
		//endregion
		
		public @NotNull Builder owner(@NotNull Type owner) {
			this.owner = owner;
			return this;
		}
		
		public @NotNull Builder name(@Nullable String name) {
			this.name = name;
			return this;
		}
		
		public @NotNull Builder type(@NotNull Type type) {
			this.type = type;
			return this;
		}
		
		public @NotNull Builder access(@NotNull TypeAccess access) {
			this.access = access;
			return this;
		}
		
		public @NotNull Builder classType(@NotNull InnerClassType classType) {
			this.classType = classType;
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
		
		public @NotNull InnerClass build() {
			return new InnerClass(this.owner, this.name, this.type, this.access, this.classType, this.modifiers);
		}
	}
	//endregion
}
