package net.luis.agent.asm.transformer.method;

import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.visitor.ContextBasedMethodVisitor;
import net.luis.agent.asm.base.visitor.MethodOnlyClassVisitor;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.preload.PreloadContext;
import net.luis.agent.preload.data.*;
import net.luis.agent.util.CaughtAction;
import net.luis.agent.util.DefaultStringFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;

import static net.luis.agent.asm.Types.*;

/**
 *
 * @author Luis-St
 *
 */

public class CaughtTransformer extends BaseClassTransformer {
	
	public CaughtTransformer(@NotNull PreloadContext context) {
		super(context);
	}
	
	//region Type filtering
	@Override
	protected boolean shouldTransform(@NotNull Type type) {
		ClassContent content = this.context.getClassContent(type);
		return content.methods().stream().anyMatch(method -> method.isAnnotatedWith(CAUGHT));
	}
	//endregion
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @Nullable Class<?> clazz, @NotNull ClassWriter writer) {
		return new MethodOnlyClassVisitor(writer, this.context, type, () -> this.modified = true) {
			
			@Override
			protected boolean isMethodValid(@NotNull MethodData method) {
				return super.isMethodValid(method) && method.isAnnotatedWith(CAUGHT);
			}
			
			@Override
			protected @NotNull MethodVisitor createMethodVisitor(@NotNull LocalVariablesSorter visitor, @NotNull MethodData method) {
				return new CaughtVisitor(visitor, this.context, this.type, method, this::markModified);
			}
		};
	}
	
	private static class CaughtVisitor extends ContextBasedMethodVisitor {
		
		private static final String REPORT_CATEGORY = "Invalid Annotated Element";
		private static final Type RUN_EX = Type.getType(RuntimeException.class);
		
		private final Label start = new Label();
		private final Label end = new Label();
		private final Label handler = new Label();
		private final CaughtAction action;
		private final Type exceptionType;
		
		private CaughtVisitor(@NotNull MethodVisitor visitor, @NotNull PreloadContext context, @NotNull Type type, @NotNull MethodData method, @NotNull Runnable markModified) {
			super(visitor, context, type, method, markModified);
			AnnotationData annotation = method.getAnnotation(CAUGHT);
			this.action = CaughtAction.valueOf(annotation.getOrDefault(context, "value"));
			this.exceptionType = annotation.getOrDefault(context, "exceptionType");
			if (this.action == CaughtAction.NOTHING && !method.returns(VOID)) {
				throw CrashReport.create("Method annotated with @Caught(NOTHING) must return void", REPORT_CATEGORY).addDetail("Method", method.getMethodSignature()).exception();
			}
		}
		
		@Override
		public void visitCode() {
			this.mv.visitCode();
			this.mv.visitTryCatchBlock(this.start, this.end, this.handler, this.exceptionType.getInternalName());
			this.mv.visitLabel(this.start);
		}
		
		@Override
		public void visitEnd() {
			this.mv.visitLabel(this.end);
			this.mv.visitJumpInsn(Opcodes.GOTO, this.handler);
			this.mv.visitLabel(this.handler);
			int local = this.newLocal(this.exceptionType);
			this.mv.visitVarInsn(Opcodes.ASTORE, local);
			
			if (this.action == CaughtAction.NOTHING) {
				this.mv.visitInsn(Opcodes.RETURN);
			} else if (this.action == CaughtAction.THROW) {
				this.instrumentThrownException(this.mv, RUN_EX, local);
			} else if (this.action == CaughtAction.DEFAULT) {
				Type type = this.method.getReturnType();
				this.instrumentFactoryCall(this.mv, Type.getType(DefaultStringFactory.class), type, "");
				this.mv.visitInsn(type.getOpcode(Opcodes.IRETURN));
			} else {
				Type type = this.method.getReturnType();
				this.loadDefaultConst(this.mv, type);
				this.mv.visitInsn(type.getOpcode(Opcodes.IRETURN));
			}
			this.mv.visitEnd();
			this.markModified();
		}
	}
}