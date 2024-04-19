package net.luis.asm;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *
 * @author Luis-St
 *
 */

public class ASMUtils {
	
	@SafeVarargs
	public static <T> List<T> newArrayList(T... elements) {
		return new ArrayList<>(Arrays.asList(elements));
	}
	
	@SafeVarargs
	public static <T> Set<T> newSet(T... elements) {
		return new HashSet<>(Arrays.asList(elements));
	}
	
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
