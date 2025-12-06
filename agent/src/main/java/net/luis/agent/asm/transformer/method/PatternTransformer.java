package net.luis.agent.asm.transformer.method;

import net.luis.agent.Agent;
import net.luis.agent.asm.ASMTreeUtils;
import net.luis.agent.asm.base.*;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.asm.report.ReportedException;
import net.luis.agent.asm.type.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static net.luis.agent.asm.Instrumentations.*;
import static net.luis.agent.asm.Types.*;

/**
 *
 * @author Luis-St
 *
 */

public class PatternTransformer extends BaseClassTransformer {

	private final Map<Type, String> lookup;

	public PatternTransformer() {
		super(true);
		// Build lookup map by scanning all annotation classes with @Pattern
		this.lookup = Agent.stream()
			.map(ASMTreeUtils::getType)
			.map(Agent::getClass)
			.filter(Objects::nonNull)
			.filter(classNode -> ASMTreeUtils.is(classNode, ClassType.ANNOTATION) && ASMTreeUtils.hasAnnotation(classNode, PATTERN))
			.collect(Collectors.toMap(ASMTreeUtils::getType, classNode -> {
				AnnotationNode annotation = ASMTreeUtils.getAnnotation(classNode, PATTERN);
				Object value = ASMTreeUtils.getAnnotationValue(annotation, "value");
				return Objects.requireNonNull((String) value);
			}));
	}

