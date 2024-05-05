package net.luis.agent.asm.transformer;

import net.luis.agent.annotation.Default;
import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.visitor.BaseClassVisitor;
import net.luis.agent.asm.base.visitor.BaseMethodVisitor;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.preload.PreloadContext;
import net.luis.agent.preload.data.*;
import net.luis.agent.preload.type.TypeAccess;
import net.luis.agent.preload.type.TypeModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Luis-St
 *
 */

public class DefaultTransformer extends BaseClassTransformer {
	
	private static final Type DEFAULT = Type.getType(Default.class);
	
	private final PreloadContext context;
	
	public DefaultTransformer(@NotNull PreloadContext context) {
		this.context = context;
	}
	
	@Override
	protected boolean shouldIgnore(@NotNull Type type) {
		ClassContent content = this.context.getClassContent(type);
		return super.shouldIgnore(type) || content.methods().stream().filter(method -> !method.is(TypeModifier.ABSTRACT)).map(MethodData::parameters)
			.flatMap(List::stream).noneMatch(parameter -> parameter.isAnnotatedWith(DEFAULT));
	}
	
	@Override
	protected int getClassWriterFlags() {
		return ClassWriter.COMPUTE_FRAMES;
	}
	
	@Override
	@SuppressWarnings("UnqualifiedFieldAccess")
	protected @NotNull ClassVisitor visit(@NotNull Type type, @Nullable Class<?> clazz, @NotNull ClassReader reader, @NotNull ClassWriter writer) {
		ClassContent content = this.context.getClassContent(type);
		Runnable markedModified = () -> this.modified = true;
		return new BaseClassVisitor(writer) {
			@Override
			public @NotNull MethodVisitor visitMethod(int access, @NotNull String name, @NotNull String descriptor, @Nullable String signature, String @Nullable [] exceptions) {
				MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
				MethodData method = content.getMethod(name, Type.getType(descriptor));
				if (method.is(TypeModifier.ABSTRACT) || method.parameters().stream().noneMatch(parameter -> parameter.isAnnotatedWith(DEFAULT))) {
					return visitor;
				}
				return new DefaultVisitor(context, visitor, method, markedModified);
			}
		};
	}
	
	private static class DefaultVisitor extends BaseMethodVisitor {
		
		private static final String REPORT_CATEGORY = "Invalid String Factory";
		private static final Type TYPE = Type.getType(Type.class);
		private static final Type STRING = Type.getType(String.class);
		
		private final List<ParameterData> lookup = new ArrayList<>();
		private final PreloadContext context;
		private final MethodData method;
		private final Runnable markedModified;
		
		private DefaultVisitor(@NotNull PreloadContext context, @NotNull MethodVisitor visitor, @NotNull MethodData method, Runnable markedModified) {
			super(visitor);
			this.context = context;
			this.method = method;
			this.markedModified = markedModified;
			method.parameters().stream().filter(parameter -> parameter.isAnnotatedWith(DEFAULT)).forEach(this.lookup::add);
		}
		
		private @NotNull Type getFactory(@NotNull ParameterData parameter) {
			AnnotationData annotation = parameter.getAnnotation(DEFAULT);
			Type factory = annotation.getOrDefault(this.context, "factory");
			ClassContent content = this.context.getClassContent(factory);
			if (!content.hasField("INSTANCE")) {
				throw CrashReport.create("Missing field INSTANCE in string factory class", REPORT_CATEGORY).addDetail("Factory", factory).exception();
			}
			FieldData field = content.getField("INSTANCE");
			if (field.access() != TypeAccess.PUBLIC) {
				throw CrashReport.create("INSTANCE field in string factory class is not public", REPORT_CATEGORY).addDetail("Factory", factory).exception();
			}
			if (!field.is(TypeModifier.STATIC)) {
				throw CrashReport.create("INSTANCE field in string factory class is not static", REPORT_CATEGORY).addDetail("Factory", factory).exception();
			}
			if (!field.is(TypeModifier.FINAL)) {
				throw CrashReport.create("INSTANCE field in string factory class is not final", REPORT_CATEGORY).addDetail("Factory", factory).exception();
			}
			if (!field.type().equals(factory)) {
				throw CrashReport.create("INSTANCE field in string factory class has invalid type", REPORT_CATEGORY).addDetail("Factory", factory)
					.addDetail("Expected Type", factory).addDetail("Actual Type", field.type()).exception();
			}
			return factory;
		}
		
		@Override
		public void visitCode() {
			this.mv.visitCode();
			boolean isStatic = this.method.is(TypeModifier.STATIC);
			for (ParameterData parameter : this.lookup) {
				Label label = new Label();
				this.mv.visitVarInsn(Opcodes.ALOAD, isStatic ? parameter.index() : parameter.index() + 1);
				this.mv.visitJumpInsn(Opcodes.IFNONNULL, label);
				
				String value = parameter.getAnnotation(DEFAULT).getOrDefault(this.context, "value");
				if (parameter.type().equals(STRING)) {
					this.mv.visitLdcInsn(value);
				} else {
					Type factory = this.getFactory(parameter);
					this.mv.visitFieldInsn(Opcodes.GETSTATIC, factory.getInternalName(), "INSTANCE", factory.getDescriptor());
					this.mv.visitLdcInsn(parameter.type().getDescriptor());
					this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, TYPE.getInternalName(), "getType", "(Ljava/lang/String;)Lorg/objectweb/asm/Type;", false);
					this.mv.visitLdcInsn(value);
					this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, factory.getInternalName(), "create", "(Lorg/objectweb/asm/Type;Ljava/lang/String;)Ljava/lang/Object;", false);
					this.mv.visitTypeInsn(Opcodes.CHECKCAST, parameter.type().getInternalName());
				}
				
				this.mv.visitVarInsn(Opcodes.ASTORE, isStatic ? parameter.index() : parameter.index() + 1);
				this.mv.visitJumpInsn(Opcodes.GOTO, label);
				this.mv.visitLabel(label);
				this.markedModified.run();
			}
		}
	}
}
