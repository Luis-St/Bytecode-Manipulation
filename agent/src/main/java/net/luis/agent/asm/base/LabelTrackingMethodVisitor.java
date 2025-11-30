package net.luis.agent.asm.base;

import org.jetbrains.annotations.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.LocalVariableNode;

import java.util.*;

/**
 *
 * @author Luis-St
 *
 */

public class LabelTrackingMethodVisitor extends MethodVisitor {

	private final List<Label> labels = new LinkedList<>();
	private final Set<Integer> inserts = new HashSet<>();
	private final List<Local> locals = new ArrayList<>();
	protected MethodNode method;

	public LabelTrackingMethodVisitor() {
		super(Opcodes.ASM9);
	}

	public LabelTrackingMethodVisitor(@NotNull MethodVisitor visitor) {
		super(Opcodes.ASM9, visitor);
	}

	public int getIndex(@NotNull Label label) {
		int index = this.labels.indexOf(label);
		if (index == -1) {
			throw new IllegalArgumentException("Trying to get index of a label that has not been visited yet");
		}
		return index;
	}

	public int getScopeIndex() {
		return this.labels.size() - 1;
	}

	public @NotNull Set<Integer> getInserts() {
		return this.inserts;
	}

	public void insertLabel(@NotNull Label label) {
		this.visitLabel(label);
		this.inserts.add(this.labels.size() - 1);
	}

	@Override
	@MustBeInvokedByOverriders
	public void visitLabel(@NotNull Label label) {
		this.labels.add(label);
		super.visitLabel(label);
	}

	public void visitLocalVariable(int index, @NotNull String name, @NotNull Type type, @Nullable String signature, @NotNull Label start, @NotNull Label end) {
		super.visitLocalVariable(name, type.getDescriptor(), signature, start, end, index);
		this.locals.add(new Local(index, name, type, signature, this.getIndex(start), this.getIndex(end)));
	}

	@Override
	@MustBeInvokedByOverriders
	public void visitEnd() {
		super.visitEnd();
		if (this.method != null && this.method.localVariables != null) {
			// Add new local variables to the method node
			for (Local local : this.locals) {
				LabelNode startLabel = new LabelNode(new Label());
				LabelNode endLabel = new LabelNode(new Label());
				LocalVariableNode localVar = new LocalVariableNode(
					local.name,
					local.type.getDescriptor(),
					local.signature,
					startLabel,
					endLabel,
					local.index
				);
				this.method.localVariables.add(localVar);
			}
		}
	}

	//region Internal
	private static record Local(int index, @NotNull String name, @NotNull Type type, @Nullable String signature, int start, int end) {}
	//endregion
}
