package net.luis.agent.asm.transformer.method;

import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.visitor.ContextBasedMethodVisitor;
import net.luis.agent.asm.base.visitor.MethodOnlyClassVisitor;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.AgentContext;
import net.luis.agent.preload.data.*;
import net.luis.agent.util.CaughtAction;
import net.luis.agent.util.DefaultStringFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;

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
		ClassData data = AgentContext.get().getClassData(type);
		return data.methods().stream().noneMatch(method -> method.isAnnotatedWith(CAUGHT));
	}
	//endregion
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @Nullable Class<?> clazz, @NotNull ClassWriter writer) {
		return new MethodOnlyClassVisitor(writer, type, () -> this.modified = true) {
			
			@Override
			protected boolean isMethodValid(@NotNull MethodData method) {
				return super.isMethodValid(method) && method.isAnnotatedWith(CAUGHT);
			}
			
			@Override
			protected @NotNull MethodVisitor createMethodVisitor(@NotNull LocalVariablesSorter visitor, @NotNull MethodData method) {
				return new CaughtVisitor(visitor, method, this::markModified);
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
		
		private CaughtVisitor(@NotNull MethodVisitor visitor, @NotNull MethodData method, @NotNull Runnable markModified) {
			super(visitor, method, markModified);
			AnnotationData annotation = method.getAnnotation(CAUGHT);
			this.action = CaughtAction.valueOf(annotation.getOrDefault("value"));
			this.exceptionType = annotation.getOrDefault("exceptionType");
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
		public void visitMaxs(int maxStack, int maxLocals) {}
		
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
				instrumentThrownException(this.mv, RUN_EX, local);
			} else if (this.action == CaughtAction.DEFAULT) {
				Type type = this.method.getReturnType();
				instrumentFactoryCall(this.mv, Type.getType(DefaultStringFactory.class), type, "");
				this.mv.visitInsn(type.getOpcode(Opcodes.IRETURN));
			} else {
				Type type = this.method.getReturnType();
				loadDefaultConst(this.mv, type);
				this.mv.visitInsn(type.getOpcode(Opcodes.IRETURN));
			}
			this.mv.visitMaxs(0, 0);
			this.mv.visitEnd();
			this.markModified();
		}
	}
}
