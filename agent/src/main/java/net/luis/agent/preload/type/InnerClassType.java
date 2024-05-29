package net.luis.agent.preload.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *
 * @author Luis-St
 *
 */

public enum InnerClassType {
	
	INNER, LOCAL, ANONYMOUS;
	
	public static @NotNull InnerClassType fromNames(@Nullable String outerName, @Nullable String innerName) {
		if (outerName == null && innerName == null) {
			return ANONYMOUS;
		}
		return outerName == null ? LOCAL : INNER;
	}
}
