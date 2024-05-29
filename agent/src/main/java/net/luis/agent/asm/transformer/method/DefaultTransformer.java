package net.luis.agent.asm.transformer.method;

import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.visitor.ContextBasedMethodVisitor;
import net.luis.agent.asm.base.visitor.MethodOnlyClassVisitor;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.AgentContext;
import net.luis.agent.preload.data.*;
import net.luis.agent.preload.type.TypeAccess;
import net.luis.agent.preload.type.TypeModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.util.ArrayList;
import java.util.List;

import static net.luis.agent.asm.Instrumentations.*;
import static net.luis.agent.asm.Types.*;

/**
 *
 * @author Luis-St
 *
 */

public class DefaultTransformer extends BaseClassTransformer {
	
	public DefaultTransformer() {
		super(true);
	}
	
	//region Type filtering
	@Override
	protected boolean shouldIgnoreClass(@NotNull Type type) {
		ClassData data = AgentContext.get().getClassData(type);
		return data.getParameters().stream().noneMatch(parameter -> parameter.isAnnotatedWith(DEFAULT));
	}
	//endregion
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @Nullable Class<?> clazz, @NotNull ClassWriter writer) {
		return new MethodOnlyClassVisitor(writer, type, () -> this.modified = true) {
			
			@Override
			protected @NotNull MethodVisitor createMethodVisitor(@NotNull LocalVariablesSorter visitor, @NotNull MethodData method) {
				return new DefaultVisitor(visitor, method, this::markModified);
			}
		};
	}
	
	private static class DefaultVisitor extends ContextBasedMethodVisitor {
		
		private static final String REPORT_CATEGORY = "Invalid String Factory";
		
		private final List<ParameterData> lookup = new ArrayList<>();
		
		private DefaultVisitor(@NotNull MethodVisitor visitor, @NotNull MethodData method, @NotNull Runnable markModified) {
			super(visitor, method, markModified);
			method.parameters().stream().filter(parameter -> parameter.isAnnotatedWith(DEFAULT)).forEach(this.lookup::add);
		}
		
		@Override
		public void visitCode() {
			this.mv.visitCode();
			for (ParameterData parameter : this.lookup) {
				Label label = new Label();
				this.visitVarInsn(Opcodes.ALOAD, parameter);
				this.mv.visitJumpInsn(Opcodes.IFNONNULL, label);
				
				String value = parameter.getAnnotation(DEFAULT).getOrDefault("value");
				if (parameter.is(STRING)) {
					this.mv.visitLdcInsn(value);
				} else {
					instrumentFactoryCall(this.mv, this.getFactory(parameter), parameter.type(), value);
				}
				
				this.visitVarInsn(Opcodes.ASTORE, parameter);
				this.mv.visitJumpInsn(Opcodes.GOTO, label);
				this.mv.visitLabel(label);
				this.markModified();
			}
		}
		
		//region Helper methods
		private @NotNull Type getFactory(@NotNull ParameterData parameter) {
			AnnotationData annotation = parameter.getAnnotation(DEFAULT);
			Type factory = annotation.getOrDefault("factory");
			FieldData field = AgentContext.get().getClassData(factory).getField("INSTANCE");
			if (field == null) {
				throw CrashReport.create("Missing INSTANCE field in string factory class", REPORT_CATEGORY).addDetail("Factory", factory).exception();
			}
			if (!field.is(TypeAccess.PUBLIC, TypeModifier.STATIC, TypeModifier.FINAL)) {
				throw CrashReport.create("INSTANCE field in string factory class is not public static final", REPORT_CATEGORY).addDetail("Factory", factory).exception();
			}
			if (!field.is(factory)) {
				throw CrashReport.create("INSTANCE field in string factory class has invalid type", REPORT_CATEGORY).addDetail("Factory", factory)
					.addDetail("Expected Type", factory).addDetail("Actual Type", field.type()).exception();
			}
			return factory;
		}
		//endregion
	}
}
