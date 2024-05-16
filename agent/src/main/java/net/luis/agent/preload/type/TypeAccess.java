package net.luis.agent.preload.type;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;

/**
 *
 * @author Luis-St
 *
 */

public enum TypeAccess {
	
	PUBLIC(Opcodes.ACC_PUBLIC),
	PROTECTED(Opcodes.ACC_PROTECTED),
	PACKAGE(0),
	PRIVATE(Opcodes.ACC_PRIVATE);
	
	private final int value;
	
	TypeAccess(int value) {
		this.value = value;
	}
	
	public static @NotNull TypeAccess fromAccess(int access) {
		for (TypeAccess typeAccess : values()) {
			if (typeAccess == PACKAGE) {
				continue;
			}
			if ((access & typeAccess.value) != 0) {
				return typeAccess;
			}
		}
		return PACKAGE;
	}
}
