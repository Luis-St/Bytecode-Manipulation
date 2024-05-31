package net.luis.agent.asm.base;

import net.luis.agent.AgentContext;
import net.luis.agent.asm.data.Class;
import net.luis.agent.asm.data.Method;
import net.luis.agent.asm.type.TypeModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;

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
	
	protected boolean isMethodValid(@NotNull Method method) {
		return !method.is(TypeModifier.ABSTRACT);
	}
	
	protected @NotNull MethodVisitor createMethodVisitor(@NotNull LocalVariablesSorter visitor, @NotNull Method method) {
		return visitor;
	}
	
	@Override
	public @NotNull MethodVisitor visitMethod(int access, @NotNull String name, @NotNull String descriptor, @Nullable String signature, String @Nullable [] exceptions) {
		Class data = AgentContext.get().getClassData(this.type);
		Method method = data.getMethod(name + descriptor);
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
