package net.luis.agent.asm.transformer.method;

import net.luis.agent.Agent;
import net.luis.agent.asm.ASMTreeUtils;
import net.luis.agent.asm.base.*;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.asm.type.MethodType;
import net.luis.agent.asm.type.TypeModifier;
import net.luis.agent.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;

import java.util.ArrayList;
import java.util.List;

import static net.luis.agent.asm.Instrumentations.*;
import static net.luis.agent.asm.Types.*;

/**
 *
 * @author Luis-St
 *
 */

public class RangeTransformer extends BaseClassTransformer {

	private static final Type[] ANNOS = { ABOVE, ABOVE_EQUAL, BELOW, BELOW_EQUAL };

	public RangeTransformer() {
		super(true);
	}

	//region Type filtering
	@Override
	protected boolean shouldIgnoreClass(@NotNull Type type) {
		ClassNode classNode = Agent.getClass(type);
		if (classNode == null) {
			return true;
		}
		// Check if any method has range annotations
		for (MethodNode method : classNode.methods) {
			if (ASMTreeUtils.hasAnnotation(method, ABOVE) || ASMTreeUtils.hasAnnotation(method, ABOVE_EQUAL) ||
				ASMTreeUtils.hasAnnotation(method, BELOW) || ASMTreeUtils.hasAnnotation(method, BELOW_EQUAL)) {
				return false;
			}
			// Check parameters
			for (int i = 0; i < ASMTreeUtils.getParameterCount(method); i++) {
				if (ASMTreeUtils.hasParameterAnnotation(method, i, ABOVE) || ASMTreeUtils.hasParameterAnnotation(method, i, ABOVE_EQUAL) ||
					ASMTreeUtils.hasParameterAnnotation(method, i, BELOW) || ASMTreeUtils.hasParameterAnnotation(method, i, BELOW_EQUAL)) {
					return false;
				}
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
				// Check method annotations
				if (ASMTreeUtils.hasAnnotation(methodNode, ABOVE) || ASMTreeUtils.hasAnnotation(methodNode, ABOVE_EQUAL) ||
					ASMTreeUtils.hasAnnotation(methodNode, BELOW) || ASMTreeUtils.hasAnnotation(methodNode, BELOW_EQUAL)) {
					return true;
				}
				// Check parameter annotations
				for (int i = 0; i < ASMTreeUtils.getParameterCount(methodNode); i++) {
					if (ASMTreeUtils.hasParameterAnnotation(methodNode, i, ABOVE) || ASMTreeUtils.hasParameterAnnotation(methodNode, i, ABOVE_EQUAL) ||
						ASMTreeUtils.hasParameterAnnotation(methodNode, i, BELOW) || ASMTreeUtils.hasParameterAnnotation(methodNode, i, BELOW_EQUAL)) {
						return true;
					}
				}
				return false;
			}

			@Override
			protected @NotNull MethodVisitor createMethodVisitor(@NotNull LocalVariablesSorter visitor, @NotNull MethodNode methodNode) {
				return new RangeVisitor(visitor, type, methodNode);
			}
		};
	}

	private static class RangeVisitor extends LabelTrackingMethodVisitor {

		private static final String INVALID_CATEGORY = "Invalid Annotated Element";
		private static final String UNSUPPORTED_CATEGORY = "Unsupported Annotation Combination";

		private final Type ownerType;
		private final MethodNode methodNode;
		private final List<ParameterInfo> lookup = new ArrayList<>();

