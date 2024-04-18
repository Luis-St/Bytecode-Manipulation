package net.luis.asm.base.visitor;

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
	
	protected BaseRecordComponentVisitor(RecordComponentVisitor recordComponentVisitor) {
		super(Opcodes.ASM9, recordComponentVisitor);
	}
}
