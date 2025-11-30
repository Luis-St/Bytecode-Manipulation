package net.luis.agent.asm.transformer.method;

import net.luis.agent.Agent;
import net.luis.agent.asm.ASMTreeUtils;
import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.ContextBasedClassVisitor;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.asm.type.MethodType;
import net.luis.agent.asm.type.TypeModifier;
import net.luis.agent.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.Map;

import static net.luis.agent.asm.Instrumentations.*;
import static net.luis.agent.asm.Types.*;

/**
 *
 * @author Luis-St
 *
 */

public class AsyncTransformer extends BaseClassTransformer {

	public AsyncTransformer() {
		super(true);
	}

	//region Type filtering
	@Override
	protected boolean shouldIgnoreClass(@NotNull Type type) {
		ClassNode classNode = Agent.getClass(type);
		if (classNode == null) {
			return true;
		}
		for (MethodNode method : classNode.methods) {
			if (ASMTreeUtils.hasAnnotation(method, ASYNC)) {
				return false;
			}
		}
		return true;
	}
	//endregion

	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @NotNull ClassWriter writer) {
		return new AsyncClassVisitor(writer, type, () -> this.modified = true);
	}

	private static class AsyncClassVisitor extends ContextBasedClassVisitor {

		private static final String REPORT_CATEGORY = "Invalid Annotated Element";

		private final Map<MethodNode, String> methods = new HashMap<>();
		private final ClassNode classNode;

		private AsyncClassVisitor(@NotNull ClassVisitor visitor, @NotNull Type type, @NotNull Runnable markModified) {
			super(visitor, type, markModified);
			this.classNode = Agent.getClass(type);
		}

		@Override
		public @NotNull MethodVisitor visitMethod(int access, @NotNull String name, @NotNull String descriptor, @Nullable String signature, String @Nullable [] exceptions) {
			MethodNode methodNode = ASMTreeUtils.getMethod(this.classNode, name + descriptor);
			if (methodNode == null || ASMTreeUtils.is(methodNode, TypeModifier.ABSTRACT) || !ASMTreeUtils.hasAnnotation(methodNode, ASYNC)) {
				return super.visitMethod(access, name, descriptor, signature, exceptions);
			}
			//region Validation
			if (!ASMTreeUtils.is(methodNode, MethodType.METHOD)) {
				throw CrashReport.create("Annotation @Async can not be applied to constructors and static initializers", REPORT_CATEGORY)
					.addDetail("Method", ASMTreeUtils.getDebugSignature(this.type, methodNode)).exception();
			}
			if (!ASMTreeUtils.returns(methodNode, VOID)) {
				throw CrashReport.create("Method annotated with @Async must return void", REPORT_CATEGORY)
					.addDetail("Method", ASMTreeUtils.getDebugSignature(this.type, methodNode))
					.addDetail("Return Type", ASMTreeUtils.getReturnType(methodNode)).exception();
			}
			if (ASMTreeUtils.getExceptionCount(methodNode) > 0) {
				throw CrashReport.create("Method annotated with @Async must not throw exceptions", REPORT_CATEGORY)
					.addDetail("Method", ASMTreeUtils.getDebugSignature(this.type, methodNode))
					.addDetail("Exceptions", ASMTreeUtils.getExceptions(methodNode)).exception();
			}
			if (ASMTreeUtils.hasAnnotation(methodNode, SCHEDULED)) {
				throw CrashReport.create("Method annotated with @Async must not be annotated with @Scheduled", REPORT_CATEGORY)
					.addDetail("Method", ASMTreeUtils.getDebugSignature(this.type, methodNode)).exception();
			}
			//endregion
			access = access & ~ASMTreeUtils.getAccess(methodNode).getOpcode();
			String newName = "generated$" + Utils.capitalize(name) + "$Async";
			this.methods.put(methodNode, newName);
			MethodVisitor visitor = super.visitMethod(access | Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC, newName, descriptor, signature, exceptions);
			return new MethodVisitor(Opcodes.ASM9, visitor) {
				//region Implementation
				@Override
				public @Nullable AnnotationVisitor visitAnnotation(@NotNull String annotationDescriptor, boolean visible) {
					return null;
				}

				@Override
				public @Nullable AnnotationVisitor visitParameterAnnotation(int parameter, @NotNull String annotationDescriptor, boolean visible) {
					return null;
				}

				@Override
				public @Nullable AnnotationVisitor visitTypeAnnotation(int typeRef, @Nullable TypePath typePath, @NotNull String annotationDescriptor, boolean visible) {
					return null;
				}
				//endregion
			};
		}

		@Override
		public void visitEnd() {
			for (Map.Entry<MethodNode, String> entry : this.methods.entrySet()) {
				MethodNode methodNode = entry.getKey();
				MethodVisitor visitor = super.visitMethod(methodNode.access, methodNode.name, methodNode.desc, methodNode.signature, null);
				visitor.visitCode();
				this.instrumentMethodAnnotations(visitor, methodNode);
				this.instrumentParameterAnnotations(visitor, methodNode);
				//region Parameter loading
				int index = 0;
				if (!ASMTreeUtils.is(methodNode, TypeModifier.STATIC)) {
					visitor.visitVarInsn(Opcodes.ALOAD, index++);
				}
				Type[] paramTypes = ASMTreeUtils.getParameterTypes(methodNode);
				for (Type paramType : paramTypes) {
					visitor.visitVarInsn(paramType.getOpcode(Opcodes.ILOAD), index);
					index += paramType.getSize();
				}
				//endregion
				visitor.visitInvokeDynamicInsn("run", this.makeDescriptor(methodNode), METAFACTORY_HANDLE, VOID_METHOD, this.createHandle(methodNode, entry.getValue()), VOID_METHOD);
				visitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/concurrent/CompletableFuture", "runAsync", "(Ljava/lang/Runnable;)Ljava/util/concurrent/CompletableFuture;", false);
				visitor.visitInsn(Opcodes.POP);
				visitor.visitInsn(Opcodes.RETURN);
				visitor.visitMaxs(0, 0);
				visitor.visitEnd();
				this.markModified();
			}
			super.visitEnd();
		}

		//region Helper methods
		private void instrumentMethodAnnotations(@NotNull MethodVisitor visitor, @NotNull MethodNode methodNode) {
			for (AnnotationNode annotation : ASMTreeUtils.getAllAnnotations(methodNode)) {
				visitor.visitAnnotation(annotation.desc, true);
			}
		}

		private void instrumentParameterAnnotations(@NotNull MethodVisitor visitor, @NotNull MethodNode methodNode) {
			if (methodNode.visibleParameterAnnotations != null) {
				for (int i = 0; i < methodNode.visibleParameterAnnotations.length; i++) {
					if (methodNode.visibleParameterAnnotations[i] != null) {
						for (AnnotationNode annotation : methodNode.visibleParameterAnnotations[i]) {
							visitor.visitParameterAnnotation(i, annotation.desc, true);
						}
					}
				}
			}
			if (methodNode.invisibleParameterAnnotations != null) {
				for (int i = 0; i < methodNode.invisibleParameterAnnotations.length; i++) {
					if (methodNode.invisibleParameterAnnotations[i] != null) {
						for (AnnotationNode annotation : methodNode.invisibleParameterAnnotations[i]) {
							visitor.visitParameterAnnotation(i, annotation.desc, false);
						}
					}
				}
			}
		}

		private @NotNull String makeDescriptor(@NotNull MethodNode methodNode) {
			StringBuilder builder = new StringBuilder();
			builder.append("(");
			if (!ASMTreeUtils.is(methodNode, TypeModifier.STATIC)) {
				builder.append(this.type.getDescriptor());
			}
			Type[] paramTypes = ASMTreeUtils.getParameterTypes(methodNode);
			for (Type paramType : paramTypes) {
				builder.append(paramType.getDescriptor());
			}
			builder.append(")Ljava/lang/Runnable;");
			return builder.toString();
		}

		private @NotNull Handle createHandle(@NotNull MethodNode methodNode, String newName) {
			return new Handle(ASMTreeUtils.is(methodNode, TypeModifier.STATIC) ? Opcodes.H_INVOKESTATIC : Opcodes.H_INVOKEVIRTUAL, this.type.getInternalName(), newName, methodNode.desc, false);
		}
		//endregion
	}
}
