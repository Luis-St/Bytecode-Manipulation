package net.luis.agent.preload.data;

import net.luis.agent.preload.type.*;
import org.jetbrains.annotations.*;
import org.objectweb.asm.Type;

import java.util.*;

/**
 *
 * @author Luis-St
 *
 */

public record ClassData(@NotNull String name, @NotNull Type type, String signature, @NotNull TypeAccess access, @NotNull ClassType classType, @NotNull Set<TypeModifier> modifiers,
						@Nullable Type superType, @NotNull List<Type> permittedSubclasses, @NotNull List<Type> interfaces, @NotNull Map<Type, AnnotationData> annotations,
						@NotNull Map<String, RecordComponentData> recordComponents, @NotNull Map<String, FieldData> fields, @NotNull List<MethodData> methods, @NotNull List<InnerClassData> innerClasses) implements ASMData {
	
	//region Record components
	public @Unmodifiable @NotNull List<RecordComponentData> getRecordComponents() {
		return List.copyOf(this.recordComponents.values());
	}
	
	public @Nullable RecordComponentData getRecordComponent(@NotNull String name) {
		return this.recordComponents.get(name);
	}
	//endregion
	
	//region Fields
	public @Unmodifiable @NotNull List<FieldData> getFields() {
		return List.copyOf(this.fields.values());
	}
	
	public @Nullable FieldData getField(@NotNull String name) {
		return this.fields.get(name);
	}
	//endregion
	
	//region Methods
	public @Unmodifiable @NotNull List<MethodData> getMethods(@NotNull String name) {
		return this.methods.stream().filter(method -> method.name().equals(name)).toList();
	}
	
	public @Nullable MethodData getMethod(@NotNull String name, @NotNull Type type) {
		return this.methods.stream().filter(method -> method.name().equals(name) && method.is(type)).findFirst().orElse(null);
	}
	
	public @Unmodifiable @NotNull List<ParameterData> getParameters() {
		return this.methods.stream().flatMap(method -> method.parameters().stream()).toList();
	}
	//endregion
}
