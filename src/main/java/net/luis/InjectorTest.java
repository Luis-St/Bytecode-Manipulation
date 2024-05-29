package net.luis;

import net.luis.agent.annotation.Caught;

import java.util.*;

/**
 *
 * @author Luis-St
 *
 */

public class InjectorTest {
	
	private static String string = "String";
	
	private int i = 0;
	
	@Caught
	private static void silentThrow() {
		throw new RuntimeException("Silent Throw");
	}
	
	public void test(int index) {
		List<Object> list = new ArrayList<>();
		list.add(List.of(1 + index, 2 - index, 3));
		
		int[] array = new int[] { 1, 2 * index, 3 };
		list.add(array);
		
		int[][] multiArray = new int[][] { { 1, 2, 3 }, { 4, 5, 6 / index } };
		list.add(multiArray);
		
		if (index >= array.length) {
			int i = index % array.length;
			string = "String " + i + " value with index " + index + " is out of bounds";
			index = i;
		}
		if (index < 0) {
			int i = array.length + index;
			string = "String " + i;
			index = i;
		}
		System.out.println(string);
		
		int i = array[index];
		if (i == 100) {
			System.out.println("100");
		}
		if (i != 8192) {
			System.out.println("!10000");
			i = (int) (8192 * new Random().nextDouble());
		}
		
		this.validate();
		
		int j = -i;
		if (j > i) {
			System.out.println("j > i");
		}
		if (i <= j) {
			System.out.println("i <= j");
		}
		
		silentThrow();
		
		array[0] = j & i;
		array[1] = j | i;
		array[2] = j ^ i;
		
		multiArray[0][0] = j << i;
		multiArray[0][1] = j >> i;
		multiArray[0][2] = j >>> i;
		multiArray[1][0] = ~i;
		
		System.out.println(this.i);
		this.i = i + j;
		
		if (list.getFirst() instanceof List) {
			System.out.println("List");
		}
		
		list.add(null);
	}
	
	private void validate() {
		System.out.println("Validate");
	}
}
