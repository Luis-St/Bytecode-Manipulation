package net.luis.agent.asm.base.visitor;

import net.luis.agent.preload.PreloadContext;
import net.luis.agent.preload.data.ClassContent;
import net.luis.agent.preload.data.MethodData;
import net.luis.agent.preload.type.TypeModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;

/**
 *
 * @author Luis-St
 *
 */

public class MethodOnlyClassVisitor extends BaseClassVisitor {
	
	public MethodOnlyClassVisitor(@NotNull PreloadContext context, @NotNull Type type, @NotNull Runnable markModified) {
		super(context, type, markModified);
	}
	
	public MethodOnlyClassVisitor(@NotNull ClassVisitor visitor, @NotNull PreloadContext context, @NotNull Type type, @NotNull Runnable markModified) {
		super(visitor, context, type, markModified);
	}
	
	protected boolean isMethodValid(@NotNull MethodData method) {
		return !method.is(TypeModifier.ABSTRACT);
	}
	
	protected @NotNull MethodVisitor createMethodVisitor(@NotNull LocalVariablesSorter visitor, @NotNull MethodData method) {
		return visitor;
	}
	
	@Override
	public @NotNull MethodVisitor visitMethod(int access, @NotNull String name, @NotNull String descriptor, @Nullable String signature, String @Nullable [] exceptions) {
		ClassContent content = this.context.getClassContent(this.type);
		MethodData method = content.getMethod(name, Type.getType(descriptor));
		MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
		if (method == null) {
			return visitor;
		}
		if (this.isMethodValid(method)) {
			LocalVariablesSorter sorter = new LocalVariablesSorter(access, descriptor, visitor);
			return this.createMethodVisitor(sorter, method);
		}
		return visitor;
	}
}
