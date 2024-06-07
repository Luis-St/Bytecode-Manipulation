package net.luis.agent.asm.base;

import net.luis.agent.asm.data.LocalVariable;
import net.luis.agent.asm.data.Method;
import org.jetbrains.annotations.*;
import org.objectweb.asm.*;

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
	private Method method;
	
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
	
	public void setMethod(@NotNull Method method) {
		this.method = method;
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
		if (this.method != null) {
			this.method.updateLocalScopes(this.getInserts());
			for (Local local : this.locals) {
				this.method.getLocals().add(LocalVariable.builder(this.method, local.index, local.name, local.type).genericSignature(local.signature).bounds(local.start, local.end).build());
			}
		}
	}
	
	//region Internal
	private static record Local(int index, @NotNull String name, @NotNull Type type, @Nullable String signature, int start, int end) {}
	//endregion
}
