package net.luis.agent.asm.transformer.method;

import net.luis.agent.Agent;
import net.luis.agent.asm.base.*;
import net.luis.agent.asm.data.*;
import net.luis.agent.asm.data.Class;
import net.luis.agent.util.StripMode;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.util.*;
import java.util.stream.Stream;

import static net.luis.agent.asm.Instrumentations.*;
import static net.luis.agent.asm.Types.*;

/**
 *
 * @author Luis-St
 *
 */

public class StringTransformer extends BaseClassTransformer {
	
	private static final Type[] CONDITIONS = {
		CONTAINS, ENDS_WITH, NOT_BLANK, NOT_EMPTY, STARTS_WITH
	};
	private static final Type[] MODIFICATIONS = {
		LOWER_CASE, REPLACE, STRIP, SUBSTRING, TRIM, UPPER_CASE
	};
	private static final Type[] ALL = Stream.concat(Arrays.stream(CONDITIONS), Arrays.stream(MODIFICATIONS)).toArray(Type[]::new);
	
	public StringTransformer() {
		super(true);
	}
	
	//region Type filtering
	@Override
	protected boolean shouldIgnoreClass(@NotNull Type type) {
		Class clazz = Agent.getClass(type);
		return clazz.getParameters().stream().noneMatch(parameter -> parameter.isAnnotatedWithAny(ALL)) && clazz.getMethods().values().stream().noneMatch(method -> method.isAnnotatedWithAny(ALL));
	}
	//endregion
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @NotNull ClassWriter writer) {
		return new MethodOnlyClassVisitor(writer, type, () -> this.modified = true) {
			
			@Override
			protected boolean isMethodValid(@NotNull Method method) {
				if (!super.isMethodValid(method)) {
					return false;
				}
				if (method.isAnnotatedWithAny(ALL) && method.returns(STRING)) {
					return true;
				}
				return method.getParameters().values().stream().anyMatch(parameter -> parameter.isAnnotatedWithAny(ALL) && parameter.is(STRING));
			}
			
			@Override
			protected @NotNull MethodVisitor createMethodVisitor(@NotNull LocalVariablesSorter visitor, @NotNull Method method) {
				return new StringMethodVisitor(visitor, method);
			}
		};
	}
	
	private static class StringMethodVisitor extends LabelTrackingMethodVisitor {
		
		private final Method method;
		private final List<Parameter> parameters;
		
		private StringMethodVisitor(@NotNull MethodVisitor visitor, @NotNull Method method) {
			super(visitor);
			this.setMethod(method);
			this.method = method;
			this.parameters = method.getParameters().values().stream().filter(parameter -> parameter.isAnnotatedWithAny(ALL) && parameter.is(STRING)).toList();
		}
		
		@Override
		public void visitCode() {
			super.visitCode();
			for (Parameter parameter : this.parameters) {
				this.mv.visitVarInsn(Opcodes.ALOAD, parameter.getLoadIndex());
				this.instrumentModifications(parameter.getAnnotations());
				this.mv.visitVarInsn(Opcodes.ASTORE, parameter.getLoadIndex());
				
				this.instrumentConditions(parameter.getLoadIndex(), parameter.getAnnotations());
			}
		}
		
		@Override
		public void visitInsn(int opcode) {
			if (isReturn(opcode) && this.method.returns(STRING)) {
				int local = -1;
				if (this.method.isAnnotatedWithAny(CONDITIONS)) {
					local = newLocal(this.mv, STRING);
				}
				
				if (local == -1) {
					this.instrumentModifications(this.method.getAnnotations());
				} else {
					Label start = new Label();
					Label end = new Label();
					
					this.mv.visitVarInsn(Opcodes.ASTORE, local);
					this.insertLabel(start);
					this.instrumentConditions(local, this.method.getAnnotations());
					
					this.mv.visitVarInsn(Opcodes.ALOAD, local);
					this.instrumentModifications(this.method.getAnnotations());
					this.insertLabel(end);
					this.visitLocalVariable(local, "generated$StringTransformer$Temp" + local, STRING, null, start, end);
				}
			}
			super.visitInsn(opcode);
		}
		
		//region Instrumentation
		private void instrumentModifications(@NotNull Map<Type, Annotation> annotations) {
			if (annotations.containsKey(LOWER_CASE)) {
				this.instrumentLowerCase(annotations.get(LOWER_CASE));
			}
			if (annotations.containsKey(REPLACE)) {
				this.instrumentReplace(annotations.get(REPLACE));
			}
			if (annotations.containsKey(STRIP)) {
				this.instrumentStrip(annotations.get(STRIP));
			}
			if (annotations.containsKey(SUBSTRING)) {
				this.instrumentSubstring(annotations.get(SUBSTRING));
			}
			if (annotations.containsKey(TRIM)) {
				this.instrumentTrim(annotations.get(TRIM));
			}
			if (annotations.containsKey(UPPER_CASE)) {
				this.instrumentUpperCase(annotations.get(UPPER_CASE));
			}
		}
		
		private void instrumentConditions(int index, @NotNull Map<Type, Annotation> annotations) {
			if (annotations.containsKey(CONTAINS)) {
				this.instrumentContains(index, annotations.get(CONTAINS));
			}
			if (annotations.containsKey(ENDS_WITH)) {
				this.instrumentEndsWith(index, annotations.get(ENDS_WITH));
			}
			if (annotations.containsKey(NOT_BLANK)) {
				this.instrumentNotBlank(index, annotations.get(NOT_BLANK));
			}
			if (annotations.containsKey(NOT_EMPTY)) {
				this.instrumentNotEmpty(index, annotations.get(NOT_EMPTY));
			}
			if (annotations.containsKey(STARTS_WITH)) {
				this.instrumentStartsWith(index, annotations.get(STARTS_WITH));
			}
		}
		//endregion
		
		//region Modifications
		private void instrumentLowerCase(@NotNull Annotation annotation) {
			String value = annotation.getOrDefault("value");
			if (value.isEmpty()) {
				this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "toLowerCase", "()Ljava/lang/String;", false);
			} else {
				this.instrumentLocale(value);
				this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "toLowerCase", "(Ljava/util/Locale;)Ljava/lang/String;", false);
			}
		}
		
		private void instrumentReplace(@NotNull Annotation annotation) {
			String value = annotation.getOrDefault("value");
			String regex = annotation.getOrDefault("regex");
			String replacement = annotation.getOrDefault("replacement");
			boolean all = annotation.getOrDefault("all");
			
			if (value.isEmpty() && regex.isEmpty()) {
				throw new IllegalArgumentException("Invalid @Replace annotation found, expected at least one of 'value' or 'regex' to be set");
			}
			if (!value.isEmpty() && !regex.isEmpty()) {
				throw new IllegalArgumentException("Invalid @Replace annotation found, expected only one of 'value' or 'regex' to be set");
			}
			if (!value.contains(" -> ") && replacement.isEmpty()) {
				throw new IllegalArgumentException("Invalid @Replace annotation found, expected 'value' to be formated as 'target -> replacement' or 'replacement' to be set");
			}
			if (regex.isEmpty()) {
				if (value.contains(" -> ")) {
					String[] parts = value.split(" -> ");
					this.mv.visitLdcInsn(parts[0]);
					this.mv.visitLdcInsn(parts[1]);
				} else {
					this.mv.visitLdcInsn(value);
					this.mv.visitLdcInsn(replacement);
				}
				this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "replace", "(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;", false);
			} else {
				this.mv.visitLdcInsn(regex);
				this.mv.visitLdcInsn(replacement);
				if (all) {
					this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "replaceAll", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", false);
				} else {
					this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "replaceFirst", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", false);
				}
			}
		}
		
		private void instrumentStrip(@NotNull Annotation annotation) {
			switch (StripMode.valueOf(annotation.getOrDefault("value"))) {
				case BOTH -> this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "strip", "()Ljava/lang/String;", false);
				case LEADING -> this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "stripLeading", "()Ljava/lang/String;", false);
				case TRAILING -> this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "stripTrailing", "()Ljava/lang/String;", false);
				case INDENT -> this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "indent", "()Ljava/lang/String;", false);
			}
		}
		
		private void instrumentSubstring(@NotNull Annotation annotation) {
			String value = annotation.getOrDefault("value");
			if (value.isBlank()) {
				throw new IllegalArgumentException("Invalid @Substring annotation found, expected 'start:end' but found '" + value + "'");
			}
			
			int start = 0;
			int end = -1;
			if (value.contains(":")) {
				String[] parts = value.split(":");
				if (parts.length != 2) {
					throw new IllegalArgumentException("Invalid @Substring annotation found, expected 'start:end' but found '" + value + "'");
				}
				if ("*".equals(parts[0]) && "*".equals(parts[1])) {
					throw new IllegalArgumentException("Invalid @Substring annotation found, expected 'start:end' but found '" + value + "'");
				}
				if (!parts[0].isBlank() && !"*".equals(parts[0])) {
					start = Integer.parseInt(parts[0]);
				}
				if (!parts[1].isBlank() && !"*".equals(parts[1])) {
					end = Integer.parseInt(parts[1]);
				}
			} else {
				start = Integer.parseInt(value);
			}
			
			loadNumber(this.mv, start);
			if (0 > end) {
				this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "substring", "(I)Ljava/lang/String;", false);
			} else {
				loadNumber(this.mv, end);
				this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "substring", "(II)Ljava/lang/String;", false);
			}
		}
		
		private void instrumentTrim(@NotNull Annotation annotation) {
			this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "trim", "()Ljava/lang/String;", false);
		}
		
		private void instrumentUpperCase(@NotNull Annotation annotation) {
			String value = annotation.getOrDefault("value");
			if (value.isEmpty()) {
				this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "toUpperCase", "()Ljava/lang/String;", false);
			} else {
				this.instrumentLocale(value);
				this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "toUpperCase", "(Ljava/util/Locale;)Ljava/lang/String;", false);
			}
		}
		//endregion
		
		//region Conditions
		private void instrumentContains(int index, @NotNull Annotation annotation) {
			String value = annotation.getOrDefault("value");
			if (value.isEmpty()) {
				throw new IllegalArgumentException("Invalid @Contains annotation found, expected 'value' to be set");
			}
			
			/*Label label = new Label();
			this.mv.visitLdcInsn(value);
			this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "contains", "(Ljava/lang/CharSequence;)Z", false);
			this.mv.visitJumpInsn(Opcodes.IFEQ, label);
			instrumentThrownException(this.mv, ILLEGAL_ARGUMENT_EXCEPTION, "String must contain '" + value + "'");
			this.insertLabel(label);*/
		}
		
		private void instrumentEndsWith(int index, @NotNull Annotation annotation) {
			String value = annotation.getOrDefault("value");
			if (value.isEmpty()) {
				throw new IllegalArgumentException("Invalid @EndsWith annotation found, expected 'value' to be set");
			}
			
			Label label = new Label();
			this.mv.visitVarInsn(Opcodes.ALOAD, index);
			this.mv.visitLdcInsn(value);
			this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "endsWith", "(Ljava/lang/String;)Z", false);
			this.mv.visitJumpInsn(Opcodes.IFEQ, label);
			instrumentThrownException(this.mv, ILLEGAL_ARGUMENT_EXCEPTION, "String must end with '" + value + "'");
			this.insertLabel(label);
		}
		
		private void instrumentNotBlank(int index, @NotNull Annotation annotation) {
			Label label = new Label();
			this.mv.visitVarInsn(Opcodes.ALOAD, index);
			this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "isBlank", "()Z", false);
			this.mv.visitJumpInsn(Opcodes.IFEQ, label);
			instrumentThrownException(this.mv, ILLEGAL_ARGUMENT_EXCEPTION, "String must not be blank");
			this.insertLabel(label);
		}
		
		private void instrumentNotEmpty(int index, @NotNull Annotation annotation) {
			Label label = new Label();
			this.mv.visitVarInsn(Opcodes.ALOAD, index);
			this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "isEmpty", "()Z", false);
			this.mv.visitJumpInsn(Opcodes.IFEQ, label);
			instrumentThrownException(this.mv, ILLEGAL_ARGUMENT_EXCEPTION, "String must not be empty");
			this.insertLabel(label);
		}
		
		private void instrumentStartsWith(int index, @NotNull Annotation annotation) {
			String value = annotation.getOrDefault("value");
			int offset = annotation.getOrDefault("offset");
			if (value.isEmpty()) {
				throw new IllegalArgumentException("Invalid @StartsWith annotation found, expected 'value' to be set");
			}
			
			this.mv.visitVarInsn(Opcodes.ALOAD, index);
			this.mv.visitLdcInsn(value);
			if (offset > 0) {
				loadNumber(this.mv, offset);
				this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "startsWith", "(Ljava/lang/String;I)Z", false);
			} else {
				this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "startsWith", "(Ljava/lang/String;)Z", false);
			}
			Label label = new Label();
			this.mv.visitJumpInsn(Opcodes.IFEQ, label);
			instrumentThrownException(this.mv, ILLEGAL_ARGUMENT_EXCEPTION, "String must start with '" + value + "'");
			this.insertLabel(label);
		}
		//endregion
		
		//region Helper methods
		private void instrumentLocale(@NotNull String value) {
			String[] parts = value.split(":|\\s");
			if (parts.length == 1) {
				this.mv.visitLdcInsn(parts[0]);
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Locale", "of", "(Ljava/lang/String;)Ljava/util/Locale;", false);
			} else if (parts.length == 2) {
				this.mv.visitLdcInsn(parts[0]);
				this.mv.visitLdcInsn(parts[1]);
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Locale", "of", "(Ljava/lang/String;Ljava/lang/String;)Ljava/util/Locale;", false);
			} else if (parts.length == 3) {
				this.mv.visitLdcInsn(parts[0]);
				this.mv.visitLdcInsn(parts[1]);
				this.mv.visitLdcInsn(parts[2]);
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Locale", "of", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/util/Locale;", false);
			} else {
				throw new IllegalArgumentException("Invalid locale format found, expected 'language:country:variant' but found '" + value + "'");
			}
		}
		//endregion
	}
}