	//region Type filtering
	@Override
	protected boolean shouldIgnoreClass(@NotNull Type type) {
		ClassNode classNode = Agent.getClass(type);
		if (classNode == null) {
			return true;
		}
		Type[] annotations = this.lookup.keySet().toArray(Type[]::new);

		// Check if any method has @Pattern or any pattern annotation
		for (MethodNode method : classNode.methods) {
			if (ASMTreeUtils.hasAnnotation(method, PATTERN)) {
				return false;
			}
			for (Type annotationType : annotations) {
				if (ASMTreeUtils.hasAnnotation(method, annotationType)) {
					return false;
				}
			}
			// Check parameters
			if (ASMTreeUtils.hasAnyParameterWithAnnotation(method, PATTERN)) {
				return false;
			}
			for (Type annotationType : annotations) {
				if (ASMTreeUtils.hasAnyParameterWithAnnotation(method, annotationType)) {
					return false;
				}
			}
		}
		return true;
	}
	//endregion

	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @NotNull ClassWriter writer) {
		return new PatternClassVisitor(writer, type, this.lookup, () -> this.modified = true);
	}

	private static class PatternClassVisitor extends MethodOnlyClassVisitor {

		private final Map<Type, String> lookup;

		private PatternClassVisitor(@NotNull ClassVisitor visitor, @NotNull Type type, @NotNull Map<Type, String> lookup, @NotNull Runnable markModified) {
			super(visitor, type, markModified);
			this.lookup = lookup;
		}

		@Override
		protected boolean isMethodValid(@NotNull MethodNode methodNode) {
			if (!super.isMethodValid(methodNode)) {
				return false;
			}

			// Check method annotation
			if (ASMTreeUtils.hasAnnotation(methodNode, PATTERN)) {
				return true;
			}
			Type[] annotations = this.lookup.keySet().toArray(Type[]::new);
			for (Type annotationType : annotations) {
				if (ASMTreeUtils.hasAnnotation(methodNode, annotationType)) {
					return true;
				}
			}

			// Check parameter annotations
			if (ASMTreeUtils.hasAnyParameterWithAnnotation(methodNode, PATTERN)) {
				return true;
			}
			for (Type annotationType : annotations) {
				if (ASMTreeUtils.hasAnyParameterWithAnnotation(methodNode, annotationType)) {
					return true;
				}
			}

			return false;
		}

		@Override
		protected @NotNull MethodVisitor createMethodVisitor(@NotNull LocalVariablesSorter visitor, @NotNull MethodNode methodNode) {
			return new PatternMethodVisitor(visitor, this.type, methodNode, this.lookup);
		}
	}

	private static class PatternMethodVisitor extends LabelTrackingMethodVisitor {

		private static final String REPORT_CATEGORY = "Invalid Annotated Element";

		private final Type ownerType;
		private final MethodNode methodNode;
		private final Map<Type, String> lookup;
		private final List<ParameterInfo> parameters = new ArrayList<>();

		private PatternMethodVisitor(@NotNull LocalVariablesSorter visitor, @NotNull Type ownerType, @NotNull MethodNode methodNode, @NotNull Map<Type, String> lookup) {
			super(visitor);
			this.ownerType = ownerType;
			this.methodNode = methodNode;
			this.lookup = lookup;

			// Validate method annotation
			this.validateMethod();

			// Collect and validate parameters
			Type[] paramTypes = ASMTreeUtils.getParameterTypes(methodNode);
			for (int i = 0; i < paramTypes.length; i++) {
				AnnotationNode annotation = this.getParameterAnnotation(i);
				if (annotation != null) {
					this.validateParameter(i, paramTypes[i], annotation);
					int loadIndex = ASMTreeUtils.getParameterLoadIndex(methodNode, i);
					String paramName = getParameterName(i);
					this.parameters.add(new ParameterInfo(i, loadIndex, paramTypes[i], paramName, annotation));
				}
			}
		}

		private static @NotNull String getParameterName(int paramIndex) {
			return "arg" + paramIndex;
		}

		@Override
		public void visitCode() {
			this.mv.visitCode();
			for (ParameterInfo parameter : this.parameters) {
				Label label = new Label();
				String value = this.getPattern(parameter.annotation);

				instrumentPatternCheck(this.mv, value, parameter.loadIndex, label);
				instrumentThrownException(this.mv, ILLEGAL_ARGUMENT_EXCEPTION, parameter.getMessageName() + " must match pattern '" + value + "'");

				this.mv.visitJumpInsn(Opcodes.GOTO, label);
				this.insertLabel(label);
			}
		}

		@Override
		public void visitInsn(int opcode) {
			AnnotationNode annotation = this.getMethodAnnotation();
			if (opcode == Opcodes.ARETURN && ASMTreeUtils.is(this.methodNode, MethodType.METHOD) && annotation != null) {
				String value = this.getPattern(annotation);
				Label start = new Label();
				Label end = new Label();
				Type returnType = ASMTreeUtils.getReturnType(this.methodNode);
				int local = newLocal(this.mv, returnType);
				this.mv.visitVarInsn(Opcodes.ASTORE, local);
				this.insertLabel(start);

				instrumentPatternCheck(this.mv, value, local, end);
				instrumentThrownException(this.mv, ILLEGAL_ARGUMENT_EXCEPTION, "Method " + this.ownerType.getClassName() + "#" + this.methodNode.name + " return value must match pattern '" + value + "'");

				this.mv.visitJumpInsn(Opcodes.GOTO, end);
				this.insertLabel(end);
				this.mv.visitVarInsn(Opcodes.ALOAD, local);
				this.visitLocalVariable(local, "generated$PatternTransformer$Temp" + local, STRING, null, start, end);
			}
			this.mv.visitInsn(opcode);
		}

		//region Helper methods
		private void validateMethod() {
			long count = this.countMethodAnnotations();
			if (count == 0) {
				return;
			}
			if (count > 1) {
				throw this.createMethodReport("A method can not be annotated with multiple pattern annotations");
			}

			if (!ASMTreeUtils.is(this.methodNode, MethodType.METHOD)) {
				throw CrashReport.create("Pattern annotation can not be applied to constructors and static initializers", REPORT_CATEGORY)
					.addDetail("Method", this.methodNode.name).exception();
			}
			if (!ASMTreeUtils.returns(this.methodNode, STRING)) {
				throw this.createMethodReport("Method annotated with pattern annotation must return a string");
			}
		}

		private void validateParameter(int index, @NotNull Type paramType, @NotNull AnnotationNode annotation) {
			long count = this.countParameterAnnotations(index);
			if (count > 1) {
				throw this.createParameterReport(index, paramType, "A parameter can not be annotated with multiple pattern annotations");
			}
			if (!paramType.equals(STRING)) {
				throw this.createParameterReport(index, paramType, "Parameter annotated with pattern annotation must be of type string");
			}
		}

		private long countMethodAnnotations() {
			long count = ASMTreeUtils.hasAnnotation(this.methodNode, PATTERN) ? 1 : 0;
			for (Type annotationType : this.lookup.keySet()) {
				if (ASMTreeUtils.hasAnnotation(this.methodNode, annotationType)) {
					count++;
				}
			}
			return count;
		}

		private long countParameterAnnotations(int paramIndex) {
			long count = ASMTreeUtils.hasParameterAnnotation(this.methodNode, paramIndex, PATTERN) ? 1 : 0;
			for (Type annotationType : this.lookup.keySet()) {
				if (ASMTreeUtils.hasParameterAnnotation(this.methodNode, paramIndex, annotationType)) {
					count++;
				}
			}
			return count;
		}

		private @Nullable AnnotationNode getMethodAnnotation() {
			AnnotationNode annotation = ASMTreeUtils.getAnnotation(this.methodNode, PATTERN);
			if (annotation != null) {
				return annotation;
			}
			for (Type annotationType : this.lookup.keySet()) {
				annotation = ASMTreeUtils.getAnnotation(this.methodNode, annotationType);
				if (annotation != null) {
					return annotation;
				}
			}
			return null;
		}

		private @Nullable AnnotationNode getParameterAnnotation(int paramIndex) {
			AnnotationNode annotation = ASMTreeUtils.getParameterAnnotation(this.methodNode, paramIndex, PATTERN);
			if (annotation != null) {
				return annotation;
			}
			for (Type annotationType : this.lookup.keySet()) {
				annotation = ASMTreeUtils.getParameterAnnotation(this.methodNode, paramIndex, annotationType);
				if (annotation != null) {
					return annotation;
				}
			}
			return null;
		}

		private @NotNull String getPattern(@NotNull AnnotationNode annotation) {
			Type annotationType = Type.getType(annotation.desc);
			if (annotationType.equals(PATTERN)) {
				Object value = ASMTreeUtils.getAnnotationValue(annotation, "value");
				return Objects.requireNonNull((String) value);
			} else {
				return Objects.requireNonNull(this.lookup.get(annotationType));
			}
		}

		private @NotNull ReportedException createMethodReport(@NotNull String message) {
			List<Type> annotations = new ArrayList<>();
			if (ASMTreeUtils.hasAnnotation(this.methodNode, PATTERN)) {
				annotations.add(PATTERN);
			}
			for (Type annotationType : this.lookup.keySet()) {
				if (ASMTreeUtils.hasAnnotation(this.methodNode, annotationType)) {
					annotations.add(annotationType);
				}
			}
			return CrashReport.create(message, REPORT_CATEGORY)
				.addDetail("Method", ASMTreeUtils.getDebugSignature(this.ownerType, this.methodNode))
				.addDetail("Pattern Annotations", annotations).exception();
		}

		private @NotNull ReportedException createParameterReport(int paramIndex, @NotNull Type paramType, @NotNull String message) {
			List<Type> annotations = new ArrayList<>();
			if (ASMTreeUtils.hasParameterAnnotation(this.methodNode, paramIndex, PATTERN)) {
				annotations.add(PATTERN);
			}
			for (Type annotationType : this.lookup.keySet()) {
				if (ASMTreeUtils.hasParameterAnnotation(this.methodNode, paramIndex, annotationType)) {
					annotations.add(annotationType);
				}
			}
			return CrashReport.create(message, REPORT_CATEGORY)
				.addDetail("Method", ASMTreeUtils.getDebugSignature(this.ownerType, this.methodNode))
				.addDetail("Parameter Index", paramIndex)
				.addDetail("Parameter Type", paramType)
				.addDetail("Parameter Name", getParameterName(paramIndex))
				.addDetail("Pattern Annotations", annotations).exception();
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
