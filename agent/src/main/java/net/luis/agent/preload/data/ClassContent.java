package net.luis.agent.preload.data;

import org.jetbrains.annotations.*;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.Map;

/**
 *
 * @author Luis-St
 *
 */

public record ClassContent(@NotNull Map</*Record Component Name*/String, RecordComponentData> recordComponents, @NotNull Map</*Field Name*/String, FieldData> fields, @NotNull List<MethodData> methods) {
	
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
		return this.methods.stream().filter(method -> method.name().equals(name) && method.type().equals(type)).findFirst().orElse(null);
	}
	//endregion
}
