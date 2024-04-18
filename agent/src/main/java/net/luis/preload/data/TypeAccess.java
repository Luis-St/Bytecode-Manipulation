package net.luis.preload.data;

import org.objectweb.asm.Opcodes;

/**
 *
 * @author Luis-St
 *
 */

public enum TypeAccess {
	
	PUBLIC(Opcodes.ACC_PUBLIC),
	PROTECTED(Opcodes.ACC_PROTECTED),
	PACKAGE(-1),
	PRIVATE(Opcodes.ACC_PRIVATE);
	
	private final int value;
	
	TypeAccess(int value) {
		this.value = value;
	}
	
	public static TypeAccess fromAccess(int access) {
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