		private RangeVisitor(@NotNull MethodVisitor visitor, @NotNull Type ownerType, @NotNull MethodNode methodNode) {
			super(visitor);
			this.ownerType = ownerType;
			this.methodNode = methodNode;
			//region Parameter validation
			String signature = ASMTreeUtils.getDebugSignature(ownerType, methodNode);
			Type[] paramTypes = ASMTreeUtils.getParameterTypes(methodNode);
			for (int i = 0; i < paramTypes.length; i++) {
				boolean hasRangeAnnotation = ASMTreeUtils.hasParameterAnnotation(methodNode, i, ABOVE) || ASMTreeUtils.hasParameterAnnotation(methodNode, i, ABOVE_EQUAL) ||
					ASMTreeUtils.hasParameterAnnotation(methodNode, i, BELOW) || ASMTreeUtils.hasParameterAnnotation(methodNode, i, BELOW_EQUAL);
				if (hasRangeAnnotation) {
					Type paramType = paramTypes[i];
					String paramName = getParameterName(methodNode, i);
					if (this.isNoNumber(paramType)) {
						throw CrashReport.create(UNSUPPORTED_CATEGORY, "Parameter annotated with @Above, @AboveEqual, @Below or @BelowEqual must be a number type").addDetail("Method", signature)
							.addDetail("Parameter Index", i).addDetail("Parameter Type", paramType).addDetail("Parameter Name", paramName).exception();
					}
					// Count upper bound annotations (@Above, @AboveEqual)
					int upperCount = 0;
					if (ASMTreeUtils.hasParameterAnnotation(methodNode, i, ABOVE)) upperCount++;
					if (ASMTreeUtils.hasParameterAnnotation(methodNode, i, ABOVE_EQUAL)) upperCount++;
					if (upperCount > 1) {
						throw CrashReport.create(UNSUPPORTED_CATEGORY, "Parameter must not be annotated with @Above and @AboveEqual at the same time").addDetail("Method", signature)
							.addDetail("Parameter Index", i).addDetail("Parameter Type", paramType).addDetail("Parameter Name", paramName).exception();
					}
					// Count lower bound annotations (@Below, @BelowEqual)
					int lowerCount = 0;
					if (ASMTreeUtils.hasParameterAnnotation(methodNode, i, BELOW)) lowerCount++;
					if (ASMTreeUtils.hasParameterAnnotation(methodNode, i, BELOW_EQUAL)) lowerCount++;
					if (lowerCount > 1) {
						throw CrashReport.create(UNSUPPORTED_CATEGORY, "Parameter must not be annotated with @Below and @BelowEqual at the same time").addDetail("Method", signature)
							.addDetail("Parameter Index", i).addDetail("Parameter Type", paramType).addDetail("Parameter Name", paramName).exception();
					}
					int loadIndex = ASMTreeUtils.getParameterLoadIndex(methodNode, i);
					this.lookup.add(new ParameterInfo(i, loadIndex, paramType, paramName));
				}
			}
			//endregion
			//region Method validation
			if (ASMTreeUtils.hasAnnotation(methodNode, ABOVE) || ASMTreeUtils.hasAnnotation(methodNode, ABOVE_EQUAL) ||
				ASMTreeUtils.hasAnnotation(methodNode, BELOW) || ASMTreeUtils.hasAnnotation(methodNode, BELOW_EQUAL)) {
				if (!ASMTreeUtils.is(methodNode, MethodType.METHOD)) {
					throw CrashReport.create(INVALID_CATEGORY, "Annotation @Above, @AboveEqual, @Below or @BelowEqual can not be applied to constructors and static initializers").addDetail("Method", methodNode.name).exception();
				}
				Type returnType = ASMTreeUtils.getReturnType(methodNode);
				if (this.isNoNumber(returnType)) {
					throw CrashReport.create(INVALID_CATEGORY, "Method annotated with @Above, @AboveEqual, @Below or @BelowEqual must return a number type").addDetail("Method", signature)
						.addDetail("Return Type", returnType).exception();
				}
				// Count upper bound annotations
				int upperCount = 0;
				if (ASMTreeUtils.hasAnnotation(methodNode, ABOVE)) upperCount++;
				if (ASMTreeUtils.hasAnnotation(methodNode, ABOVE_EQUAL)) upperCount++;
				if (upperCount > 1) {
					throw CrashReport.create(UNSUPPORTED_CATEGORY, "Method must not be annotated with @Above and @AboveEqual at the same time").addDetail("Method", signature).exception();
				}
				// Count lower bound annotations
				int lowerCount = 0;
				if (ASMTreeUtils.hasAnnotation(methodNode, BELOW)) lowerCount++;
				if (ASMTreeUtils.hasAnnotation(methodNode, BELOW_EQUAL)) lowerCount++;
				if (lowerCount > 1) {
					throw CrashReport.create(UNSUPPORTED_CATEGORY, "Method must not be annotated with @Below and @BelowEqual at the same time").addDetail("Method", signature).exception();
				}
			}
			//endregion
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
			for (ParameterInfo parameter : this.lookup) {
				String message = parameter.getMessageName() + " must be ";
				int index = parameter.loadIndex;

				AnnotationNode aboveAnnotation = ASMTreeUtils.getParameterAnnotation(this.methodNode, parameter.index, ABOVE);
				if (aboveAnnotation != null) {
					this.instrument(aboveAnnotation, parameter.type, index, true, Opcodes.IFGT, message + "above");
				}
				AnnotationNode aboveEqualAnnotation = ASMTreeUtils.getParameterAnnotation(this.methodNode, parameter.index, ABOVE_EQUAL);
				if (aboveEqualAnnotation != null) {
					this.instrument(aboveEqualAnnotation, parameter.type, index, true, Opcodes.IFGE, message + "above or equal to");
				}
				AnnotationNode belowAnnotation = ASMTreeUtils.getParameterAnnotation(this.methodNode, parameter.index, BELOW);
				if (belowAnnotation != null) {
					this.instrument(belowAnnotation, parameter.type, index, false, Opcodes.IFGT, message + "below");
				}
				AnnotationNode belowEqualAnnotation = ASMTreeUtils.getParameterAnnotation(this.methodNode, parameter.index, BELOW_EQUAL);
				if (belowEqualAnnotation != null) {
					this.instrument(belowEqualAnnotation, parameter.type, index, false, Opcodes.IFGE, message + "below or equal to");
				}
			}
		}

