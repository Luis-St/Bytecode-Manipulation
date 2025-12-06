package net.luis.agent.asm.transformer.method;

import net.luis.agent.Agent;
import net.luis.agent.asm.ASMTreeUtils;
import net.luis.agent.asm.base.*;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.asm.type.*;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.*;

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
		ClassNode classNode = Agent.getClass(type);
		if (classNode == null) {
			return true;
		}
		// Check if any method has parameters with @Default annotation
		for (MethodNode method : classNode.methods) {
			if (ASMTreeUtils.hasAnyParameterWithAnnotation(method, DEFAULT)) {
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
				return ASMTreeUtils.hasAnyParameterWithAnnotation(methodNode, DEFAULT);
			}

			@Override
			protected @NotNull MethodVisitor createMethodVisitor(@NotNull LocalVariablesSorter visitor, @NotNull MethodNode methodNode) {
				return new DefaultVisitor(visitor, type, methodNode);
			}
		};
	}

	private static class DefaultVisitor extends LabelTrackingMethodVisitor {

		private static final String REPORT_CATEGORY = "Invalid String Factory";

		private final Type ownerType;
		private final MethodNode methodNode;
		private final List<ParameterInfo> parameters = new ArrayList<>();

		private DefaultVisitor(@NotNull MethodVisitor visitor, @NotNull Type ownerType, @NotNull MethodNode methodNode) {
			super(visitor);
			this.ownerType = ownerType;
			this.methodNode = methodNode;

			// Collect parameters with @Default annotation
			Type[] paramTypes = ASMTreeUtils.getParameterTypes(methodNode);
			for (int i = 0; i < paramTypes.length; i++) {
				AnnotationNode annotation = ASMTreeUtils.getParameterAnnotation(methodNode, i, DEFAULT);
				if (annotation != null) {
					int loadIndex = ASMTreeUtils.getParameterLoadIndex(methodNode, i);
					this.parameters.add(new ParameterInfo(i, loadIndex, paramTypes[i], annotation));
				}
			}
		}

		@Override
		public void visitCode() {
			this.mv.visitCode();
			for (ParameterInfo parameter : this.parameters) {
				Label label = new Label();
				this.visitVarInsn(Opcodes.ALOAD, parameter.loadIndex);
				this.mv.visitJumpInsn(Opcodes.IFNONNULL, label);

				String value = ASMTreeUtils.getAnnotationValue(parameter.annotation, "value", "");
				if (parameter.type.equals(STRING)) {
					this.mv.visitLdcInsn(value);
				} else {
					ClassNode ownerClass = Agent.getClass(this.ownerType);
					String classSignature = ownerClass != null && ownerClass.signature != null ? ownerClass.signature : null;
					String methodSignature = this.methodNode.signature;
					Type factoryType = this.getFactory(parameter);
					instrumentFactoryCall(this.mv, factoryType, parameter.type, classSignature, methodSignature, parameter.index, value);
				}

				this.visitVarInsn(Opcodes.ASTORE, parameter.loadIndex);
				this.mv.visitJumpInsn(Opcodes.GOTO, label);
				this.insertLabel(label);
			}
		}

		//region Helper methods
		private @NotNull Type getFactory(@NotNull ParameterInfo parameter) {
			Object factoryValue = ASMTreeUtils.getAnnotationValue(parameter.annotation, "factory");
			Type factory;
			if (factoryValue instanceof org.objectweb.asm.Type) {
				factory = (org.objectweb.asm.Type) factoryValue;
			} else {
				// Use default value from annotation definition when not explicitly set
				factory = org.objectweb.asm.Type.getType("Lnet/luis/agent/util/factory/StringFactoryRegistry;");
			}

			ClassNode factoryClass = Agent.getClass(factory);
			if (factoryClass == null) {
				throw CrashReport.create("Cannot find string factory class", REPORT_CATEGORY)
					.addDetail("Factory", factory).exception();
			}

			FieldNode field = ASMTreeUtils.getField(factoryClass, "INSTANCE");
			if (field == null) {
				throw CrashReport.create("Missing INSTANCE field in string factory class", REPORT_CATEGORY)
					.addDetail("Factory", factory).exception();
			}
			if (!ASMTreeUtils.is(field, TypeAccess.PUBLIC) || !ASMTreeUtils.is(field, TypeModifier.STATIC) || !ASMTreeUtils.is(field, TypeModifier.FINAL)) {
				throw CrashReport.create("INSTANCE field in string factory class is not public static final", REPORT_CATEGORY)
					.addDetail("Factory", factory).exception();
			}
			Type fieldType = ASMTreeUtils.getType(field);
			if (!fieldType.equals(factory)) {
				throw CrashReport.create("INSTANCE field in string factory class has invalid type", REPORT_CATEGORY)
					.addDetail("Factory", factory)
					.addDetail("Expected Type", factory)
					.addDetail("Actual Type", fieldType).exception();
			}
			return factory;
		}
		//endregion

		private static class ParameterInfo {
			private final int index;
			private final int loadIndex;
			private final Type type;
			private final AnnotationNode annotation;

			private ParameterInfo(int index, int loadIndex, @NotNull Type type, @NotNull AnnotationNode annotation) {
				this.index = index;
				this.loadIndex = loadIndex;
				this.type = type;
				this.annotation = annotation;
			}
		}
	}
}
