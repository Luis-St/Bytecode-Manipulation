package net.luis.agent.asm.base.visitor;

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
	
	protected BaseRecordComponentVisitor(RecordComponentVisitor visitor) {
		super(Opcodes.ASM9, visitor);
	}
}
