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

public class Class implements ASMData {
	
	private final String name;
	private final Type type;
	private final String genericSignature;
	private final TypeAccess access;
	private final ClassType classType;
	private final Set<TypeModifier> modifiers;
	private final Type superType;
	private final List<Type> permittedSubclasses;
	private final List<Type> interfaces;
	private final Map<Type, Annotation> annotations;
	private final Map<String, RecordComponent> recordComponents;
	private final Map<String, Field> fields;
	private final Map<String, Method> methods;
	private final List<InnerClass> innerClasses;
	
	private Class(@NotNull String name, @NotNull Type type, @Nullable String genericSignature, @NotNull TypeAccess access, @NotNull ClassType classType, @NotNull Set<TypeModifier> modifiers,
				  @NotNull Type superType, @NotNull List<Type> permittedSubclasses, @NotNull List<Type> interfaces, @NotNull Map<Type, Annotation> annotations,
				  @NotNull Map<String, RecordComponent> recordComponents, @NotNull Map<String, Field> fields, @NotNull Map<String, Method> methods, @NotNull List<InnerClass> innerClasses) {
		this.name = Objects.requireNonNull(name);
		this.type = Objects.requireNonNull(type);
		this.genericSignature = genericSignature == null ? "" : genericSignature;
		this.access = Objects.requireNonNull(access);
		this.classType = Objects.requireNonNull(classType);
		this.modifiers = Objects.requireNonNull(modifiers);
		this.superType = Objects.requireNonNull(superType);
		this.permittedSubclasses = Objects.requireNonNull(permittedSubclasses);
		this.interfaces = Objects.requireNonNull(interfaces);
		this.annotations = Objects.requireNonNull(annotations);
		this.recordComponents = Objects.requireNonNull(recordComponents);
		this.fields = Objects.requireNonNull(fields);
		this.methods = Objects.requireNonNull(methods);
		this.innerClasses = Objects.requireNonNull(innerClasses);
	}
	
	public static @NotNull Builder builder() {
		return new Builder();
	}
	
	public static @NotNull Builder builder(@NotNull String name, @NotNull Type type) {
		return new Builder(name, type);
	}
	
	public static @NotNull Builder builder(@NotNull String name, @NotNull Type type, @NotNull ClassType classType) {
		return new Builder(name, type, classType);
	}
	
	public static @NotNull Builder builder(@NotNull Class clazz) {
		return new Builder(clazz);
	}
	
	//region Getters
	@Override
	public @NotNull String getName() {
		return this.name;
	}
	
	@Override
	public @NotNull Type getType() {
		return this.type;
	}
	
	@Override
	public @NotNull String getSignature(@NotNull SignatureType type) {
		return switch (type) {
			case GENERIC -> this.genericSignature;
			case FULL -> this.type.getDescriptor();
			case DEBUG -> this.type.getClassName();
			case SOURCE -> Types.getSimpleName(this.type);
			default -> "";
		};
	}
	
	@Override
	public @NotNull TypeAccess getAccess() {
		return this.access;
	}
	
	public @NotNull ClassType getClassType() {
		return this.classType;
	}
	
	@Override
	public @NotNull Set<TypeModifier> getModifiers() {
		return this.modifiers;
	}
	
	public @NotNull Type getSuperType() {
		return this.superType;
	}
	
	public @NotNull List<Type> getPermittedSubclasses() {
		return this.permittedSubclasses;
	}
	
	public @NotNull List<Type> getInterfaces() {
		return this.interfaces;
	}
	
	@Override
	public @NotNull Map<Type, Annotation> getAnnotations() {
		return this.annotations;
	}
	
	public @NotNull Map<String, RecordComponent> getRecordComponents() {
		return this.recordComponents;
	}
	
	public @NotNull Map<String, Field> getFields() {
		return this.fields;
	}
	
	public @NotNull Map<String, Method> getMethods() {
		return this.methods;
	}
	
	public @NotNull List<InnerClass> getInnerClasses() {
		return this.innerClasses;
	}
	//endregion
	
	//region Functional getters
	public @Nullable RecordComponent getRecordComponent(@Nullable String name) {
		return this.recordComponents.get(name);
	}
	
