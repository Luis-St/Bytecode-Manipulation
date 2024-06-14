package net.luis.agent.util.factory;

import net.luis.utils.io.reader.ScopedStringReader;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 *
 * @author Luis-St
 *
 */

@SuppressWarnings("unchecked")
class DefaultStringFactories {
	
	public static @NotNull BiFunction<String, ScopedStringReader, ?> createSimple(@NotNull Function<ScopedStringReader, ?> factory) {
		Objects.requireNonNull(factory, "Factory must not be null");
		return (type, reader) -> factory.apply(reader);
	}
	
	public static <T> @NotNull Object createArray(@NotNull String type, @NotNull ScopedStringReader reader) {
		int dimensions = (int) type.chars().filter(c -> c == '[').count();
		if (dimensions > 1) {
			throw new IllegalArgumentException("Multi-dimensional arrays are currently not supported");
		}
		String innerType = type.substring(0, type.indexOf("[]"));
		List<T> list = reader.readList(r -> (T) StringFactoryRegistry.INSTANCE.create(innerType, r));
		T[] array = (T[]) Array.newInstance(getClass(innerType), list.size());
		for (int i = 0; i < list.size(); i++) {
			array[i] = list.get(i);
		}
		return array;
	}
	
	//region Helper methods
	private static <T> @NotNull Class<T> getClass(@NotNull String type) {
		try {
			return (Class<T>) Class.forName(type);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("Class not found: " + type, e);
		}
	}
	//endregion
}
