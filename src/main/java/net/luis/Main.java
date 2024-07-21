package net.luis;

import com.google.common.base.Converter;
import com.google.common.collect.Lists;
import net.luis.agent.annotation.*;
import net.luis.agent.annotation.range.*;
import net.luis.agent.annotation.string.condition.Contains;
import net.luis.agent.annotation.string.condition.NotEmpty;
import net.luis.agent.annotation.string.modification.Substring;
import net.luis.utils.collection.WeightCollection;
import net.luis.utils.lang.StringUtils;
import org.intellij.lang.annotations.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ScheduledFuture;

import static org.apache.commons.lang3.StringUtils.*;

/**
 *
 * @author Luis
 *
 */

public final class Main {
	
	/*
	 * ToDo:
	 *  - @ImplicitNotNull annotation for other annotations where the value must not be null
	 *  - Add transformers for unused annotations
	 *  - Find solution for annotations on fields works currently only on local fields (because of computing frames)
	 */
	
	@NotEmpty
	@Pattern("^\\S*$")
	private static String test = "Hello";
	
	public static void main(@Default @NotNull String[] args) {
		WeightCollection<String> collection = new WeightCollection<>();
		collection.add(10, "Hello");
		
		Converter<String, Integer> converter = new Converter<>() {
			@Override
			protected @NotNull Integer doForward(@NotNull String s) {
				return Integer.parseInt(s);
			}
			
			@Override
			protected @NotNull String doBackward(@NotNull Integer integer) {
				return String.valueOf(integer).replace("0", "X");
			}
		};
		Lists.newArrayList("10", "1").stream().map(converter::convert).forEach(System.out::println);
		
		supports(new ArrayList<>(Arrays.asList("Hello", "World")));
		supports(new HashMap<>(Map.of("Hello", "World")));
		supports(10);
		execute("ls", null, null, null);
		parseUUID("550e8400-e29b-41d4-a716-446655440000", null, null);
		validateIndex(1);
		async(1, "Hello World!", Arrays.asList("Hello", "World", "!"));
		caught();
		System.out.println(StringUtils.levenshteinDistance("Hello", "World"));
		test += "World";
		System.out.println(test);
	}
	
	public static void supports(@NotNull @Supports({ List.class, Map.class, int.class }) Object obj) {
		System.out.println(obj.getClass());
	}
	
	@RestrictedAccess("Main#main")
	public static void execute(@Pattern("^[a-z]*$") String command, @Default("[-t, -r]") String[] args, @Default("[1, '2.05']") List<Number> values, @Default("{user=test,debug=false,threads=2}") LinkedHashMap<String, Object> environment) {
		@NotEmpty
		@Contains("-")
		@Substring("1:*-1")
		String arguments = Arrays.toString(args);
		System.out.println("Command: " + command);
		System.out.println("Args: " + arguments);
		System.out.println("Values: " + values);
		System.out.println("Environment: ");
		for (Map.Entry<String, Object> entry : environment.entrySet()) {
			System.out.println("  " + entry.getKey() + ": " + entry.getValue() + " (" + entry.getValue().getClass().getSimpleName() + ")");
		}
	}
	
	public static void parseUUID(@UUID String uuid, @Default("strict") ParserMode first, @Default("built in") ParserMode second) {
		System.out.println("UUID: " + uuid);
		System.out.println("First: " + first);
		System.out.println("Second: " + second);
	}
	
	@Pattern("^.*$")
	public static @NotNull String getExtension(@Nullable String file) {
		String str = stripToEmpty(file);
		@AboveEqual(-1)
		int index = str.lastIndexOf(".");
		if (index == -1) {
			return "";
		} else {
			return str.substring(index + 1);
		}
	}
	
	@Above(0)
	public static int validateIndex(@BelowEqual(1) Integer index) {
		System.out.println("Index: " + index);
		return index;
	}
	
	@Async
	public static void async(int i, @NotNull String str, @Default("[]") List<String> values) {
		System.out.println("i: " + i);
		System.out.println("str: " + str);
		System.out.println("values: " + values);
		System.out.println("Thread: " + Thread.currentThread().getName());
	}
	
	@Caught
	public static void caught() {
		System.out.println("Test Caught");
		throw new RuntimeException("Caught Exception");
	}
	
	@Scheduled(5000)
	public static void scheduled(int count, @NotNull ScheduledFuture<?> future) {
		System.out.println(count + " " + future);
	}
	
	public enum ParserMode {
		DEFAULT, STRICT, LENIENT, BUILT_IN
	}
}
