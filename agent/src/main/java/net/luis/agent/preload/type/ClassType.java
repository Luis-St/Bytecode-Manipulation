package net.luis.agent.preload.type;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;

/**
 *
 * @author Luis-St
 *
 */

public enum ClassType {
	
	CLASS(-1),
	ANNOTATION(Opcodes.ACC_ANNOTATION),
	INTERFACE(Opcodes.ACC_INTERFACE),
	ENUM(Opcodes.ACC_ENUM),
	RECORD(Opcodes.ACC_RECORD),
	MODULE(Opcodes.ACC_MODULE);
	
	private final int value;
	
	ClassType(int value) {
		this.value = value;
	}
	
	public static @NotNull ClassType fromAccess(int access) {
		for (ClassType type : values()) {
			if (type == CLASS) {
				continue;
			}
			if ((access & type.value) != 0) {
				return type;
			}
		}
		return CLASS;
	}
}
