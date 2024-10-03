package net.luis.agent.util;

import org.jetbrains.annotations.NotNull;

/**
 *
 * @author Luis-St
 *
 */

public enum RoundingMode {
	
	ROUND("roundTo"),
	FLOOR("floor"),
	FLOOR_DIVISION("floorDiv"),
	FLOOR_DIVISION_EXACT("floorDivExact"),
	FLOOR_MODULO("floorMod"),
	CEIL("ceil"),
	CEIL_DIVISION("ceilDiv"),
	CEIL_DIVISION_EXACT("ceilDivExact"),
	CEIL_MODULO("ceilMod");
	
	private final String methodName;
	
	RoundingMode(@NotNull String methodName) {
		this.methodName = methodName;
	}
	
	public @NotNull String getMethodName() {
		return this.methodName;
	}
	
	public boolean requiresFloatingPointInput() {
		return this == ROUND || this == FLOOR || this == CEIL;
	}
}
