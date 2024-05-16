package net.luis.agent.asm.base.visitor;

import net.luis.agent.asm.Instrumentations;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

/**
 *
 * @author Luis-St
 *
 */

public class BaseMethodVisitor extends MethodVisitor implements Instrumentations {
	
	private boolean skipAnnotation = false;
	
	protected BaseMethodVisitor() {
		super(Opcodes.ASM9);
	}
	
	public BaseMethodVisitor(@NotNull MethodVisitor visitor) {
		super(Opcodes.ASM9, visitor);
	}
	
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
}
