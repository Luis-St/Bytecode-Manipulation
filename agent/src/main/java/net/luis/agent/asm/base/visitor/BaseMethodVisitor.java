package net.luis.agent.asm.base.visitor;

import net.luis.agent.asm.Instrumentations;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;

public class BaseMethodVisitor extends MethodVisitor implements Instrumentations {
	
	private final Runnable markModified;
	private boolean skipAnnotation;
	
	public BaseMethodVisitor(@NotNull Runnable markModified) {
		super(Opcodes.ASM9);
		this.markModified = markModified;
	}
	
	public BaseMethodVisitor(@NotNull MethodVisitor visitor, @NotNull Runnable markModified) {
		super(Opcodes.ASM9, visitor);
		this.markModified = markModified;
	}
	
	//region Annotation skipping
	public @NotNull BaseMethodVisitor skipAnnotation() {
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
