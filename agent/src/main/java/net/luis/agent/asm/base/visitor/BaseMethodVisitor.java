package net.luis.agent.asm.base.visitor;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class BaseMethodVisitor extends MethodVisitor {
	
	public BaseMethodVisitor() {
		super(Opcodes.ASM9);
	}
	
	public BaseMethodVisitor(@NotNull MethodVisitor methodVisitor) {
		super(Opcodes.ASM9, methodVisitor);
	}
}
