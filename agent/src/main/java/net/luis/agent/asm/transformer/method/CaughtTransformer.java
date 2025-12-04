package net.luis.agent.asm.transformer.method;

import net.luis.agent.Agent;
import net.luis.agent.asm.ASMTreeUtils;
import net.luis.agent.asm.base.*;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.util.CaughtAction;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

import static net.luis.agent.asm.Instrumentations.*;
import static net.luis.agent.asm.Types.*;

/**
 *
 * @author Luis-St
 *
 */

public class CaughtTransformer extends BaseClassTransformer {

	public CaughtTransformer() {
		super(true);
	}

	//region Type filtering
	@Override
	protected boolean shouldIgnoreClass(@NotNull Type type) {
		// Check if any method in the class has @Caught annotation
		ClassNode classNode = Agent.getClass(type);
		if (classNode == null) {
			return true;
		}
		for (MethodNode method : classNode.methods) {
			if (ASMTreeUtils.hasAnnotation(method, CAUGHT)) {
				return false;
			}
		}
		return true;
	}
	//endregion

	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @NotNull ClassWriter writer) {
		return new MethodOnlyClassVisitor(writer, type, () -> this.modified = true) {

			@Override
			protected boolean isMethodValid(@NotNull MethodNode methodNode) {
				if (!super.isMethodValid(methodNode)) {
					return false;
				}
				return ASMTreeUtils.hasAnnotation(methodNode, CAUGHT);
			}

			@Override
			protected @NotNull MethodVisitor createMethodVisitor(@NotNull LocalVariablesSorter visitor, @NotNull MethodNode methodNode) {
				return new CaughtVisitor(visitor, type, methodNode);
			}
		};
	}

	private static class CaughtVisitor extends LabelTrackingMethodVisitor {

		private static final String REPORT_CATEGORY = "Invalid Annotated Element";

		private final Type ownerType;
		private final MethodNode methodNode;
		private final Label start = new Label();
		private final Label end = new Label();
		private final Label handler = new Label();
		private final CaughtAction action;
		private final Type exceptionType;
		private final Type returnType;

		private CaughtVisitor(@NotNull MethodVisitor visitor, @NotNull Type ownerType, @NotNull MethodNode methodNode) {
			super(visitor);
			this.ownerType = ownerType;
			this.methodNode = methodNode;
			AnnotationNode annotation = ASMTreeUtils.getAnnotation(methodNode, CAUGHT);
			// Enum values are stored as [descriptor, value] arrays in ASM
			Object valueEnum = ASMTreeUtils.getAnnotationValue(annotation, "value");
			if (valueEnum instanceof String[] && ((String[]) valueEnum).length >= 2) {
				this.action = CaughtAction.valueOf(((String[]) valueEnum)[1]);
			} else {
				this.action = CaughtAction.DEFAULT; // default value
			}
			Object exceptionTypeValue = ASMTreeUtils.getAnnotationValue(annotation, "exceptionType");
			this.exceptionType = exceptionTypeValue instanceof org.objectweb.asm.Type ? (org.objectweb.asm.Type) exceptionTypeValue : RUNTIME_EXCEPTION;
			this.returnType = ASMTreeUtils.getReturnType(methodNode);
			if (this.action == CaughtAction.NOTHING && !this.returnType.equals(VOID)) {
				throw CrashReport.create("Method annotated with @Caught(NOTHING) must return void", REPORT_CATEGORY).addDetail("Method", ASMTreeUtils.getDebugSignature(ownerType, methodNode)).exception();
			}
		}

		@Override
		public void visitCode() {
			this.mv.visitCode();
			this.mv.visitTryCatchBlock(this.start, this.end, this.handler, this.exceptionType.getInternalName());
			this.insertLabel(this.start);
		}

		@Override
		public void visitMaxs(int maxStack, int maxLocals) {}

		@Override
		public void visitEnd() {
			this.insertLabel(this.end);
			this.mv.visitJumpInsn(Opcodes.GOTO, this.handler);
			this.insertLabel(this.handler);
			int local = newLocal(this.mv, this.exceptionType);
			this.mv.visitVarInsn(Opcodes.ASTORE, local);

			if (this.action == CaughtAction.NOTHING) {
				this.mv.visitInsn(Opcodes.RETURN);
			} else if (this.action == CaughtAction.THROW) {
				instrumentThrownException(this.mv, RUNTIME_EXCEPTION, local);
			} else {
				loadDefaultConst(this.mv, this.returnType);
				this.mv.visitInsn(this.returnType.getOpcode(Opcodes.IRETURN));
			}
			this.visitLocalVariable(local, "e", this.exceptionType, null, this.start, this.end);
			this.mv.visitMaxs(0, 0);
			super.visitEnd();
		}
	}
}
