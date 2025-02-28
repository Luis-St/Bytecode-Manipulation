package net.luis.agent.asm.report;

import net.luis.agent.asm.data.*;
import net.luis.agent.util.SortedHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Luis-St
 *
 */

public final class CrashReport {
	
	private static final String DEFAULT_MESSAGE = "An error occurred during the class transformation process";
	private static final String DEFAULT_CATEGORY = "Class Transformation Error";
	
	private final SortedHashMap<String, Object> details = new SortedHashMap<>();
	private final String category;
	private String message = DEFAULT_MESSAGE;
	private @Nullable Throwable exception;
	private int exitCode = 1;
	
	private CrashReport() {
		this(DEFAULT_CATEGORY);
	}
	
	private CrashReport(@NotNull String category) {
		this.category = category;
	}
	
	//region Factory methods
	public static @NotNull CrashReport create(@NotNull String category) {
		return new CrashReport(category);
	}
	
	public static @NotNull CrashReport create(@NotNull String message, @Nullable Throwable exception) {
		return new CrashReport().setMessage(message).setException(exception);
	}
	
	public static @NotNull CrashReport create(@NotNull String message, @NotNull String category) {
		return new CrashReport(category).setMessage(message);
	}
	//endregion
	
	//region Getters
	public @NotNull String getMessage() {
		return this.message;
	}
	
	public @NotNull String getCategory() {
		return this.category;
	}
	
	public @Nullable Throwable getException() {
		return this.exception;
	}
	
	public @NotNull Map<String, Object> getDetails() {
		return this.details;
	}
	
	public int getExitCode() {
		return this.exitCode;
	}
	//endregion
	
	//region Builder methods
	public @NotNull CrashReport setMessage(@Nullable String message) {
		this.message = message == null ? DEFAULT_MESSAGE : message;
		return this;
	}
	
	public @NotNull CrashReport setException(@Nullable Throwable exception) {
		this.exception = exception;
		return this;
	}
	
	public @NotNull CrashReport setExitCode(int exitCode) {
		this.exitCode = exitCode;
		return this;
	}
	
	public @NotNull CrashReport addDetail(@NotNull String key, @Nullable Object value) {
		this.details.put(key, value);
		return this;
	}
	
	public @NotNull CrashReport addDetailFirst(@NotNull String key, @Nullable Object value) {
		this.details.putFirst(key, value);
		return this;
	}
	
	public @NotNull CrashReport addDetailBefore(@NotNull String target, @NotNull String key, @Nullable Object value) {
		this.details.putBefore(target, key, value);
		return this;
	}
	
	public @NotNull CrashReport addDetailAfter(@NotNull String target, @NotNull String key, @Nullable Object value) {
		this.details.putAfter(target, key, value);
		return this;
	}
	
	public @NotNull CrashReport addParameterDetails(@NotNull Parameter parameter) {
		this.details.put("Parameter Index", parameter.getIndex());
		this.details.put("Parameter Type", parameter.getType());
		this.details.put("Parameter Name", parameter.getName());
		return this;
	}
	
	public @NotNull CrashReport addFieldDetails(@NotNull Field field) {
		this.details.put("Field Name", field.getName());
		this.details.put("Field Type", field.getType());
		return this;
	}
	
	public @NotNull CrashReport addLocalDetails(@NotNull LocalVariable local) {
		this.details.put("Local Index", local.getIndex());
		this.details.put("Local Name", local.getName());
		this.details.put("Local Type", local.getType());
		return this;
	}
	
	public @NotNull CrashReport replaceDetail(@NotNull String key, @Nullable Object value) {
		this.details.replace(key, value);
		return this;
	}
	
	public @NotNull CrashReport removeParameterDetails() {
		this.details.remove("Parameter Index");
		this.details.remove("Parameter Type");
		this.details.remove("Parameter Name");
		return this;
	}
	
	public @NotNull CrashReport removeFieldDetails() {
		this.details.remove("Field Name");
		this.details.remove("Field Type");
		return this;
	}
	
	public @NotNull CrashReport removeLocalDetails() {
		this.details.remove("Local Index");
		this.details.remove("Local Name");
		this.details.remove("Local Type");
		return this;
	}
	
	public @NotNull CrashReport removeDetail(@NotNull String key) {
		this.details.remove(key);
		return this;
	}
	//endregion
	
	public @NotNull ReportedException exception() {
		return new ReportedException(this);
	}
	
	public ReportedException exception(@Nullable String message) {
		return new ReportedException(this.setMessage(message));
	}
	
	public void print() {
		this.print(System.err);
	}
	
	public void print(@NotNull PrintStream stream) {
		stream.println(this);
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("-".repeat(25)).append(" Crash Report ").append("-".repeat(25)).append("\n");
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
	
	private @NotNull String getDetailString(@NotNull String key, @Nullable Object value) {
		return switch (value) {
			case List<?> list -> key + ": " + this.getListString(list);
			case Map<?, ?> map -> key + ": " + this.getMapString(map);
			case null -> key + ": null\n";
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
