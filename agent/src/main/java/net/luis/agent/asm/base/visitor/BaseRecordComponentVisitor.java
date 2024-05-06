package net.luis.agent.asm.base.visitor;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.RecordComponentVisitor;

/**
 *
 * @author Luis-St
 *
 */

public class BaseRecordComponentVisitor extends RecordComponentVisitor {
	
	protected BaseRecordComponentVisitor() {
		super(Opcodes.ASM9);
	}
	
	protected BaseRecordComponentVisitor(@NotNull RecordComponentVisitor visitor) {
		super(Opcodes.ASM9, visitor);
	}
}
