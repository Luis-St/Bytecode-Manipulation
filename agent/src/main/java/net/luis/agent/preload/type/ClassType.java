package net.luis.agent.preload.type;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;

/**
 *
 * @author Luis-St
 *
 */

public enum ClassType {
	
	CLASS(0),
	ANNOTATION(Opcodes.ACC_ANNOTATION),
	INTERFACE(Opcodes.ACC_INTERFACE),
	ENUM(Opcodes.ACC_ENUM),
	RECORD(Opcodes.ACC_RECORD),
	MODULE(Opcodes.ACC_MODULE);
	
	private final int opcode;
	
	ClassType(int opcode) {
		this.opcode = opcode;
	}
	
	//region Static methods
	public static @NotNull ClassType fromAccess(int access) {
		for (ClassType type : values()) {
			if (type == CLASS) {
				continue;
			}
			if ((access & type.opcode) != 0) {
				return type;
			}
		}
		return CLASS;
	}
	//endregion
	
	public int getOpcode() {
		return this.opcode;
	}
}
