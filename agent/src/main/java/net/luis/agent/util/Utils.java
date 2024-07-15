package net.luis.agent.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.*;

/**
 *
 * @author Luis-St
 *
 */

public class Utils {
	
	public static <T> @NotNull T make(@NotNull T object, @NotNull Consumer<T> consumer) {
		consumer.accept(object);
		return object;
	}
	
	public static <T> @NotNull Stream<T> stream(T @Nullable [] array) {
		return array == null ? Stream.empty() : Arrays.stream(array).filter(Objects::nonNull);
	}
	
	public static @NotNull String capitalize(@NotNull String string) {
		return string.isEmpty() ? string : Character.toUpperCase(string.charAt(0)) + string.substring(1);
	}
	
	public static @NotNull String uncapitalize(@NotNull String string) {
		return string.isEmpty() ? string : Character.toLowerCase(string.charAt(0)) + string.substring(1);
	}
	
	public static boolean isSingleWord(@NotNull String string) {
		return string.chars().allMatch(Character::isLetterOrDigit);
	}
	
	public static @NotNull String getSeparated(@NotNull String name) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (Character.isUpperCase(c) && i > 0) {
				builder.append(" ");
			}
			builder.append(c);
		}
		return builder.toString().toLowerCase();
	}
	
	public static <T> int indexOf(@NotNull T[] array, @NotNull T element) {
		for (int i = 0; i < array.length; i++) {
			if (Objects.equals(array[i], element)) {
				return i;
			}
		}
		return -1;
	}
	
	//region Array to list
	public static @NotNull List<Boolean> asList(boolean @NotNull [] array) {
		return IntStream.range(0, array.length).mapToObj(i -> array[i]).collect(Collectors.toList());
	}
	
	public static @NotNull List<Byte> asList(byte @NotNull [] array) {
		return IntStream.range(0, array.length).mapToObj(i -> array[i]).collect(Collectors.toList());
	}
	
	public static @NotNull List<Short> asList(short @NotNull [] array) {
		return IntStream.range(0, array.length).mapToObj(i -> array[i]).collect(Collectors.toList());
	}
	
	public static @NotNull List<Integer> asList(int @NotNull [] array) {
		return Arrays.stream(array).boxed().collect(Collectors.toList());
	}
	
	public static @NotNull List<Long> asList(long @NotNull [] array) {
		return Arrays.stream(array).boxed().collect(Collectors.toList());
	}
	
	public static @NotNull List<Float> asList(float @NotNull [] array) {
		return IntStream.range(0, array.length).mapToObj(i -> array[i]).collect(Collectors.toList());
	}
	
	public static @NotNull List<Double> asList(double @NotNull [] array) {
		return Arrays.stream(array).boxed().collect(Collectors.toList());
	}
	
	public static @NotNull List<Character> asList(char @NotNull [] array) {
		return IntStream.range(0, array.length).mapToObj(i -> array[i]).collect(Collectors.toList());
	}
	//endregion
}
