package net.luis;

import net.luis.agent.annotation.*;
import net.luis.agent.annotation.range.Above;
import net.luis.agent.annotation.range.BelowEqual;
import net.luis.utils.logging.LoggerConfiguration;
import net.luis.utils.logging.LoggingType;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.intellij.lang.annotations.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 *
 * @author Luis
 *
 */

@Test(
	testInt = -1,
	testString = "Hello World!",
	testBoolean = true,
	testDouble = 0.5,
	testClass = Main.class,
	testIntArray = { 1, 2, 3 },
	testStringArray = { "Hello", "World", "!" },
	testClassArray = Main.class,
	testAnnotation = @AnnotationExample("test")
)
public final class Main {
	
	public static void main(@Default @NotNull String[] args) {
		LoggerConfiguration logger = new LoggerConfiguration("*");
		
		execute("ls", null, null);
		validateIndex(1);
		async(1, "Hello World!", Arrays.asList("Hello", "World", "!"));
		caught();
		if (logger instanceof MyInterface my) {
			System.out.println("LoggerConfiguration is an instance of MyInterface!");
			System.out.println(my.build().getName());
			List<String> loggers = my.getLoggers();
			Set<LoggingType> types = my.getTypes();
			System.out.println(loggers);
			System.out.println(types);
			my.setLoggers(loggers);
			my.setTypes(types);
			System.out.println(my.invokeGetPattern(LoggingType.CONSOLE, Level.TRACE));
			System.out.println(my.getLoggingPattern(LoggingType.FILE, Level.ERROR));
		} else {
			System.out.println("LoggerConfiguration is not an instance of MyInterface!");
		}
	}
	
	public static void execute(@Pattern("^[a-z]*$") String command, @Default("[]") @NotNull String[] args, @Default List<String> values) {
		System.out.println("Command: " + command);
		System.out.println("Args: " + Arrays.toString(args));
		System.out.println("Values: " + values);
	}
	
	@Pattern("^.*$")
	public static @NotNull String getExtension(@Nullable String file) {
		String str = StringUtils.stripToEmpty(file);
		int index = str.lastIndexOf(".");
		if (index == -1) {
			return "";
		} else {
			return str.substring(index + 1);
		}
	}
	
	@Above(0)
	public static int validateIndex(@BelowEqual(1) int index) {
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
	
	//@Scheduled(1000)
	public static void scheduled() {
		System.out.println("Test Scheduled");
	}
}
