package net.luis.agent.asm.transformer.method;

import net.luis.agent.Agent;
import net.luis.agent.asm.ASMTreeUtils;
import net.luis.agent.asm.base.*;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.asm.type.MethodType;
import net.luis.agent.util.Utils;
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

public class NotNullTransformer extends BaseClassTransformer {

	public NotNullTransformer() {
		super(true);
	}

	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @NotNull ClassWriter writer) {
		return new MethodOnlyClassVisitor(writer, type, () -> this.modified = true) {

			@Override
			protected boolean isMethodValid(@NotNull MethodNode methodNode) {
				if (!super.isMethodValid(methodNode)) {
					return false;
				}
				// Check if method has @NotNull annotation
				if (ASMTreeUtils.hasAnnotation(methodNode, NOT_NULL)) {
					return true;
				}
				// Check if any parameter has @NotNull annotation
				return ASMTreeUtils.hasAnyParameterWithAnnotation(methodNode, NOT_NULL);
			}

			@Override
			protected @NotNull MethodVisitor createMethodVisitor(@NotNull LocalVariablesSorter visitor, @NotNull MethodNode methodNode) {
				return new NotNullVisitor(visitor, type, methodNode);
			}
		};
	}

	private static class NotNullVisitor extends LabelTrackingMethodVisitor {

		private static final String REPORT_CATEGORY = "Invalid Annotated Element";

		private final Type ownerType;
		private final MethodNode methodNode;
		private final List<ParameterInfo> parameters = new ArrayList<>();

		private NotNullVisitor(@NotNull MethodVisitor visitor, @NotNull Type ownerType, @NotNull MethodNode methodNode) {
			super(visitor);
			this.ownerType = ownerType;
			this.methodNode = methodNode;

			// Collect parameters with @NotNull annotation
			Type[] paramTypes = ASMTreeUtils.getParameterTypes(methodNode);
			for (int i = 0; i < paramTypes.length; i++) {
				AnnotationNode annotation = ASMTreeUtils.getParameterAnnotation(methodNode, i, NOT_NULL);
				if (annotation != null) {
					int loadIndex = ASMTreeUtils.getParameterLoadIndex(methodNode, i);
					String paramName = getParameterName(methodNode, i);
					this.parameters.add(new ParameterInfo(i, loadIndex, paramTypes[i], paramName, annotation));
				}
			}
		}

		private static @NotNull String getParameterName(@NotNull MethodNode methodNode, int paramIndex) {
			if (methodNode.parameters != null && paramIndex < methodNode.parameters.size()) {
				ParameterNode param = methodNode.parameters.get(paramIndex);
				if (param.name != null) {
					return param.name;
				}
			}
			return "arg" + paramIndex;
		}

		@Override
		public void visitCode() {
			this.mv.visitCode();
			for (ParameterInfo parameter : this.parameters) {
				this.validateParameter(parameter);
				instrumentNonNullCheck(this.mv, parameter.loadIndex, this.getMessage(parameter.annotation, parameter.getMessageName()));
				this.mv.visitInsn(Opcodes.POP);
			}
		}

		@Override
		public void visitFieldInsn(int opcode, @NotNull String owner, @NotNull String name, @NotNull String descriptor) {
			Type type = Type.getType(descriptor);
			if (opcode == Opcodes.PUTFIELD && type.getSort() == Type.OBJECT) {
				Type ownerType = Type.getObjectType(owner);
				ClassNode ownerClass = Agent.getClass(ownerType);
				if (ownerClass != null) {
					FieldNode field = ASMTreeUtils.getField(ownerClass, name);
					if (field != null && ASMTreeUtils.hasAnnotation(field, NOT_NULL)) {
						AnnotationNode annotation = ASMTreeUtils.getAnnotation(field, NOT_NULL);
						instrumentNonNullCheck(this.mv, -1, this.getMessage(annotation, getSimpleName(ownerType) + "#" + name));
						this.mv.visitTypeInsn(Opcodes.CHECKCAST, type.getInternalName());
					}
				}
			}
			super.visitFieldInsn(opcode, owner, name, descriptor);
		}

		@Override
		public void visitVarInsn(int opcode, int index) {
			// Local variable annotation support removed - not commonly used in practice
			super.visitVarInsn(opcode, index);
		}

		@Override
		public void visitInsn(int opcode) {
			if (opcode == Opcodes.ARETURN && ASMTreeUtils.hasAnnotation(this.methodNode, NOT_NULL)) {
				this.validateMethod();
				instrumentNonNullCheck(this.mv, -1, "Method " + this.ownerType.getClassName() + "#" + this.methodNode.name + " must not return null");
				Type returnType = ASMTreeUtils.getReturnType(this.methodNode);
				this.mv.visitTypeInsn(Opcodes.CHECKCAST, returnType.getInternalName());
			}
			this.mv.visitInsn(opcode);
		}

		//region Helper methods
		private @NotNull String getMessage(@NotNull AnnotationNode annotation, @NotNull String messageName) {
			Object value = ASMTreeUtils.getAnnotationValue(annotation, "value");
			if (value instanceof String strValue && !strValue.isBlank()) {
				strValue = strValue.strip();
				if (Utils.isSingleWord(strValue)) {
					return Utils.capitalize(strValue) + " must not be null";
				} else if (strValue.charAt(0) == '\'' && strValue.charAt(strValue.length() - 1) == '\'') {
					return strValue.substring(1, strValue.length() - 1) + " must not be null";
				}
				return strValue;
			}
			return messageName + " must not be null";
		}

		private void validateParameter(@NotNull ParameterInfo parameter) {
			if (isPrimitive(parameter.type)) {
				throw CrashReport.create("Parameter annotated with @NotNull must not be a primitive type", REPORT_CATEGORY)
					.addDetail("Method", ASMTreeUtils.getDebugSignature(this.ownerType, this.methodNode))
					.addDetail("Parameter Index", parameter.index)
					.addDetail("Parameter Type", parameter.type)
					.addDetail("Parameter Name", parameter.name).exception();
			}
		}

		private void validateMethod() {
			if (!ASMTreeUtils.is(this.methodNode, MethodType.METHOD)) {
				throw CrashReport.create("Annotation @NotNull can not be applied to constructors and static initializers", REPORT_CATEGORY)
					.addDetail("Method", ASMTreeUtils.getDebugSignature(this.ownerType, this.methodNode)).exception();
			}
			Type returnType = ASMTreeUtils.getReturnType(this.methodNode);
			if (returnType.equals(VOID)) {
				throw CrashReport.create("Method annotated with @NotNull must not return void", REPORT_CATEGORY)
					.addDetail("Method", ASMTreeUtils.getDebugSignature(this.ownerType, this.methodNode)).exception();
			}
			if (isPrimitive(returnType)) {
				throw CrashReport.create("Method annotated with @NotNull must not return a primitive type", REPORT_CATEGORY)
					.addDetail("Method", ASMTreeUtils.getDebugSignature(this.ownerType, this.methodNode))
					.addDetail("Return Type", returnType).exception();
			}
		}

		private static boolean isPrimitive(@NotNull Type type) {
			return type.equals(BOOLEAN) || type.equals(BYTE) || type.equals(SHORT) || type.equals(CHAR) ||
				   type.equals(INT) || type.equals(LONG) || type.equals(FLOAT) || type.equals(DOUBLE);
		}
		//endregion

		private static class ParameterInfo {
			private final int index;
			private final int loadIndex;
			private final Type type;
			private final String name;
			private final AnnotationNode annotation;

			private ParameterInfo(int index, int loadIndex, @NotNull Type type, @NotNull String name, @NotNull AnnotationNode annotation) {
				this.index = index;
				this.loadIndex = loadIndex;
				this.type = type;
				this.name = name;
				this.annotation = annotation;
			}

			private @NotNull String getMessageName() {
				return this.name.equals("arg" + this.index) ? "Parameter " + this.index : this.name;
			}
		}
	}
}
