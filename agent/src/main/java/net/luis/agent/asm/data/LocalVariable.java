package net.luis.agent.asm.data;

import net.luis.agent.asm.Types;
import net.luis.agent.asm.type.*;
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

public class LocalVariable implements ASMData {
	
	private final Method owner;
	private final int index;
	private final String name;
	private final Type type;
	private final String genericSignature;
	private final Map<Type, Annotation> annotations;
	private Scope scope;
	
	private LocalVariable(@NotNull Method owner, int index, @NotNull String name, @NotNull Type type, @Nullable String genericSignature, @NotNull LocalVariable.Scope scope, @NotNull Map<Type, Annotation> annotations) {
		this.owner = Objects.requireNonNull(owner);
		this.index = index;
		this.name = Objects.requireNonNull(name);
		this.type = Objects.requireNonNull(type);
		this.genericSignature = genericSignature == null ? "" : genericSignature;
		this.scope = Objects.requireNonNull(scope);
		this.annotations = Objects.requireNonNull(annotations);
	}
	
	public static @NotNull Builder builder(@NotNull Method owner) {
		return new Builder(owner);
	}
	
	public static @NotNull Builder builder(@NotNull Method owner, int index) {
		return new Builder(owner, index);
	}
	
	public static @NotNull Builder builder(@NotNull Method owner, int index, @NotNull String name, @NotNull Type type) {
		return new Builder(owner, index, name, type);
	}
	
	public static @NotNull Builder builder(@NotNull LocalVariable localVariable) {
		return new Builder(localVariable);
	}
	
	//region Getters
	public @NotNull Method getOwner() {
		return this.owner;
	}
	
	public int getIndex() {
		return this.index;
	}
	
	public @NotNull String getName() {
		return this.name;
	}
	
	public @NotNull Type getType() {
		return this.type;
	}
	
	@Override
	public @NotNull String getSignature(@NotNull SignatureType type) {
		return switch (type) {
			case GENERIC -> this.genericSignature;
			case DEBUG -> this.owner.getOwner().getClassName() + "()#" + this.owner.getName() + "#" + this.name + "(#" + this.index + ") (Scope " + this.scope.start + " - " + this.scope.end + ") : " + this.type.getClassName();
			case SOURCE -> this.owner.getSignature(SignatureType.SOURCE) + "()#" + this.name + " (#" + this.index + ") (Scope " + this.scope.start + " - " + this.scope.end + ")";
			default -> "";
		};
	}
	
	@Override
	public @NotNull TypeAccess getAccess() {
		return TypeAccess.PUBLIC;
	}
	
	@Override
	public @NotNull Set<TypeModifier> getModifiers() {
		return EnumSet.noneOf(TypeModifier.class);
	}
	
	public int getStart() {
		return this.scope.start;
	}
	
	public int getEnd() {
		return this.scope.end;
	}
	
	public @NotNull Map<Type, Annotation> getAnnotations() {
		return this.annotations;
	}
	//endregion
	
	//region Functional getters
	public @NotNull String getMessageName() {
		return Utils.capitalize(Utils.getSeparated(this.name));
	}
	
	public boolean isInScope(int scope) {
		return this.scope.start <= scope && scope < this.scope.end;
	}
	
	public boolean isScope(int start, int end) {
		return this.scope.start == start && this.scope.end == end;
	}
	
	public void updateScope(@NotNull Set</*Insert After Index*/Integer> inserts) {
		this.scope = this.scope.update(inserts);
	}
	//endregion
	
	//region Object overrides
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof LocalVariable that)) return false;
		
		if (this.index != that.index) return false;
		if (!this.owner.getSignature(SignatureType.FULL).equals(that.owner.getSignature(SignatureType.FULL))) return false;
		if (!this.name.equals(that.name)) return false;
		if (!this.type.equals(that.type)) return false;
		if (!Objects.equals(this.genericSignature, that.genericSignature)) return false;
		if (!this.annotations.equals(that.annotations)) return false;
		return Objects.equals(this.scope, that.scope);
	}
	
	@Override
	@SuppressWarnings("NonFinalFieldReferencedInHashCode")
	public int hashCode() {
		return Objects.hash(this.owner.getSignature(SignatureType.FULL), this.index, this.name, this.type, this.genericSignature, this.annotations, this.scope);
	}
	
	@Override
	public String toString() {
		return this.getSignature(SignatureType.SOURCE);
	}
	//endregion
	
	//region Builder
	public static class Builder {
		
		private final Map<Type, Annotation> annotations = new HashMap<>();
		private Method owner;
		private int index;
		private String name;
		private Type type;
		private String genericSignature;
		private Scope scope;
		
		//region Constructors
		private Builder(@NotNull Method owner) {
			this.owner = owner;
		}
		
		private Builder(@NotNull Method owner, int index) {
			this.owner = owner;
			this.index = index;
		}
		
		private Builder(@NotNull Method owner, int index, @NotNull String name, @NotNull Type type) {
			this.owner = owner;
			this.index = index;
			this.name = name;
			this.type = type;
		}
		
		private Builder(@NotNull LocalVariable localVariable) {
			this.owner = localVariable.owner;
			this.index = localVariable.index;
			this.name = localVariable.name;
			this.type = localVariable.type;
			this.genericSignature = localVariable.genericSignature;
			this.scope = localVariable.scope;
			this.annotations.putAll(localVariable.annotations);
		}
		//endregion
		
		public @NotNull Builder owner(@NotNull Method owner) {
			this.owner = owner;
			return this;
		}
		
		public @NotNull Builder index(int index) {
			this.index = index;
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
		
		public @NotNull Builder bounds(int start, int end) {
			this.scope = new Scope(start, end);
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
		
		public @NotNull LocalVariable build() {
			return new LocalVariable(this.owner, this.index, this.name, this.type, this.genericSignature, this.scope, this.annotations);
		}
	}
	//endregion
	
	//region Scope
	private record Scope(int start, int end) {
		
		public @NotNull Scope update(@NotNull Set</*Insert After Index*/Integer> inserts) {
			if (inserts.isEmpty()) {
				return this;
			}
			int newStart = this.start;
			int newEnd = this.end;
			
			for (int insert : inserts) {
				if (insert >= this.end) {
					continue;
				}
				if (this.start > insert) {
					newStart++;
				}
				newEnd++;
			}
			return new Scope(newStart, newEnd);
		}
	}
	//endregion
}
