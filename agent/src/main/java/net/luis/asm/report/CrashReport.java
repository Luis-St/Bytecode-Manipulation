package net.luis.asm.report;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.*;

/**
 *
 * @author Luis-St
 *
 */

public class CrashReport {
	
	private static final String DEFAULT_MESSAGE = "An error occurred during the class transformation process";
	private static final String DEFAULT_TYPE = "Crash Report";
	
	private final LinkedHashMap<String, Object> details = new LinkedHashMap<>();
	private final String message;
	private final String type;
	private final Throwable exception;
	private boolean canContinue = false;
	private int exitCode = 1;
	
	private CrashReport(@Nullable String message, @Nullable String type, @Nullable Throwable exception) {
		this.message = message == null ? DEFAULT_MESSAGE : message;
		this.type = type == null ? DEFAULT_TYPE : type;
		this.exception = exception;
	}
	
	//region Factory methods
	public static @NotNull CrashReport create(@NotNull String message, @Nullable Throwable exception) {
		return new CrashReport(message, null, exception);
	}
	
	public static @NotNull CrashReport create(@NotNull String message, @NotNull String type, @Nullable Throwable exception) {
		return new CrashReport(message, type, exception);
	}
	
	public static @NotNull CrashReport createPreload(@NotNull String message, @Nullable Throwable exception) {
		return new CrashReport(message, "Preload Report", exception);
	}
	
	public static @NotNull CrashReport createTransformation(@NotNull String message, @Nullable Throwable exception) {
		return new CrashReport(message, "Class Transformation Report", exception);
	}
	//endregion
	
	//region Getters and setters
	public @NotNull String getMessage() {
		return this.message;
	}
	
	public @NotNull String getType() {
		return this.type;
	}
	
	public @Nullable Throwable getException() {
		return this.exception;
	}
	
	public boolean canContinue() {
		return this.canContinue;
	}
	
	public @NotNull CrashReport setCanContinue(boolean canContinue) {
		this.canContinue = canContinue;
		return this;
	}
	
	public int getExitCode() {
		return this.exitCode;
	}
	
	public @NotNull CrashReport setExitCode(int exitCode) {
		this.exitCode = exitCode;
		return this;
	}
	
	public @NotNull Map<String, Object> getDetails() {
		return this.details;
	}
	
	public @NotNull CrashReport addDetailFirst(@NotNull String key, @NotNull Object value) {
		this.details.putFirst(key, value);
		return this;
	}
	
	public @NotNull CrashReport addDetail(@NotNull String key, @NotNull Object value) {
		this.details.put(key, value);
		return this;
	}
	
	public @NotNull CrashReport addDetailLast(@NotNull String key, @NotNull Object value) {
		this.details.putLast(key, value);
		return this;
	}
	//endregion
	
	public ReportedException exception() {
		return new ReportedException(this);
	}
	
	public void print() {
		this.print(System.err);
	}
	
	public void print(@NotNull PrintStream stream) {
		stream.println(this.toString());
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("-".repeat(25)).append(" ").append(this.type).append(" ").append("-".repeat(25)).append("\n");
		builder.append("Message").append(": ").append(this.message).append("\n");
		this.details.forEach((key, value) -> builder.append(this.getDetailString(key, value)));
		if (this.exception != null) {
			builder.append("Exception:\n").append(this.getStackTrace());
		}
		return builder.toString();
	}
	
	//region Formatting helpers
	private @NotNull String getStackTrace() {
		StringWriter writer = new StringWriter();
		if (this.exception != null) {
			this.exception.printStackTrace(new PrintWriter(writer));
		}
		return writer.toString();
	}
	
	private @NotNull String getDetailString(@NotNull String key, @NotNull Object value) {
		return switch (value) {
			case List<?> list -> key + ": " + this.getListString(list);
			case Map<?, ?> map -> key + ": " + this.getMapString(map);
			default -> key + ": " + value + "\n";
		};
	}
	
	private @NotNull String getListString(@NotNull List<?> list) {
		StringBuilder builder = new StringBuilder("\n");
		for (Object object : list) {
			builder.append("\t");
			if (object instanceof List<?> innerList) {
				builder.append(this.getListString(innerList).replace("\n", "\n\t").stripTrailing());
			} else if (object instanceof Map<?, ?> map) {
				builder.append(this.getMapString(map).replace("\n", "\n\t").stripTrailing());
			} else {
				builder.append(object);
			}
			builder.append("\n");
		}
		return builder.toString();
	}
	
	private @NotNull String getMapString(@NotNull Map<?, ?> map) {
		StringBuilder builder = new StringBuilder("\n");
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			builder.append("\t").append(entry.getKey()).append(": ");
			if (entry.getValue() instanceof List<?> list) {
				builder.append(this.getListString(list).replace("\n", "\n\t").stripTrailing());
			} else if (entry.getValue() instanceof Map<?, ?> innerMap) {
				builder.append(this.getMapString(innerMap).replace("\n", "\n\t").stripTrailing());
			} else {
				builder.append(entry.getValue());
			}
			builder.append("\n");
		}
		return builder.toString();
	}
	//endregion
}
