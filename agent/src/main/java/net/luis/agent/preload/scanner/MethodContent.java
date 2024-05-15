package net.luis.agent.preload.scanner;

import net.luis.agent.preload.scanner.instructions.ASMInstruction;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.MethodVisitor;

import java.util.LinkedList;

/**
 *
 * @author Luis-St
 *
 */

public record MethodContent(@NotNull LinkedList<ASMInstruction> instructions) {
	
	public void copyTo(@NotNull MethodVisitor visitor) {
		this.instructions.forEach(instruction -> instruction.copyTo(visitor));
	}
}
