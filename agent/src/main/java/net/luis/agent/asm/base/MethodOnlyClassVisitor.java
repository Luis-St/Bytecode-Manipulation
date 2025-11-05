package net.luis.agent.asm.base;

import net.luis.agent.Agent;
import net.luis.agent.asm.ASMTreeUtils;
import net.luis.agent.asm.type.TypeModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 *
 * @author Luis-St
 *
 */

public class MethodOnlyClassVisitor extends ContextBasedClassVisitor {

	public MethodOnlyClassVisitor(@NotNull Type type, @NotNull Runnable markModified) {
		super(type, markModified);
	}

	public MethodOnlyClassVisitor(@NotNull ClassVisitor visitor, @NotNull Type type, @NotNull Runnable markModified) {
		super(visitor, type, markModified);
	}

	protected boolean isMethodValid(@NotNull MethodNode method) {
		return !ASMTreeUtils.is(method, TypeModifier.ABSTRACT);
	}

	protected @NotNull MethodVisitor createMethodVisitor(@NotNull LocalVariablesSorter visitor, @NotNull MethodNode method) {
		return visitor;
	}

	@Override
	public @NotNull MethodVisitor visitMethod(int access, @NotNull String name, @NotNull String descriptor, @Nullable String signature, String @Nullable [] exceptions) {
		ClassNode classNode = Agent.getClass(this.type);
		MethodNode method = ASMTreeUtils.getMethod(classNode, name + descriptor);
		MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
		if (method == null) {
			return visitor;
		}
		if (this.isMethodValid(method)) {
			LocalVariablesSorter sorter = new LocalVariablesSorter(access, descriptor, visitor);
			this.markModified();
			return this.createMethodVisitor(sorter, method);
		}
		return visitor;
	}
}
