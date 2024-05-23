package net.luis.agent.asm.transformer.method;

import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.visitor.*;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.preload.PreloadContext;
import net.luis.agent.preload.data.*;
import net.luis.agent.preload.type.TypeAccess;
import net.luis.agent.preload.type.TypeModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.util.ArrayList;
import java.util.List;

import static net.luis.agent.asm.Types.*;

/**
 *
 * @author Luis-St
 *
 */

public class DefaultTransformer extends BaseClassTransformer {
	
	public DefaultTransformer(@NotNull PreloadContext context) {
		super(context, true);
	}
	
	//region Type filtering
	@Override
	protected boolean shouldTransform(@NotNull Type type) {
		ClassContent content = this.context.getClassContent(type);
		return content.getParameters().stream().anyMatch(parameter -> parameter.isAnnotatedWith(DEFAULT));
	}
	//endregion
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @Nullable Class<?> clazz, @NotNull ClassWriter writer) {
		return new MethodOnlyClassVisitor(writer, this.context, type, () -> this.modified = true) {
			
			@Override
			protected @NotNull MethodVisitor createMethodVisitor(@NotNull LocalVariablesSorter visitor, @NotNull MethodData method) {
				return new DefaultVisitor(visitor, this.context, this.type, method, this::markModified);
			}
		};
	}
	
	private static class DefaultVisitor extends ContextBasedMethodVisitor {
		
		private static final String REPORT_CATEGORY = "Invalid String Factory";
		
		private final List<ParameterData> lookup = new ArrayList<>();
		
		private DefaultVisitor(@NotNull MethodVisitor visitor, @NotNull PreloadContext context, @NotNull Type type, @NotNull MethodData method, @NotNull Runnable markModified) {
			super(visitor, context, type, method, markModified);
			method.parameters().stream().filter(parameter -> parameter.isAnnotatedWith(DEFAULT)).forEach(this.lookup::add);
		}
		
		@Override
		public void visitCode() {
			this.mv.visitCode();
			for (ParameterData parameter : this.lookup) {
				Label label = new Label();
				this.visitVarInsn(Opcodes.ALOAD, parameter);
				this.mv.visitJumpInsn(Opcodes.IFNONNULL, label);
				
				String value = parameter.getAnnotation(DEFAULT).getOrDefault(this.context, "value");
				if (parameter.is(STRING)) {
					this.mv.visitLdcInsn(value);
				} else {
					this.instrumentFactoryCall(this.mv, this.getFactory(parameter), parameter.type(), value);
					
					//this.mv.visitFieldInsn(Opcodes.GETSTATIC, factory.getInternalName(), "INSTANCE", factory.getDescriptor());
					//this.mv.visitLdcInsn(parameter.type().getDescriptor());
					//this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, TYPE.getInternalName(), "getType", "(Ljava/lang/String;)Lorg/objectweb/asm/Type;", false);
					//this.mv.visitLdcInsn(value);
					//this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, factory.getInternalName(), "create", "(Lorg/objectweb/asm/Type;Ljava/lang/String;)Ljava/lang/Object;", false);
					//this.mv.visitTypeInsn(Opcodes.CHECKCAST, parameter.type().getInternalName());
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
			Type factory = annotation.getOrDefault(this.context, "factory");
			FieldData field = this.context.getClassContent(factory).getField("INSTANCE");
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
