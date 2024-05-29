package net.luis.agent.asm.base.visitor;

import net.luis.agent.preload.data.MethodData;
import net.luis.agent.preload.data.ParameterData;
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

public class ContextBasedMethodVisitor extends MethodVisitor {
	
	private final Runnable markModified;
	protected final MethodData method;
	private boolean skipAnnotation;
	
	public ContextBasedMethodVisitor(@NotNull MethodData method, @NotNull Runnable markModified) {
		super(Opcodes.ASM9);
		this.method = method;
		this.markModified = markModified;
	}
	
	public ContextBasedMethodVisitor(@NotNull MethodVisitor visitor, @NotNull MethodData method, @NotNull Runnable markModified) {
		super(Opcodes.ASM9, visitor);
		this.method = method;
		this.markModified = markModified;
	}
	
	//region Annotation skipping
	public @NotNull ContextBasedMethodVisitor skipAnnotation() {
		this.skipAnnotation = true;
		return this;
	}
	
	@Override
	public @Nullable AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		return this.skipAnnotation ? null : super.visitAnnotation(descriptor, visible);
	}
	
	@Override
	public @Nullable AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
		return this.skipAnnotation ? null : super.visitParameterAnnotation(parameter, descriptor, visible);
	}
	
	@Override
	public @Nullable AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
		return this.skipAnnotation ? null : super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
	}
	//endregion
	
	protected void visitVarInsn(int opcode, @NotNull ParameterData parameter) {
		this.mv.visitVarInsn(opcode, this.method.is(TypeModifier.STATIC) ? parameter.index() : parameter.index() + 1);
	}
	
	protected int newLocal(@NotNull Type type) {
		if (this.mv instanceof LocalVariablesSorter sorter) {
			return sorter.newLocal(type);
		}
		throw new IllegalStateException("LocalVariablesSorter is required as base visitor");
	}
	
	protected void markModified() {
		this.markModified.run();
	}
}
