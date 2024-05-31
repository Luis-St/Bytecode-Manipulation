package net.luis.agent.util;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;

import static net.luis.agent.asm.Types.*;

/**
 *
 * @author Luis-St
 *
 */

public class DefaultStringFactory implements StringFactory {
	
	private static final Type OPTIONAL = Type.getType(Optional.class);
	
	public static final DefaultStringFactory INSTANCE = new DefaultStringFactory();
	private final Map<Type, Function<String, ?>> directFactories = new HashMap<>();
	private final Map<Type, Function<String, ?>> inheritanceFactories = new HashMap<>();
	private final Mutable<BiFunction<Type, String, ?>> arrayFactory = new Mutable<>(this::createArray);
	
	private DefaultStringFactory() {
		//region Factories
		this.directFactories.put(BOOLEAN, Boolean::parseBoolean);
		this.directFactories.put(convertToWrapper(BOOLEAN), Boolean::parseBoolean);
		this.directFactories.put(BYTE, value -> value.isBlank() ? 0 : Byte.parseByte(value));
		this.directFactories.put(convertToWrapper(BYTE), value -> value.isBlank() ? 0 : Byte.parseByte(value));
		this.directFactories.put(CHAR, value -> value.isBlank() ? '\0' : value.charAt(0));
		this.directFactories.put(convertToWrapper(CHAR), value -> value.isBlank() ? '\0' : value.charAt(0));
		this.directFactories.put(SHORT, value -> value.isBlank() ? 0 : Short.parseShort(value));
		this.directFactories.put(convertToWrapper(SHORT), value -> value.isBlank() ? 0 : Short.parseShort(value));
		this.directFactories.put(INT, value -> value.isBlank() ? 0 : Integer.parseInt(value));
		this.directFactories.put(convertToWrapper(INT), value -> value.isBlank() ? 0 : Integer.parseInt(value));
		this.directFactories.put(LONG, value -> value.isBlank() ? 0L : Long.parseLong(value));
		this.directFactories.put(convertToWrapper(LONG), value -> value.isBlank() ? 0L : Long.parseLong(value));
		this.directFactories.put(FLOAT, value -> value.isBlank() ? 0.0F : Float.parseFloat(value));
		this.directFactories.put(convertToWrapper(FLOAT), value -> value.isBlank() ? 0.0F : Float.parseFloat(value));
		this.directFactories.put(DOUBLE, value -> value.isBlank() ? 0.0D : Double.parseDouble(value));
		this.directFactories.put(convertToWrapper(DOUBLE), value -> value.isBlank() ? 0.0D : Double.parseDouble(value));
		this.directFactories.put(STRING, Function.identity());
		this.directFactories.put(OPTIONAL, value -> Optional.empty());
		this.inheritanceFactories.put(Type.getType(List.class), this::createList);
		this.inheritanceFactories.put(Type.getType(Set.class), this::createSet);
		this.inheritanceFactories.put(Type.getType(Map.class), this::createMap);
		//endregion
	}
	
	@Override
	public @NotNull Object create(@NotNull Type type, @NotNull String value) {
		value = value.strip();
		if (type.getSort() == Type.ARRAY) {
			return this.arrayFactory.get().apply(type, value);
		}
		Function<String, ?> factory = this.directFactories.get(type);
		if (factory != null) {
			return factory.apply(value);
		}
		Class<?> clazz = this.getClass(type);
		for (Map.Entry<Type, Function<String, ?>> entry : this.inheritanceFactories.entrySet()) {
			Class<?> target = this.getClass(entry.getKey());
			if (target.isAssignableFrom(clazz)) {
				return entry.getValue().apply(value);
			}
		}
		throw new IllegalArgumentException("No factory for type '" + type.getClassName() + "' in default string factory found");
	}
	
	//region Default factories
	private @NotNull Object createArray(@NotNull Type type, @NotNull String value) {
		if (!(value.isEmpty() || "[]".equals(value))) {
			throw new IllegalArgumentException("Invalid array value '" + value + "', default string factory only supports empty arrays (e.g. '' or '[]')");
		}
		Class<?> arrayType = this.getClass(type.getElementType());
		int[] dimensions = IntStream.range(0, type.getDimensions()).map(i -> 0).toArray();
		return Array.newInstance(arrayType, dimensions);
	}
	
	private @NotNull Object createList(@NotNull String value) {
		if (!(value.isEmpty() || "[]".equals(value) || "![]".equals(value))) {
			throw new IllegalArgumentException("Invalid list value '" + value + "', default string factory only supports empty lists (e.g. '' or '[]' for mutable lists and '![]' for immutable lists)");
		}
		if ("![]".equals(value)) {
			return List.of();
		}
		return new ArrayList<>();
	}
	
	private @NotNull Object createSet(@NotNull String value) {
		if (!(value.isEmpty() || "()".equals(value) || "!()".equals(value))) {
			throw new IllegalArgumentException("Invalid set value '" + value + "', default string factory only supports empty sets (e.g. '' or '()' for mutable sets and '!()' for immutable sets)");
		}
		if ("!()".equals(value)) {
			return Set.of();
		}
		return new HashSet<>();
	}
	
	private @NotNull Object createMap(@NotNull String value) {
		if (!(value.isEmpty() || "{}".equals(value) || "!{}".equals(value))) {
			throw new IllegalArgumentException("Invalid map value '" + value + "', default string factory only supports empty maps (e.g. '' or '{}' for mutable maps and '!{}' for immutable maps)");
		}
		if ("!{}".equals(value)) {
			return Map.of();
		}
		return new HashMap<>();
	}
	//endregion
	
	//region Direct registration
	public boolean registerDirect(@NotNull Type type, @NotNull Function<String, Object> factory) {
		if (this.directFactories.containsKey(type)) {
			return false;
		}
		return this.directFactories.put(type, factory) == null;
	}
	
	public boolean replaceDirect(@NotNull Type type, @NotNull Function<String, Object> factory) {
		if (!this.directFactories.containsKey(type)) {
			return false;
		}
		return this.directFactories.put(type, factory) != null;
	}
	
	public boolean removeDirect(@NotNull Type type) {
		return this.directFactories.remove(type) != null;
	}
	//endregion
	
	//region Inheritance registration
	public boolean registerInheritance(@NotNull Type type, @NotNull Function<String, Object> factory) {
		if (this.inheritanceFactories.containsKey(type)) {
			return false;
		}
		return this.inheritanceFactories.put(type, factory) == null;
	}
	
	public boolean replaceInheritance(@NotNull Type type, @NotNull Function<String, Object> factory) {
		if (!this.inheritanceFactories.containsKey(type)) {
			return false;
		}
		return this.inheritanceFactories.put(type, factory) != null;
	}
	
	public boolean removeInheritance(@NotNull Type type) {
		return this.inheritanceFactories.remove(type) != null;
	}
	//endregion
	
	public void registerArray(@NotNull BiFunction<Type, String, Object> factory) {
		this.arrayFactory.accept(factory);
	}
	
	//region Helper methods
	private @NotNull Class<?> getClass(@NotNull Type type) {
		try {
			return Class.forName(type.getClassName());
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	//endregion
}
