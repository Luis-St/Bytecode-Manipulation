package net.luis;

import net.luis.agent.annotation.Default;
import net.luis.utils.logging.LoggerConfiguration;
import net.luis.utils.logging.LoggingType;
import org.apache.logging.log4j.Level;
import org.jetbrains.annotations.NotNull;

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
		
		execute(null, null, null);
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
	
	public static void execute(@Default("ls") String command, @Default("[]") String[] args, @Default List<String> values) {
		System.out.println("Command: " + command);
		System.out.println("Args: " + Arrays.toString(args));
		System.out.println("Values: " + values);
	}
}
