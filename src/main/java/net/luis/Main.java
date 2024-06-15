package net.luis;

import com.google.common.base.Converter;
import com.google.common.collect.Lists;
import net.luis.agent.annotation.*;
import net.luis.agent.annotation.range.Above;
import net.luis.agent.annotation.range.BelowEqual;
import net.luis.utils.collection.WeightCollection;
import net.luis.utils.lang.StringUtils;
import net.luis.utils.logging.LoggerConfiguration;
import net.luis.utils.logging.LoggingType;
import org.apache.logging.log4j.Level;
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
	 *  - Add transformers for unused annotations
	 *  - (Not possible?) Add parsing of signature -> Method#getSignature -> Signature -> update StringFactory
	 *  - Update CrashReport -> global context where details can be pushed and popped
	 *  - Add support for static redirect methods to copy the original parameters (only if caller object can be popped)
	 *  - Add parsing of signature -> Method#getSignature -> Signature -> update StringFactory (if possible, else argument for annotation)
	 */
	
	public static void main(@Default @NotNull String[] args) {
		WeightCollection<String> collection = new WeightCollection<>();
		collection.add(10, "Hello");
		
		Converter<String, Integer> converter = new Converter<String, Integer>() {
			@Override
			protected @NotNull Integer doForward(@NotNull String s) {
				return Integer.parseInt(s);
			}
			
			@Override
			protected @NotNull String doBackward(@NotNull Integer integer) {
				return integer.toString();
			}
		};
		Lists.newArrayList("10", "1").stream().map(converter::convert).forEach(System.out::println);
		
		new InjectTest("ABC").test(1, new int[] { 1 });
		
		execute("ls", null, null);
		parseUUID("550e8400-e29b-41d4-a716-446655440000");
		validateIndex(1);
		async(1, "Hello World!", Arrays.asList("Hello", "World", "!"));
		caught();
		System.out.println(StringUtils.levenshteinDistance("Hello", "World"));
		LoggerConfiguration logger = new LoggerConfiguration("*");
		if (logger instanceof ILoggerConfiguration iLogger) {
			System.out.println("LoggerConfiguration is an instance of ILoggerConfiguration!");
			System.out.println(iLogger.build().getName());
			List<String> loggers = iLogger.getLoggers();
			Set<LoggingType> types = iLogger.getTypes();
			System.out.println(loggers);
			System.out.println(types);
			iLogger.setLoggers(loggers);
			iLogger.setTypes(types);
			System.out.println(iLogger.invokeGetPattern(LoggingType.CONSOLE, Level.TRACE));
			System.out.println(iLogger.getLoggingPattern(LoggingType.FILE, Level.ERROR));
		} else {
			System.out.println("LoggerConfiguration is not an instance of ILoggerConfiguration!");
		}
	}
	
	@RestrictedAccess("Main#main")
	public static void execute(@Pattern("^[a-z]*$") String command, @Default("[-t, -r]") @NotNull String[] args, /*@Default */List<String> values) {
		System.out.println("Command: " + command);
		System.out.println("Args: " + Arrays.toString(args));
		System.out.println("Values: " + values);
	}
	
	public static void parseUUID(@UUID String uuid) {
		System.out.println("UUID: " + uuid);
	}
	
	@Pattern("^.*$")
	public static @NotNull String getExtension(@Nullable String file) {
		String str = stripToEmpty(file);
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
}
