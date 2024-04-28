package net.luis.asm.report;

import org.jetbrains.annotations.NotNull;

/**
 *
 * @author Luis-St
 *
 */

public class ReportedException extends RuntimeException {
	
	private final CrashReport report;
	
	public ReportedException(@NotNull CrashReport report) {
		this.report = report;
	}
	
	public @NotNull CrashReport getReport() {
		return this.report;
	}
}
