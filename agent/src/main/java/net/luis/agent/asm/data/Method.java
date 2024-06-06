package net.luis.agent.asm.data;

import net.luis.agent.asm.Types;
import net.luis.agent.asm.type.*;
import net.luis.agent.util.Mutable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * @author Luis-St
 *
 */

public class Method implements ASMData {
	
	private final Type owner;
	private final String name;
	private final Type type;
	private final String genericSignature;
	private final TypeAccess access;
	private final MethodType methodType;
	private final Set<TypeModifier> modifiers;
	private final Map<Type, Annotation> annotations;
	private final Map<Integer, Parameter> parameters;
	private final List<Type> exceptions;
	private final List<LocalVariable> locals;
	private final Mutable<Object> annotationDefault;
	
	private Method(@NotNull Type owner, @NotNull String name, @NotNull Type type, @Nullable String genericSignature, @NotNull TypeAccess access, @NotNull Set<TypeModifier> modifiers,
				   @NotNull Map<Type, Annotation> annotations, @NotNull Map<Integer, Parameter> parameters, @NotNull List<Type> exceptions, @NotNull List<LocalVariable> locals, @NotNull Mutable<Object> annotationDefault) {
		MethodType methodType = MethodType.fromName(name);
		this.owner = Objects.requireNonNull(owner);
		this.name = Objects.requireNonNull(name);
		this.type = Objects.requireNonNull(type);
		this.genericSignature = genericSignature;
		this.access = methodType == MethodType.STATIC_INITIALIZER ? TypeAccess.PACKAGE : Objects.requireNonNull(access);
		this.methodType = Objects.requireNonNull(methodType);
		this.modifiers = Objects.requireNonNull(modifiers);
		this.annotations = Objects.requireNonNull(annotations);
		this.parameters = Objects.requireNonNull(parameters);
		this.exceptions = Objects.requireNonNull(exceptions);
		this.locals = Objects.requireNonNull(locals);
		this.annotationDefault = Objects.requireNonNull(annotationDefault);
	}
	
	public static @NotNull Method of(@NotNull Type owner, @NotNull String name, @NotNull Type type, @Nullable String genericSignature, int access) {
		return builder(owner, name, type).genericSignature(genericSignature).access(TypeAccess.fromAccess(access)).modifiers(TypeModifier.fromMethodAccess(access)).build();
	}
	
	public static @NotNull Builder builder(@NotNull Type owner) {
		return new Builder(owner);
	}
	
	public static @NotNull Builder builder(@NotNull Type owner, @NotNull String name) {
		return new Builder(owner, name);
	}
	
	public static @NotNull Builder builder(@NotNull Type owner, @NotNull String name, @NotNull Type type) {
		return new Builder(owner, name, type);
	}
	
	public static @NotNull Builder builder(@NotNull Method method) {
		return new Builder(method);
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
		return this.access;
	}
	
	public @NotNull MethodType getMethodType() {
		return this.methodType;
	}
	
	@Override
	public @NotNull Set<TypeModifier> getModifiers() {
		return this.modifiers;
	}
	
	@Override
	public @NotNull Map<Type, Annotation> getAnnotations() {
		return this.annotations;
	}
	
	public @NotNull Map<Integer, Parameter> getParameters() {
		return this.parameters;
	}
	
	public @NotNull List<Type> getExceptions() {
		return this.exceptions;
	}
	
	public @NotNull List<LocalVariable> getLocals() {
		return this.locals;
	}
	
	public @NotNull Mutable<Object> getAnnotationDefault() {
		return this.annotationDefault;
	}
	//endregion
	
	//region Functional getters
	public @NotNull Parameter getParameter(int index) {
		return this.parameters.get(index);
	}
	
	public @NotNull List<LocalVariable> getLocals(int localIndex) {
		return this.locals.stream().filter(local -> local.getIndex() == localIndex).collect(Collectors.toList());
	}
	
	public @Nullable LocalVariable getLocal(int localIndex, int labelIndex) {
		return this.locals.stream().filter(local -> local.getIndex() == localIndex && local.isInBounds(labelIndex)).findFirst().orElse(null);
	}
	
	public @Nullable LocalVariable getLocal(int localIndex, int start, int end) {
		return this.locals.stream().filter(local -> local.getIndex() == localIndex && local.isBoundary(start, end)).findFirst().orElse(null);
	}
	
	@Override
	public @NotNull String getSourceSignature(boolean full) {
		if (full) {
			return this.owner.getClassName() + "#" + this.name + Arrays.stream(this.type.getArgumentTypes()).map(Types::getSimpleName).collect(Collectors.joining(", ", "(", ")"));
		}
		return Types.getSimpleName(this.owner) + "#" + this.name;
	}
	
	public @NotNull Type getReturnType() {
		return this.type.getReturnType();
	}
	
	public int getParameterCount() {
		return this.parameters.size();
	}
	
	public int getExceptionCount() {
		return this.exceptions.size();
	}
	
	public int getLocalCount() {
		return this.locals.size();
	}
	
	public boolean is(MethodType type) {
		return this.methodType == type;
	}
	
	public boolean is(@NotNull String owner, @NotNull String name, @NotNull String descriptor) {
		return this.is(Type.getObjectType(owner), name, Type.getType(descriptor));
	}
	
	public boolean is(@NotNull Type owner, @NotNull String name, @NotNull Type descriptor) {
		return this.owner.equals(owner) && this.name.equals(name) && this.type.equals(descriptor);
	}
	
