package net.luis.test;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

/**
 *
 * @author Luis-St
 *
 */

public class ModifyTest {
	
	private int result = 0;
	
	public int calculate(int a, @NotNull String radix, @NotNull List<Integer> values) {
		int r = Integer.parseInt(radix);
		if (r != 2 && r != 8 && r != 10 && r != 16) {
			throw new IllegalArgumentException("Invalid radix: " + radix);
		}
		int result = 0;
		for (int value : values) {
			result += (value * r);
		}
		
		Function<Integer, Integer> recursiveTest = value -> {
			Class<?> clazz = Integer.class;
			System.out.println(clazz.getSimpleName());
			return value;
		};
		
		this.result = recursiveTest.apply(a + result);
		return this.result;
	}
	
	public int getResult() {
		return this.result;
	}
}
