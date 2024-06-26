package net.luis.agent.asm.transformer.method;

import net.luis.agent.Agent;
import net.luis.agent.asm.base.*;
import net.luis.agent.asm.data.Class;
import net.luis.agent.asm.data.*;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.asm.report.ReportedException;
import net.luis.agent.asm.type.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;

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
	
	private final Map<Type, String> lookup = Agent.stream().filter(clazz -> clazz.is(ClassType.ANNOTATION) && clazz.isAnnotatedWith(PATTERN))
		.collect(Collectors.toMap(Class::getType, clazz -> Objects.requireNonNull(clazz.getAnnotation(PATTERN).get("value"))));
	
	public PatternTransformer() {
		super(true);
	}
	
	//region Type filtering
	@Override
	protected boolean shouldIgnoreClass(@NotNull Type type) {
		Class clazz = Agent.getClass(type);
		Type[] annotations = this.lookup.keySet().toArray(Type[]::new);
		return clazz.getMethods().values().stream().noneMatch(method -> method.isAnnotatedWith(PATTERN) || method.isAnnotatedWithAny(annotations)) &&
			clazz.getParameters().stream().noneMatch(parameter -> parameter.isAnnotatedWith(PATTERN) || parameter.isAnnotatedWithAny(annotations));
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
		protected boolean isMethodValid(@NotNull Method method) {
			if (!super.isMethodValid(method)) {
				return false;
			}
			if (method.isAnnotatedWith(PATTERN) || method.getParameters().values().stream().anyMatch(parameter -> parameter.isAnnotatedWith(PATTERN))) {
				return true;
			}
			Type[] annotations = this.lookup.keySet().toArray(Type[]::new);
			return method.isAnnotatedWithAny(annotations) || method.getParameters().values().stream().anyMatch(parameter -> parameter.isAnnotatedWithAny(annotations));
		}
		
		@Override
		protected @NotNull MethodVisitor createMethodVisitor(@NotNull LocalVariablesSorter visitor, @NotNull Method method) {
			return new PatternMethodVisitor(visitor, method, this.lookup);
		}
	}
	
	private static class PatternMethodVisitor extends LabelTrackingMethodVisitor {
		
		private static final String REPORT_CATEGORY = "Invalid Annotated Element";
		
		private final Map<Type, String> lookup;
		
		private PatternMethodVisitor(@NotNull LocalVariablesSorter visitor, @NotNull Method method, @NotNull Map<Type, String> lookup) {
			super(visitor);
			this.method = method;
			this.lookup = lookup;
			this.validate(method);
			method.getParameters().values().forEach(this::validate);
		}
		
		@Override
		public void visitCode() {
			this.mv.visitCode();
			for (Parameter parameter : this.method.getParameters().values()) {
				Annotation annotation = this.getAnnotation(parameter);
				if (annotation == null) {
					continue;
				}
				Label label = new Label();
				String value = this.getPattern(annotation);
				
				instrumentPatternCheck(this.mv, value, parameter.getLoadIndex(), label);
				instrumentThrownException(this.mv, ILLEGAL_ARGUMENT_EXCEPTION, parameter.getMessageName() + " must match pattern '" + value + "'");
				
				this.mv.visitJumpInsn(Opcodes.GOTO, label);
				this.insertLabel(label);
			}
		}
		
		@Override
		public void visitInsn(int opcode) {
			Annotation annotation = this.getAnnotation(this.method);
			if (opcode == Opcodes.ARETURN && this.method.is(MethodType.METHOD) && annotation != null) {
				String value = this.getPattern(annotation);
				Label start = new Label();
				Label end = new Label();
				int local = newLocal(this.mv, this.method.getReturnType());
				this.mv.visitVarInsn(Opcodes.ASTORE, local);
				this.insertLabel(start);
				
				instrumentPatternCheck(this.mv, value, local, end);
				instrumentThrownException(this.mv, ILLEGAL_ARGUMENT_EXCEPTION, "Method " + this.method.getOwner().getClassName() + "#" + this.method.getName() + " return value must match pattern '" + value + "'");
				
				this.mv.visitJumpInsn(Opcodes.GOTO, end);
				this.insertLabel(end);
				this.mv.visitVarInsn(Opcodes.ALOAD, local);
				this.visitLocalVariable(local, "generated$PatternTransformer$Temp" + local, STRING, null, start, end);
			}
			this.mv.visitInsn(opcode);
		}
		
		//region Helper methods
		private void validate(@NotNull ASMData data) {
			long count = data.getAnnotations().keySet().stream().filter(this.lookup::containsKey).count();
			if (data.getAnnotations().containsKey(PATTERN)) {
				count++;
			}
			if (0 >= count) {
				return;
			}
			if (count > 1) {
				throw this.createReport(data, type -> "A " + type + " can not be annotated with multiple pattern annotations");
			}
			if (data instanceof Parameter parameter) {
				if (!parameter.getType().equals(STRING)) {
					throw this.createReport(parameter, type -> "Parameter annotated with pattern annotation must be of type string");
				}
			} else if (data instanceof Method method) {
				if (!method.is(MethodType.METHOD)) {
					throw CrashReport.create("Pattern annotation can not be applied to constructors and static initializers", REPORT_CATEGORY).addDetail("Method", this.method.getName()).exception();
				}
				if (!method.returns(STRING)) {
					throw this.createReport(method, type -> "Method annotated with pattern annotation must return a string");
				}
			}
		}
		
		private @Nullable Annotation getAnnotation(@NotNull ASMData data) {
			if (data.getAnnotations().containsKey(PATTERN)) {
				return data.getAnnotation(PATTERN);
			} else {
				return data.getAnnotations().values().stream().filter(annotation -> this.lookup.containsKey(annotation.getType())).findFirst().orElse(null);
			}
		}
		
		private @NotNull String getPattern(@NotNull Annotation annotation) {
			if (annotation.getType().equals(PATTERN)) {
				return Objects.requireNonNull(annotation.get("value"));
			} else {
				return Objects.requireNonNull(this.lookup.get(annotation.getType()));
			}
		}
		
		private @NotNull ReportedException createReport(@NotNull ASMData data, UnaryOperator<String> message) {
			List<Type> annotations = data.getAnnotations().keySet().stream().filter(this.lookup::containsKey).collect(Collectors.toList());
			if (data.getAnnotations().containsKey(PATTERN)) {
				annotations.addFirst(PATTERN);
			}
			if (data instanceof Method) {
				return CrashReport.create(message.apply("method"), REPORT_CATEGORY).addDetail("Method", this.method.getSignature(SignatureType.DEBUG)).addDetail("Pattern Annotations", annotations).exception();
			} else if (data instanceof Parameter parameter) {
				return CrashReport.create(message.apply("parameter"), REPORT_CATEGORY).addDetail("Method", this.method.getSignature(SignatureType.DEBUG)).addDetail("Parameter Index", parameter.getIndex())
					.addDetail("Parameter Type", parameter.getType()).addDetail("Parameter Name", parameter.getName()).addDetail("Pattern Annotations", annotations).exception();
			}
			throw new IllegalArgumentException("Invalid data type, expected Method or Parameter but got " + data.getClass().getSimpleName());
		}
		//endregion
	}
}
