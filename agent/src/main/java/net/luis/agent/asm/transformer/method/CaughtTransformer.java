package net.luis.agent.asm.transformer.method;

import net.luis.agent.Agent;
import net.luis.agent.asm.base.*;
import net.luis.agent.asm.data.Annotation;
import net.luis.agent.asm.data.Method;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.asm.type.SignatureType;
import net.luis.agent.util.CaughtAction;
import net.luis.agent.util.factory.StringFactoryRegistry;
import org.jetbrains.annotations.NotNull;
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
		return Agent.getClass(type).getMethods().values().stream().noneMatch(method -> method.isAnnotatedWith(CAUGHT));
	}
	//endregion
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @NotNull ClassWriter writer) {
		return new MethodOnlyClassVisitor(writer, type, () -> this.modified = true) {
			
			@Override
			protected boolean isMethodValid(@NotNull Method method) {
				return super.isMethodValid(method) && method.isAnnotatedWith(CAUGHT);
			}
			
			@Override
			protected @NotNull MethodVisitor createMethodVisitor(@NotNull LocalVariablesSorter visitor, @NotNull Method method) {
				return new CaughtVisitor(visitor, method);
			}
		};
	}
	
	private static class CaughtVisitor extends LabelTrackingMethodVisitor {
		
		private static final String REPORT_CATEGORY = "Invalid Annotated Element";
		
		private final Label start = new Label();
		private final Label end = new Label();
		private final Label handler = new Label();
		private final CaughtAction action;
		private final Type exceptionType;
		private final Type returnType;
		
		private CaughtVisitor(@NotNull MethodVisitor visitor, @NotNull Method method) {
			super(visitor);
			this.method = method;
			Annotation annotation = method.getAnnotation(CAUGHT);
			this.action = CaughtAction.valueOf(annotation.getOrDefault("value"));
			this.exceptionType = annotation.getOrDefault("exceptionType");
			this.returnType = method.getReturnType();
			if (this.action == CaughtAction.NOTHING && !method.returns(VOID)) {
				throw CrashReport.create("Method annotated with @Caught(NOTHING) must return void", REPORT_CATEGORY).addDetail("Method", method.getSignature(SignatureType.DEBUG)).exception();
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
			} else if (this.action == CaughtAction.DEFAULT) {
				instrumentFactoryCall(this.mv, Type.getType(StringFactoryRegistry.class), this.returnType, "");
				this.mv.visitInsn(this.returnType.getOpcode(Opcodes.IRETURN));
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