	public boolean isImplementedMethod() {
		return this.is(MethodType.METHOD) && !this.is(TypeModifier.ABSTRACT);
	}
	
	public boolean returns(@NotNull Type type) {
		return this.getReturnType().equals(type);
	}
	
	public boolean returnsAny(@NotNull Type... types) {
		return Arrays.stream(types).anyMatch(this::returns);
	}
	
	public boolean isLocal(int index) {
		return this.is(TypeModifier.STATIC) ? index >= this.parameters.size() : index > this.parameters.size();
	}
	
	public void updateLocalBounds(@NotNull Set</*Insert After Index*/Integer> inserts) {
		this.locals.forEach(local -> local.updateBounds(inserts));
	}
	//endregion
	
	//region Object overrides
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Method that)) return false;
		
		if (!this.owner.equals(that.owner)) return false;
		if (!this.name.equals(that.name)) return false;
		if (!this.type.equals(that.type)) return false;
		if (!Objects.equals(this.genericSignature, that.genericSignature)) return false;
		if (this.access != that.access) return false;
		if (this.methodType != that.methodType) return false;
		if (!this.modifiers.equals(that.modifiers)) return false;
		if (!this.annotations.equals(that.annotations)) return false;
		if (!this.parameters.equals(that.parameters)) return false;
		if (!this.exceptions.equals(that.exceptions)) return false;
		if (!this.locals.equals(that.locals)) return false;
		return this.annotationDefault.equals(that.annotationDefault);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.owner, this.name, this.type, this.genericSignature, this.access, this.methodType, this.modifiers, this.annotations, this.parameters, this.exceptions, this.locals, this.annotationDefault);
	}
	
	@Override
	public String toString() {
		return this.getSourceSignature(true);
	}
	//endregion
	
	//region Builder
	public static class Builder {
		
		private final Set<TypeModifier> modifiers = EnumSet.noneOf(TypeModifier.class);
		private final Map<Type, Annotation> annotations = new HashMap<>();
		private final Map<Integer, Parameter> parameters = new HashMap<>();
		private final List<Type> exceptions = new ArrayList<>();
		private final List<LocalVariable> locals = new ArrayList<>();
		private final Mutable<Object> annotationDefault = new Mutable<>();
		private Type owner;
		private String name;
		private Type type;
		private String genericSignature;
		private TypeAccess access;
		
		//region Constructors
		private Builder(@NotNull Type owner) {
			this.owner = owner;
		}
		
		private Builder(@NotNull Type owner, @NotNull String name) {
			this.owner = owner;
			this.name = name;
		}
		
		private Builder(@NotNull Type owner, @NotNull String name, @NotNull Type type) {
			this.owner = owner;
			this.name = name;
			this.type = type;
		}
		
		private Builder(@NotNull Method method) {
			this.owner = method.owner;
			this.name = method.name;
			this.type = method.type;
			this.genericSignature = method.genericSignature;
			this.access = method.access;
			this.modifiers.addAll(method.modifiers);
			this.annotations.putAll(method.annotations);
			this.parameters.putAll(method.parameters);
			this.exceptions.addAll(method.exceptions);
			this.locals.addAll(method.locals);
			this.annotationDefault.set(method.annotationDefault.get());
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
		
		//region Parameters
		public @NotNull Builder parameters(@NotNull Map<Integer, Parameter> parameters) {
			this.parameters.clear();
			this.parameters.putAll(parameters);
			return this;
		}
		
		public @NotNull Builder clearParameters() {
			this.parameters.clear();
			return this;
		}
		
		public @NotNull Builder addParameter(int index, @NotNull Parameter parameter) {
			this.parameters.put(index, parameter);
			return this;
		}
		
		public @NotNull Builder removeParameter(int index) {
			this.parameters.remove(index);
			return this;
		}
		//endregion
		
		//region Exceptions
		public @NotNull Builder exceptions(@NotNull List<Type> exceptions) {
			this.exceptions.clear();
			this.exceptions.addAll(exceptions);
			return this;
		}
		
		public @NotNull Builder clearExceptions() {
			this.exceptions.clear();
			return this;
		}
		
		public @NotNull Builder addException(@NotNull Type exception) {
			this.exceptions.add(exception);
			return this;
		}
		
		public @NotNull Builder removeException(@NotNull Type exception) {
			this.exceptions.remove(exception);
			return this;
		}
		//endregion
		
		//region Locals
		public @NotNull Builder locals(@NotNull List<LocalVariable> locals) {
			this.locals.clear();
			this.locals.addAll(locals);
			return this;
		}
		
		public @NotNull Builder clearLocals() {
			this.locals.clear();
			return this;
		}
		
		public @NotNull Builder addLocal(@NotNull LocalVariable local) {
			this.locals.add(local);
			return this;
		}
		
		public @NotNull Builder removeLocal(@NotNull LocalVariable local) {
			this.locals.remove(local);
			return this;
		}
		//endregion
		
		public @NotNull Builder annotationDefault(@NotNull Object annotationDefault) {
			this.annotationDefault.set(annotationDefault);
			return this;
		}
		
		public @NotNull Method build() {
			return new Method(this.owner, this.name, this.type, this.genericSignature, this.access, this.modifiers, this.annotations, this.parameters, this.exceptions, this.locals, this.annotationDefault);
		}
	}
	//endregion
}