		@Override
		public void visitInsn(int opcode) {
			if (isReturn(opcode) && (ASMTreeUtils.hasAnnotation(this.methodNode, ABOVE) || ASMTreeUtils.hasAnnotation(this.methodNode, ABOVE_EQUAL) ||
				ASMTreeUtils.hasAnnotation(this.methodNode, BELOW) || ASMTreeUtils.hasAnnotation(this.methodNode, BELOW_EQUAL))) {
				Label start = new Label();
				Label end = new Label();
				Type type = ASMTreeUtils.getReturnType(this.methodNode);

				int local = newLocal(this.mv, type);
				this.mv.visitVarInsn(type.getOpcode(Opcodes.ISTORE), local);
				this.insertLabel(start);

				String message = "Method " + this.ownerType.getClassName() + "#" + this.methodNode.name + " return value must be ";
				AnnotationNode aboveAnnotation = ASMTreeUtils.getAnnotation(this.methodNode, ABOVE);
				if (aboveAnnotation != null) {
					this.instrument(aboveAnnotation, type, local, true, Opcodes.IFGT, message + "above");
				}
				AnnotationNode aboveEqualAnnotation = ASMTreeUtils.getAnnotation(this.methodNode, ABOVE_EQUAL);
				if (aboveEqualAnnotation != null) {
					this.instrument(aboveEqualAnnotation, type, local, true, Opcodes.IFGE, message + "above or equal to");
				}
				AnnotationNode belowAnnotation = ASMTreeUtils.getAnnotation(this.methodNode, BELOW);
				if (belowAnnotation != null) {
					this.instrument(belowAnnotation, type, local, false, Opcodes.IFGT, message + "below");
				}
				AnnotationNode belowEqualAnnotation = ASMTreeUtils.getAnnotation(this.methodNode, BELOW_EQUAL);
				if (belowEqualAnnotation != null) {
					this.instrument(belowEqualAnnotation, type, local, false, Opcodes.IFGE, message + "below or equal to");
				}

				this.mv.visitJumpInsn(Opcodes.GOTO, end);
				this.insertLabel(end);
				this.mv.visitVarInsn(type.getOpcode(Opcodes.ILOAD), local);
				this.visitLocalVariable(local, "generated$RangeTransformer$Temp" + local, type, null, start, end);
			}
			this.mv.visitInsn(opcode);
		}

		//region Helper methods
		private boolean isNoNumber(@NotNull Type type) {
			return Utils.indexOf(NUMBERS, convertToPrimitive(type)) == -1;
		}

		private void instrument(@NotNull AnnotationNode annotation, @NotNull Type type, int loadIndex, boolean above, int compare, String message) {
			Label label = new Label();
			Object valueObj = ASMTreeUtils.getAnnotationValue(annotation, "value");
			if (valueObj == null) {
				return;
			}
			double value = ((Number) valueObj).doubleValue();
			if (above) {
				loadNumberAsDouble(this.mv, type, loadIndex);
				loadNumber(this.mv, value);
			} else {
				loadNumber(this.mv, value);
				loadNumberAsDouble(this.mv, type, loadIndex);
			}
			this.mv.visitInsn(Opcodes.DCMPL);
			this.mv.visitJumpInsn(compare, label);
			instrumentThrownException(this.mv, ILLEGAL_ARGUMENT_EXCEPTION, message + " " + value);
			this.mv.visitJumpInsn(Opcodes.GOTO, label);
			this.insertLabel(label);
		}
		//endregion

		private static class ParameterInfo {
			private final int index;
			private final int loadIndex;
			private final Type type;
			private final String name;

			private ParameterInfo(int index, int loadIndex, @NotNull Type type, @NotNull String name) {
				this.index = index;
				this.loadIndex = loadIndex;
				this.type = type;
				this.name = name;
			}

			private @NotNull String getMessageName() {
				return this.name.equals("arg" + this.index) ? "Parameter " + this.index : this.name;
			}
		}
	}
}
