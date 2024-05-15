package net.luis.agent.preload.scanner;

import net.luis.agent.asm.base.visitor.BaseClassVisitor;
import net.luis.agent.asm.base.visitor.BaseMethodVisitor;
import net.luis.agent.preload.PreloadContext;
import net.luis.agent.preload.data.MethodData;
import net.luis.agent.preload.scanner.instructions.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.util.LinkedList;

/**
 *
 * @author Luis-St
 *
 */

public class MethodContentScanner extends ClassVisitor {
	
	private final LinkedList<ASMInstruction> instructions = new LinkedList<>();
	private final MethodData method;
	
	public MethodContentScanner(@NotNull MethodData method) {
		super(Opcodes.ASM9);
		this.method = method;
	}
	
	@Override
	public MethodVisitor visitMethod(int access, @NotNull String name, @NotNull String descriptor, @Nullable String signature, String @Nullable [] exceptions) {
		if (!name.equals(this.method.name()) && descriptor.equals(this.method.type().getDescriptor())) {
			return super.visitMethod(access, name, descriptor, signature, exceptions);
		}
		return new MethodInstructionScanner(this.instructions);
	}
	
	public @NotNull MethodContent getMethodContent() {
		return new MethodContent(this.instructions);
	}
	
	//region Scanner
	private static class MethodInstructionScanner extends BaseMethodVisitor {
		
		private final LinkedList<ASMInstruction> instructions;
		
		private MethodInstructionScanner(LinkedList<ASMInstruction> instructions) {
			this.instructions = instructions;
		}
		
		@Override
		public void visitFrame(int type, int numLocal, Object @NotNull [] local, int numStack, Object @NotNull [] stack) {
			this.instructions.add(new FrameInstruction(type, numLocal, local, numStack, stack));
		}
		
		@Override
		public void visitInsn(int opcode) {
			this.instructions.add(new Instruction(opcode));
		}
		
		@Override
		public void visitIntInsn(int opcode, int operand) {
			this.instructions.add(new IntInstruction(opcode, operand));
		}
		
		@Override
		public void visitVarInsn(int opcode, int varIndex) {
			this.instructions.add(new VarInstruction(opcode, varIndex));
		}
		
		@Override
		public void visitTypeInsn(int opcode, @NotNull String type) {
			this.instructions.add(new TypeInstruction(opcode, type));
		}
		
		@Override
		public void visitFieldInsn(int opcode, @NotNull String owner, @NotNull String name, @NotNull String descriptor) {
			this.instructions.add(new FieldInstruction(opcode, owner, name, descriptor));
		}
		
		@Override
		public void visitMethodInsn(int opcode, @NotNull String owner, @NotNull String name, @NotNull String descriptor, boolean isInterface) {
			this.instructions.add(new MethodInstruction(opcode, owner, name, descriptor, isInterface));
		}
		
		@Override
		public void visitInvokeDynamicInsn(@NotNull String name, @NotNull String descriptor, @NotNull Handle handle, Object @NotNull ... arguments) {
			this.instructions.add(new InvokeDynamicInstruction(name, descriptor, handle, arguments));
		}
		
		@Override
		public void visitJumpInsn(int opcode, @NotNull Label label) {
			this.instructions.add(new JumpInstruction(opcode, label));
		}
		
		@Override
		public void visitLabel(@NotNull Label label) {
			this.instructions.add(new LabelInstruction(label));
		}
		
		@Override
		public void visitLdcInsn(@NotNull Object value) {
			this.instructions.add(new LdcInstruction(value));
		}
		
		@Override
		public void visitIincInsn(int varIndex, int increment) {
			this.instructions.add(new IincInstruction(varIndex, increment));
		}
		
		@Override
		public void visitTableSwitchInsn(int min, int max, @NotNull Label dflt, Label @NotNull ... labels) {
			this.instructions.add(new TableSwitchInstruction(min, max, dflt, labels));
		}
		
		@Override
		public void visitLookupSwitchInsn(@NotNull Label dflt, int[] keys, Label @NotNull [] labels) {
			this.instructions.add(new LookupSwitchInstruction(dflt, keys, labels));
		}
		
		@Override
		public void visitMultiANewArrayInsn(@NotNull String descriptor, int numDimensions) {
			this.instructions.add(new MultiNewArrayInstruction(descriptor, numDimensions));
		}
		
		@Override
		public void visitTryCatchBlock(@NotNull Label start, @NotNull Label end, @NotNull Label handler, @NotNull String type) {
			this.instructions.add(new TryCatchBlockInstruction(start, end, handler, type));
		}
		
		@Override
		public void visitLocalVariable(@NotNull String name, @NotNull String descriptor, @NotNull String signature, @NotNull Label start, @NotNull Label end, int index) {
			this.instructions.add(new LocalVariableInstruction(name, descriptor, signature, start, end, index));
		}
		
		@Override
		public void visitMaxs(int maxStack, int maxLocals) {
			this.instructions.add(new MaxsInstruction(maxStack, maxLocals));
		}
	}
	//endregion
}
