package net.luis;

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
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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
	 *  - Allow parameters in method with @Scheduled annotation (int -> for current cycle, ScheduleFuture -> for canceling) -> make threads daemon
	 *  - Overhaul DefaultStringFactory
	 */
	
	private static final ScheduledExecutorService EXECUTOR;
	
	public static void main(@Default @NotNull String[] args) {
		WeightCollection<String> collection = new WeightCollection<>();
		collection.add(10, "Hello");
		
		new InjectorTest().test(1);
		
		execute("ls", null, null);
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
	public static void execute(@Pattern("^[a-z]*$") String command, @Default("[]") @NotNull String[] args, @Default List<String> values) {
		System.out.println("Command: " + command);
		System.out.println("Args: " + Arrays.toString(args));
		System.out.println("Values: " + values);
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
	
	public static void scheduled(int count) {
		System.out.println(count);
	}
	
	public static void scheduled(@NotNull ScheduledFuture<?> future) {
		System.out.println(future);
	}
	
	public static void scheduled(int count, @NotNull ScheduledFuture<?> future) {
		System.out.println(count + " " + future);
	}
	
	static {
		EXECUTOR = new ScheduledThreadPoolExecutor(4, new DaemonThreadFactory());
		EXECUTOR.scheduleAtFixedRate(Main::scheduled, 0, 1, TimeUnit.SECONDS);
		EXECUTOR.scheduleAtFixedRate(new CountingRunnable(Main::scheduled), 0, 1, TimeUnit.SECONDS);
		Map<String, ScheduledFuture<?>> lookup = new ConcurrentHashMap<>();
		lookup.put("net.luis.Main#scheduled(ScheduledFuture)", EXECUTOR.scheduleAtFixedRate(new CancelableRunnable("net.luis.Main#scheduled(ScheduledFuture)", lookup, Main::scheduled), 0, 1, TimeUnit.SECONDS));
		lookup.put("net.luis.Main#scheduled(int, ScheduledFuture)", EXECUTOR.scheduleAtFixedRate(new ContextRunnable("net.luis.Main#scheduled(int, ScheduledFuture)", lookup, Main::scheduled), 0, 1, TimeUnit.SECONDS));
	}
	
	private static class DaemonThreadFactory implements ThreadFactory {
		
		private final ThreadFactory defaultFactory = Executors.defaultThreadFactory();
		
		@Override
		public @NotNull Thread newThread(@NotNull Runnable runnable) {
			Thread thread = this.defaultFactory.newThread(runnable);
			thread.setDaemon(true);
			return thread;
		}
	}
	
	private static class CountingRunnable implements Runnable {
		
		private final Consumer<Integer> action;
		private int count;
		
		private CountingRunnable(@NotNull Consumer<Integer> action) {
			this.action = action;
		}
		
		@Override
		public void run() {
			this.action.accept(this.count++);
		}
	}
	
	private static class CancelableRunnable implements Runnable {
		
		private final String method;
		private final Map<String, ScheduledFuture<?>> lookup;
		private final Consumer<ScheduledFuture<?>> action;
		private ScheduledFuture<?> future;
		
		private CancelableRunnable(@NotNull String method, @NotNull Map<String, ScheduledFuture<?>> lookup, @NotNull Consumer<ScheduledFuture<?>> action) {
			this.method = method;
			this.lookup = lookup;
			this.action = action;
		}
		
		@Override
		public void run() {
			if (this.future == null) {
				this.future = this.lookup.get(this.method);
			}
			this.action.accept(this.future);
		}
	}
	
	private static class ContextRunnable implements Runnable {
		
		private final String method;
		private final Map<String, ScheduledFuture<?>> lookup;
		private final BiConsumer<Integer, ScheduledFuture<?>> action;
		private ScheduledFuture<?> future;
		private int count;
		
		private ContextRunnable(@NotNull String method, @NotNull Map<String, ScheduledFuture<?>> lookup, @NotNull BiConsumer<Integer, ScheduledFuture<?>> action) {
			this.method = method;
			this.lookup = lookup;
			this.action = action;
		}
		
		@Override
		public void run() {
			if (this.future == null) {
				this.future = this.lookup.get(this.method);
			}
			this.action.accept(this.count++, this.future);
		}
	}
}
