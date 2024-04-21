package net.luis.asm;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *
 * @author Luis-St
 *
 */

public class ASMUtils {
	
	@SafeVarargs
	public static <T> @NotNull List<T> newArrayList(T @NotNull ... elements) {
		return new ArrayList<>(Arrays.asList(elements));
	}
	
	@SafeVarargs
	public static <T> @NotNull Set<T> newSet(T@NotNull ... elements) {
		return new HashSet<>(Arrays.asList(elements));
	}
	
	public static <T> @NotNull Supplier<T> memorize(@NotNull Supplier<T> supplier) {
		return new MemorizedSupplier<>(supplier);
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
	
	//region Internal
	private static class MemorizedSupplier<T> implements Supplier<T> {
		
		private final Supplier<T> supplier;
		private T value;
		
		private MemorizedSupplier(@NotNull Supplier<T> supplier) {
			this.supplier = supplier;
		}
		
		@Override
		public T get() {
			if (this.value == null) {
				this.value = this.supplier.get();
			}
			return this.value;
		}
	}
	//endregion
}
