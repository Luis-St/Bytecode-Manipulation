package net.luis.agent.asm.transformer.method;

import net.luis.agent.AgentContext;
import net.luis.agent.asm.ASMUtils;
import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.visitor.ContextBasedMethodVisitor;
import net.luis.agent.asm.base.visitor.MethodOnlyClassVisitor;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.preload.data.*;
import net.luis.agent.preload.type.MethodType;
import net.luis.agent.preload.type.TypeModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.util.*;

import static net.luis.agent.asm.Instrumentations.*;
import static net.luis.agent.asm.Types.*;

/**
 *
 * @author Luis-St
 *
 */

public class PatternTransformer extends BaseClassTransformer {
	
	public PatternTransformer() {
		super(true);
	}
	
	//region Type filtering
	@Override
	protected boolean shouldIgnoreClass(@NotNull Type type) {
		ClassData data = AgentContext.get().getClassData(type);
		return data.getParameters().stream().noneMatch(parameter -> parameter.isAnnotatedWith(PATTERN)) && data.methods().stream().noneMatch(method -> method.returns(STRING) && method.isAnnotatedWith(PATTERN));
	}
	//endregion
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @Nullable Class<?> clazz, @NotNull ClassWriter writer) {
		return new MethodOnlyClassVisitor(writer, type, () -> this.modified = true) {
			
			@Override
			protected @NotNull MethodVisitor createMethodVisitor(@NotNull LocalVariablesSorter visitor, @NotNull MethodData method) {
				return new PatternVisitor(visitor, method, this::markModified);
			}
		};
	}
	
	private static class PatternVisitor extends ContextBasedMethodVisitor {
		
		private static final Type ILL_ARG = Type.getType(IllegalArgumentException.class);
		private static final String REPORT_CATEGORY = "Invalid Annotated Element";
		
		private final List<ParameterData> lookup = new ArrayList<>();
		
		private PatternVisitor(@NotNull LocalVariablesSorter visitor, @NotNull MethodData method, @NotNull Runnable markModified) {
			super(visitor, method, markModified);
			method.parameters().stream().filter(parameter -> parameter.isAnnotatedWith(PATTERN)).forEach(this.lookup::add);
		}
		
		@Override
		public void visitCode() {
			this.mv.visitCode();
			boolean isStatic = this.method.is(TypeModifier.STATIC);
			for (ParameterData parameter : this.lookup) {
				Label label = new Label();
				String value = Objects.requireNonNull(parameter.getAnnotation(PATTERN).get("value"));
				
				instrumentPatternCheck(this.mv, value, isStatic ? parameter.index() : parameter.index() + 1, label);
				instrumentThrownException(this.mv, ILL_ARG, parameter.getMessageName() + " must match pattern '" + value + "'");
				
				this.mv.visitJumpInsn(Opcodes.GOTO, label);
				this.mv.visitLabel(label);
				this.markModified();
			}
		}
		
		@Override
		public void visitInsn(int opcode) {
			if (opcode == Opcodes.ARETURN && this.method.isAnnotatedWith(PATTERN)) {
				this.validateMethod();
				String value = Objects.requireNonNull(this.method.getAnnotation(PATTERN).get("value"));
				Label start = new Label();
				Label end = new Label();
				int local = this.newLocal(this.method.getReturnType());
				this.mv.visitLabel(start);
				this.mv.visitVarInsn(Opcodes.ASTORE, local);
				
				instrumentPatternCheck(this.mv, value, local, end);
				instrumentThrownException(this.mv, ILL_ARG, "Method " + ASMUtils.getSimpleName(this.method.owner()) + "#" + this.method.name() + " return value must match pattern '" + value + "'");
				
				this.mv.visitJumpInsn(Opcodes.GOTO, end);
				this.mv.visitLabel(end);
				this.mv.visitVarInsn(Opcodes.ALOAD, local);
				this.mv.visitLocalVariable("generated$PatternTransformer$Temp" + local, STRING.getDescriptor(), null, start, end, local);
				this.markModified();
			}
			this.mv.visitInsn(opcode);
		}
		
		//region Validation
		private void validateMethod() {
			if (!this.method.is(MethodType.METHOD)) {
				throw CrashReport.create("Annotation @Pattern can not be applied to constructors and static initializers", REPORT_CATEGORY).addDetail("Method", this.method.name()).exception();
			}
			if (!this.method.returns(STRING)) {
				throw CrashReport.create("Method annotated with @Pattern must return a String", REPORT_CATEGORY).addDetail("Method", this.method.getMethodSignature())
					.addDetail("Return Type", this.method.type().getReturnType()).exception();
			}
		}
		//endregion
	}
}
