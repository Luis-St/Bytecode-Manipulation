package net.luis.asm;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *
 * @author Luis-St
 *
 */

public class ASMHelper {
	
	//region Factory helper methods
	public static <T> List<T> newList() {
		return new ArrayList<>();
	}
	
	@SafeVarargs
	public static <T> List<T> newList(T... elements) {
		List<T> list = newList();
		list.addAll(Arrays.asList(elements));
		return list;
	}
	
	public static <T> Set<T> newSet() {
		return new HashSet<>();
	}
	
	@SafeVarargs
	public static <T> Set<T> newSet(T... elements) {
		Set<T> set = newSet();
		set.addAll(Arrays.asList(elements));
		return set;
	}
	
	public static <K, V> Map<K, V> newMap() {
		return new HashMap<>();
	}
	//endregion
	
	//region Array to list
	public static List<Boolean> asList(boolean[] array) {
		return IntStream.range(0, array.length).mapToObj(i -> array[i]).collect(Collectors.toList());
	}
	
	public static List<Byte> asList(byte[] array) {
		return IntStream.range(0, array.length).mapToObj(i -> array[i]).collect(Collectors.toList());
	}
	
	public static List<Short> asList(short[] array) {
		return IntStream.range(0, array.length).mapToObj(i -> array[i]).collect(Collectors.toList());
	}
	
	public static List<Integer> asList(int[] array) {
		return Arrays.stream(array).boxed().collect(Collectors.toList());
	}
	
	public static List<Long> asList(long[] array) {
		return Arrays.stream(array).boxed().collect(Collectors.toList());
	}
	
	public static List<Float> asList(float[] array) {
		return IntStream.range(0, array.length).mapToObj(i -> array[i]).collect(Collectors.toList());
	}
	
	public static List<Double> asList(double[] array) {
		return Arrays.stream(array).boxed().collect(Collectors.toList());
	}
	
	public static List<Character> asList(char[] array) {
		return IntStream.range(0, array.length).mapToObj(i -> array[i]).collect(Collectors.toList());
	}
	//endregion
}
