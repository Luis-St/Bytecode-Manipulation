package net.luis.agent.util.factory;

import net.luis.agent.asm.signature.ActualType;
import net.luis.utils.io.reader.ScopedStringReader;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;

/**
 *
 * @author Luis-St
 *
 */

@SuppressWarnings("unchecked")
class DefaultStringFactories {
	
	public static @NotNull StringFactory createSimple(@NotNull Function<ScopedStringReader, ?> factory) {
		Objects.requireNonNull(factory, "Factory must not be null");
		return (type, actual, reader) -> factory.apply(reader);
	}
	
	public static @NotNull Object createObject(@NotNull String type, @NotNull ActualType actual, @NotNull ScopedStringReader reader) {
		String value = reader.getString().strip();
		if (value.isEmpty()) {
			return "";
		}
		if (value.length() == 1) {
			if (Character.isDigit(value.charAt(0))) {
				return Integer.parseInt(value);
			}
			return value.charAt(0);
		}
		char first = value.charAt(0);
		char last = value.charAt(value.length() - 1);
		if (first == '[' && last == ']') {
			return reader.readList(r -> StringFactoryRegistry.INSTANCE.create(type, actual, r));
		} else if (first == '(' && last == ')') {
			return reader.readSet(r -> StringFactoryRegistry.INSTANCE.create(type, actual, r));
		} else if (first == '{' && last == '}') {
			return reader.readMap(r -> StringFactoryRegistry.INSTANCE.create(type, actual, r), r -> StringFactoryRegistry.INSTANCE.create(type, actual, r));
		}
		try {
			return reader.readBoolean();
		} catch (Exception ignored) {}
		try {
			return reader.readNumber();
		} catch (Exception ignored) {}
		return reader.readString();
	}
	
	public static <T> @NotNull Object createArray(@NotNull String type, @NotNull ActualType actual, @NotNull ScopedStringReader reader) {
		int dimensions = (int) type.chars().filter(c -> c == '[').count();
		if (dimensions > 1) {
			throw new IllegalArgumentException("Multi-dimensional arrays are currently not supported");
		}
		String innerType = type.substring(0, type.indexOf("[]"));
		List<T> list = reader.readList(r -> (T) StringFactoryRegistry.INSTANCE.create(innerType, ActualType.of(actual.type().getElementType()), r));
		try {
			T[] array = (T[]) Array.newInstance(Class.forName(innerType), list.size());
			for (int i = 0; i < list.size(); i++) {
				array[i] = list.get(i);
			}
			return array;
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("Unable to create array of type: " + innerType, e);
		}
	}
	
	public static <T> @NotNull Object createList(@NotNull String type, @NotNull ActualType actual, @NotNull ScopedStringReader reader) {
		List<T> list = new ArrayList<>();
		if (!"java.util.List".equals(type)) {
			try {
				list = createInstance(type, list);
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException("Unable to create list of type '" + type + "' because the class was not found on the classpath", e);
			} catch (Exception e) {
				throw new IllegalArgumentException("Unable to create list of type: " + type, e);
			}
		}
		if (actual.nested().size() != 1) {
			throw new IllegalArgumentException("List must have exactly one nested type, but found: " + actual);
		}
		ActualType element = actual.nested().getFirst();
		list.addAll(reader.readList(r -> (T) StringFactoryRegistry.INSTANCE.create(element.type().getClassName(), element, r)));
		return list;
	}
	
	public static <T> @NotNull Object createSet(@NotNull String type, @NotNull ActualType actual, @NotNull ScopedStringReader reader) {
		Set<T> set = new HashSet<>();
		if (!"java.util.Set".equals(type)) {
			try {
				set = createInstance(type, set);
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException("Unable to create set of type '" + type + "' because the class was not found on the classpath", e);
			} catch (Exception e) {
				throw new IllegalArgumentException("Unable to create set of type: " + type, e);
			}
		}
		if (actual.nested().size() != 1) {
			throw new IllegalArgumentException("Set must have exactly one nested type, but found: " + actual);
		}
		ActualType element = actual.nested().getFirst();
		set.addAll(reader.readSet(r -> (T) StringFactoryRegistry.INSTANCE.create(element.type().getClassName(), element, r)));
		return set;
	}
	
	public static <K, V> @NotNull Object createMap(@NotNull String type, @NotNull ActualType actual, @NotNull ScopedStringReader reader) {
		Map<K, V> map = new HashMap<>();
		if (!"java.util.Map".equals(type)) {
			try {
				map = createInstance(type, map);
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException("Unable to create map of type '" + type + "' because the class was not found on the classpath", e);
			} catch (Exception e) {
				throw new IllegalArgumentException("Unable to create map of type: " + type, e);
			}
		}
		if (actual.nested().size() != 2) {
			throw new IllegalArgumentException("Map must have exactly two nested types, but found: " + actual);
		}
		ActualType key = actual.nested().getFirst();
		ActualType value = actual.nested().getLast();
		map.putAll(reader.readMap(r -> (K) StringFactoryRegistry.INSTANCE.create(key.type().getClassName(), key, r), r -> (V) StringFactoryRegistry.INSTANCE.create(value.type().getClassName(), value, r)));
		return map;
	}
	
	//region Helper methods
	private static <T> @NotNull T createInstance(@NotNull String clazz, @NotNull T defaultValue) throws Exception {
		Class<T> type = (Class<T>) Class.forName(clazz);
		if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
			if (type.isInstance(defaultValue)) {
				return defaultValue;
			}
			RuntimeException exception = new RuntimeException("Unable to use default value for class '" + clazz + "' because it is not an instance of the class");
			exception.initCause(new IllegalArgumentException("Cannot create instance of class '" + clazz + "' because it is an interface or abstract class"));
			throw exception;
		}
		try {
			return type.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			if (type.isInstance(defaultValue)) {
				return defaultValue;
			}
			RuntimeException exception = new RuntimeException("Unable to use default value for class '" + clazz + "' because it is not an instance of the class");
			exception.initCause(e);
			throw exception;
		}
	}
	//endregion
}
