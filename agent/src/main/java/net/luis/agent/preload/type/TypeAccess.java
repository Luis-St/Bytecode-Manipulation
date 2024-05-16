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
	
	private final int opcode;
	
	TypeAccess(int opcode) {
		this.opcode = opcode;
	}
	
	//region Static methods
	public static @NotNull TypeAccess fromAccess(int access) {
		for (TypeAccess typeAccess : values()) {
			if (typeAccess == PACKAGE) {
				continue;
			}
			if ((access & typeAccess.opcode) != 0) {
				return typeAccess;
			}
		}
		return PACKAGE;
	}
	//endregion
	
	public int getOpcode() {
		return this.opcode;
	}
}
