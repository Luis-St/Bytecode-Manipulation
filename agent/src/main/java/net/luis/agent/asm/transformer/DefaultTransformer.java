package net.luis.agent.asm.transformer;

import net.luis.agent.Agent;
import net.luis.agent.asm.base.*;
import net.luis.agent.asm.data.*;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.asm.type.*;
import org.jetbrains.annotations.NotNull;
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
		return Agent.getClass(type).getParameters().stream().noneMatch(parameter -> parameter.isAnnotatedWith(DEFAULT));
	}
	//endregion
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @NotNull ClassWriter writer) {
		return new MethodOnlyClassVisitor(writer, type, () -> this.modified = true) {
			
			@Override
			protected boolean isMethodValid(@NotNull Method method) {
				return super.isMethodValid(method) && method.getParameters().values().stream().anyMatch(parameter -> parameter.isAnnotatedWith(DEFAULT));
			}
			
			@Override
			protected @NotNull MethodVisitor createMethodVisitor(@NotNull LocalVariablesSorter visitor, @NotNull Method method) {
				return new DefaultVisitor(visitor, method);
			}
		};
	}
	
	private static class DefaultVisitor extends LabelTrackingMethodVisitor {
		
		private static final String REPORT_CATEGORY = "Invalid String Factory";
		
		private final List<Parameter> lookup = new ArrayList<>();
		
		private DefaultVisitor(@NotNull MethodVisitor visitor, @NotNull Method method) {
			super(visitor);
			this.method = method;
			method.getParameters().values().stream().filter(parameter -> parameter.isAnnotatedWith(DEFAULT)).forEach(this.lookup::add);
		}
		
		@Override
		public void visitCode() {
			this.mv.visitCode();
			for (Parameter parameter : this.lookup) {
				Label label = new Label();
				this.visitVarInsn(Opcodes.ALOAD, parameter.getLoadIndex());
				this.mv.visitJumpInsn(Opcodes.IFNONNULL, label);
				
				String value = parameter.getAnnotation(DEFAULT).getOrDefault("value");
				if (parameter.is(STRING)) {
					this.mv.visitLdcInsn(value);
				} else {
					String classSignature = Agent.getClass(this.method.getOwner()).getSignature(SignatureType.GENERIC);
					String methodSignature = this.method.getSignature(SignatureType.GENERIC);
					instrumentFactoryCall(this.mv, this.getFactory(parameter), parameter.getType(), classSignature, methodSignature, parameter.getIndex(), value);
				}
				
				this.visitVarInsn(Opcodes.ASTORE, parameter.getLoadIndex());
				this.mv.visitJumpInsn(Opcodes.GOTO, label);
				this.insertLabel(label);
			}
		}
		
		//region Helper methods
		private @NotNull Type getFactory(@NotNull Parameter parameter) {
			Annotation annotation = parameter.getAnnotation(DEFAULT);
			Type factory = annotation.getOrDefault("factory");
			Field field = Agent.getClass(factory).getField("INSTANCE");
			if (field == null) {
				throw CrashReport.create("Missing INSTANCE field in string factory class", REPORT_CATEGORY).addDetail("Factory", factory).exception();
			}
			if (!field.is(TypeAccess.PUBLIC, TypeModifier.STATIC, TypeModifier.FINAL)) {
				throw CrashReport.create("INSTANCE field in string factory class is not public static final", REPORT_CATEGORY).addDetail("Factory", factory).exception();
			}
			if (!field.is(factory)) {
				throw CrashReport.create("INSTANCE field in string factory class has invalid type", REPORT_CATEGORY).addDetail("Factory", factory)
					.addDetail("Expected Type", factory).addDetail("Actual Type", field.getType()).exception();
			}
			return factory;
		}
		//endregion
	}
}