	public @Nullable Field getField(@Nullable String name) {
		return this.fields.get(name);
	}
	
	public @Nullable Method getMethod(@Nullable String fullSignature) {
		return this.methods.get(fullSignature);
	}
	
	public @NotNull List<Method> getMethods(@Nullable String name) {
		return this.methods.values().stream().filter(method -> method.getName().equals(name)).toList();
	}
	
	public @NotNull List<Parameter> getParameters() {
		return this.methods.values().stream().flatMap(method -> method.getParameters().values().stream()).toList();
	}
	
	public @NotNull List<LocalVariable> getLocals() {
		return this.methods.values().stream().flatMap(method -> method.getLocals().stream()).toList();
	}
	
	public boolean is(@NotNull ClassType type) {
		return this.classType == type;
	}
	//endregion
	
	//region Object overrides
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Class data)) return false;
		
		if (!this.name.equals(data.name)) return false;
		if (!this.type.equals(data.type)) return false;
		if (!Objects.equals(this.genericSignature, data.genericSignature)) return false;
		if (this.access != data.access) return false;
		if (this.classType != data.classType) return false;
		if (!this.modifiers.equals(data.modifiers)) return false;
		if (!this.superType.equals(data.superType)) return false;
		if (!this.permittedSubclasses.equals(data.permittedSubclasses)) return false;
		if (!this.interfaces.equals(data.interfaces)) return false;
		if (!this.annotations.equals(data.annotations)) return false;
		if (!this.recordComponents.equals(data.recordComponents)) return false;
		if (!this.fields.equals(data.fields)) return false;
		if (!this.methods.equals(data.methods)) return false;
		return this.innerClasses.equals(data.innerClasses);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.name, this.type, this.genericSignature, this.access, this.classType, this.modifiers, this.superType, this.permittedSubclasses, this.interfaces, this.annotations, this.recordComponents, this.fields,
			this.methods, this.innerClasses);
	}
	
	@Override
	public String toString() {
		return this.getSignature(SignatureType.DEBUG);
	}
	//endregion
	
	//region Builder
	public static class Builder {
		
		private final Set<TypeModifier> modifiers = EnumSet.noneOf(TypeModifier.class);
		private final List<Type> permittedSubclasses = new ArrayList<>();
		private final List<Type> interfaces = new ArrayList<>();
		private final Map<Type, Annotation> annotations = new HashMap<>();
		private final Map<String, RecordComponent> recordComponents = new HashMap<>();
		private final Map<String, Field> fields = new HashMap<>();
		private final Map<String, Method> methods = new HashMap<>();
		private final List<InnerClass> innerClasses = new ArrayList<>();
		private String name;
		private Type type;
		private String genericSignature;
		private TypeAccess access = TypeAccess.PUBLIC;
		private ClassType classType = ClassType.CLASS;
		private Type superType;
		
		//region Constructors
		public Builder() {}
		
		public Builder(@NotNull String name, @NotNull Type type) {
			this.name = name;
			this.type = type;
		}
		
		public Builder(@NotNull String name, @NotNull Type type, @NotNull ClassType classType) {
			this.name = name;
			this.type = type;
			this.classType = classType;
		}
		
		public Builder(@NotNull Class clazz) {
			this.name = clazz.name;
			this.type = clazz.type;
			this.genericSignature = clazz.genericSignature;
			this.access = clazz.access;
			this.classType = clazz.classType;
			this.modifiers.addAll(clazz.modifiers);
			this.superType = clazz.superType;
			this.permittedSubclasses.addAll(clazz.permittedSubclasses);
			this.interfaces.addAll(clazz.interfaces);
			this.annotations.putAll(clazz.annotations);
			this.recordComponents.putAll(clazz.recordComponents);
			this.fields.putAll(clazz.fields);
			this.methods.putAll(clazz.methods);
			this.innerClasses.addAll(clazz.innerClasses);
		}
		//endregion
		
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
		
		public @NotNull Builder classType(@NotNull ClassType classType) {
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
		
		public @NotNull Builder superType(@NotNull Type superType) {
			this.superType = superType;
			return this;
		}
		
		//region Permitted subclasses
		public @NotNull Builder permittedSubclasses(@NotNull List<Type> permittedSubclasses) {
			this.permittedSubclasses.clear();
			this.permittedSubclasses.addAll(permittedSubclasses);
			return this;
		}
		
		public @NotNull Builder clearPermittedSubclasses() {
			this.permittedSubclasses.clear();
			return this;
		}
		
		public @NotNull Builder addPermittedSubclass(@NotNull Type permittedSubclass) {
			this.permittedSubclasses.add(permittedSubclass);
			return this;
		}
		
		public @NotNull Builder removePermittedSubclass(@NotNull Type permittedSubclass) {
			this.permittedSubclasses.remove(permittedSubclass);
			return this;
		}
		//endregion
		
		//region Interfaces
		public @NotNull Builder interfaces(@NotNull List<Type> interfaces) {
			this.interfaces.clear();
			this.interfaces.addAll(interfaces);
			return this;
		}
		
		public @NotNull Builder clearInterfaces() {
			this.interfaces.clear();
			return this;
		}
		
		public @NotNull Builder addInterface(@NotNull Type interfaceType) {
			this.interfaces.add(interfaceType);
			return this;
		}
		
		public @NotNull Builder removeInterface(@NotNull Type interfaceType) {
			this.interfaces.remove(interfaceType);
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
		
		//region Record components
		public @NotNull Builder recordComponents(@NotNull Map<String, RecordComponent> recordComponents) {
			this.recordComponents.clear();
			this.recordComponents.putAll(recordComponents);
			return this;
		}
		
		public @NotNull Builder clearRecordComponents() {
			this.recordComponents.clear();
			return this;
		}
		
		public @NotNull Builder addRecordComponent(@NotNull String name, @NotNull RecordComponent recordComponent) {
			this.recordComponents.put(name, recordComponent);
			return this;
		}
		
		public @NotNull Builder removeRecordComponent(@NotNull String name) {
			this.recordComponents.remove(name);
			return this;
		}
		//endregion
		
		//region Fields
		public @NotNull Builder fields(@NotNull Map<String, Field> fields) {
			this.fields.clear();
			this.fields.putAll(fields);
			return this;
		}
		
		public @NotNull Builder clearFields() {
			this.fields.clear();
			return this;
		}
		
		public @NotNull Builder addField(@NotNull String name, @NotNull Field field) {
			this.fields.put(name, field);
			return this;
		}
		
		public @NotNull Builder removeField(@NotNull String name) {
			this.fields.remove(name);
			return this;
		}
		//endregion
		
		//region Methods
		public @NotNull Builder methods(@NotNull Map<String, Method> methods) {
			this.methods.clear();
			this.methods.putAll(methods);
			return this;
		}
		
		public @NotNull Builder clearMethods() {
			this.methods.clear();
			return this;
		}
		
		public @NotNull Builder addMethod(@NotNull String fullSignature, @NotNull Method method) {
			this.methods.put(fullSignature, method);
			return this;
		}
		
		public @NotNull Builder removeMethod(@NotNull String fullSignature) {
			this.methods.remove(fullSignature);
			return this;
		}
		//endregion
		
		//region Inner classes
		public @NotNull Builder innerClasses(@NotNull List<InnerClass> innerClasses) {
			this.innerClasses.clear();
			this.innerClasses.addAll(innerClasses);
			return this;
		}
		
		public @NotNull Builder clearInnerClasses() {
			this.innerClasses.clear();
			return this;
		}
		
		public @NotNull Builder addInnerClass(@NotNull InnerClass innerClass) {
			this.innerClasses.add(innerClass);
			return this;
		}
		
		public @NotNull Builder removeInnerClass(@NotNull InnerClass innerClass) {
			this.innerClasses.remove(innerClass);
			return this;
		}
		//endregion
		
		public @NotNull Class build() {
			return new Class(this.name, this.type, this.genericSignature, this.access, this.classType, this.modifiers, this.superType, this.permittedSubclasses, this.interfaces, this.annotations, this.recordComponents, this.fields,
				this.methods, this.innerClasses);
		}
	}
	//endregion
}
