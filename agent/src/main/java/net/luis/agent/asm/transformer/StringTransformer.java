package net.luis.agent.asm.transformer;

import net.luis.agent.Agent;
import net.luis.agent.asm.base.*;
import net.luis.agent.asm.data.*;
import net.luis.agent.asm.data.Class;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.asm.type.SignatureType;
import net.luis.agent.util.StripMode;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.util.*;
import java.util.regex.Pattern;
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
				if (method.getParameters().values().stream().anyMatch(parameter -> parameter.isAnnotatedWithAny(ALL) && parameter.is(STRING))) {
					return true;
				}
				return method.getLocals().stream().anyMatch(local -> local.isAnnotatedWithAny(ALL) && local.is(STRING));
			}
			
			@Override
			protected @NotNull MethodVisitor createMethodVisitor(@NotNull LocalVariablesSorter visitor, @NotNull Method method) {
				return new StringMethodVisitor(visitor, method);
			}
		};
	}
	
	private static class StringMethodVisitor extends LabelTrackingMethodVisitor {
		
		private static final String REPORT_CATEGORY = "Invalid Annotated Element";
		
		private final Set<Integer> handledLocals = new HashSet<>();
		private final List<Parameter> parameters;
		private final boolean includeLocals;
		
		private StringMethodVisitor(@NotNull MethodVisitor visitor, @NotNull Method method) {
			super(visitor);
			this.method = method;
			this.parameters = method.getParameters().values().stream().filter(parameter -> parameter.isAnnotatedWithAny(ALL) && parameter.is(STRING)).toList();
			this.includeLocals = method.getLocals().stream().anyMatch(local -> local.isAnnotatedWithAny(ALL));
		}
		
		@Override
		public void visitCode() {
			super.visitCode();
			for (Parameter parameter : this.parameters) {
				this.mv.visitVarInsn(Opcodes.ALOAD, parameter.getLoadIndex());
				this.instrumentModifications(parameter.getLoadIndex(), parameter.getAnnotations());
				this.mv.visitVarInsn(Opcodes.ASTORE, parameter.getLoadIndex());
				this.instrumentConditions(parameter.getLoadIndex(), parameter.getAnnotations());
			}
		}
		
		@Override
		public void visitVarInsn(int opcode, int index) {
			super.visitVarInsn(opcode, index);
			if (this.includeLocals && isStore(opcode) && this.method.isLocal(index) && this.handledLocals.add(index)) {
				LocalVariable local = this.method.getLocals(index).stream().filter(l -> l.isAnnotatedWithAny(ALL)).filter(l -> l.isInScope(this.getScopeIndex())).findFirst().orElse(null);
				if (local != null && local.is(STRING)) {
					this.mv.visitVarInsn(Opcodes.ALOAD, index);
					if (local.isAnnotatedWithAny(MODIFICATIONS)) {
						this.instrumentModifications(index, local.getAnnotations());
					}
					this.mv.visitVarInsn(Opcodes.ASTORE, index);
					this.instrumentConditions(index, local.getAnnotations());
				}
			}
		}
		
		@Override
		public void visitInsn(int opcode) {
			if (isReturn(opcode) && this.method.returns(STRING)) {
				int local = newLocal(this.mv, STRING);
				Label start = new Label();
				Label end = new Label();
				
				this.mv.visitVarInsn(Opcodes.ASTORE, local);
				this.insertLabel(start);
				if (this.method.isAnnotatedWithAny(CONDITIONS)) {
					this.instrumentConditions(local, this.method.getAnnotations());
				}
				this.mv.visitVarInsn(Opcodes.ALOAD, local);
				this.instrumentModifications(local, this.method.getAnnotations());
				this.insertLabel(end);
				this.visitLocalVariable(local, "generated$StringTransformer$Temp" + local, STRING, null, start, end);
			}
			super.visitInsn(opcode);
		}
		
		//region Instrumentation
		private void instrumentModifications(int index, @NotNull Map<Type, Annotation> annotations) {
			if (annotations.containsKey(SUBSTRING)) { // Depends on length (no modifications)
				this.instrumentSubstring(index, annotations.get(SUBSTRING));
			}
			if (annotations.containsKey(LOWER_CASE)) {
				this.instrumentLowerCase(annotations.get(LOWER_CASE));
			}
			if (annotations.containsKey(REPLACE)) {
				this.instrumentReplace(annotations.get(REPLACE));
			}
			if (annotations.containsKey(STRIP)) {
				this.instrumentStrip(annotations.get(STRIP));
			}
			if (annotations.containsKey(TRIM)) {
				this.instrumentTrim();
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
				this.instrumentNotBlank(index);
			}
			if (annotations.containsKey(NOT_EMPTY)) {
				this.instrumentNotEmpty(index);
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
				throw CrashReport.create("Invalid @Replace annotation found, expected at least one of 'value' or 'regex' to be set", REPORT_CATEGORY).addDetail("Method", this.method.getSignature(SignatureType.DEBUG))
					.addDetail("Annotation", annotation.getSignature(SignatureType.SOURCE)).exception();
			}
			if (!value.isEmpty() && !regex.isEmpty()) {
				throw CrashReport.create("Invalid @Replace annotation found, expected only one of 'value' or 'regex' to be set", REPORT_CATEGORY).addDetail("Method", this.method.getSignature(SignatureType.DEBUG))
					.addDetail("Annotation", annotation.getSignature(SignatureType.SOURCE)).exception();
			}
			if (!value.contains(" -> ") && replacement.isEmpty()) {
				throw CrashReport.create("Invalid @Replace annotation found, expected 'replacement' to be set", REPORT_CATEGORY).addDetail("Method", this.method.getSignature(SignatureType.DEBUG))
					.addDetail("Annotation", annotation.getSignature(SignatureType.SOURCE)).exception();
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
		
		private void instrumentSubstring(int index, @NotNull Annotation annotation) {
			String value = annotation.getOrDefault("value");
			if (value.isBlank()) {
				throw CrashReport.create("Invalid @Substring annotation found, expected 'start:end'", REPORT_CATEGORY).addDetail("Method", this.method.getSignature(SignatureType.DEBUG))
					.addDetail("Annotation", annotation.getSignature(SignatureType.SOURCE)).addDetail("Value Found", value).exception();
			}
			
			int start = 0;
			int end = -1;
			boolean dynamic = false;
			if (value.contains(":")) {
				String[] parts = value.split(":");
				if (parts.length != 2) {
					throw CrashReport.create("Invalid @Substring annotation found, expected 'start:end'", REPORT_CATEGORY).addDetail("Method", this.method.getSignature(SignatureType.DEBUG))
						.addDetail("Annotation", annotation.getSignature(SignatureType.SOURCE)).addDetail("Value Found", value).exception();
				}
				if ("*".equals(parts[0]) && "*".equals(parts[1])) {
					throw CrashReport.create("Invalid @Substring annotation found, expected 'start:end'", REPORT_CATEGORY).addDetail("Method", this.method.getSignature(SignatureType.DEBUG))
						.addDetail("Annotation", annotation.getSignature(SignatureType.SOURCE)).addDetail("Value Found", value).exception();
				}
				if (parts[0].isBlank() || parts[1].isBlank()) {
					throw CrashReport.create("Invalid @Substring annotation found, expected 'start:end'", REPORT_CATEGORY).addDetail("Method", this.method.getSignature(SignatureType.DEBUG))
						.addDetail("Annotation", annotation.getSignature(SignatureType.SOURCE)).addDetail("Value Found", value).exception();
				}
				if (!"*".equals(parts[0])) {
					start = Integer.parseInt(parts[0]);
				}
				
				if (!"*".equals(parts[1])) {
					if (Pattern.matches("^\\*\\s*-\\s*\\d+$", parts[1])) {
						dynamic = true;
						end = Integer.parseInt(parts[1].replace("*", "").replace("-", "").trim());
					} else {
						end = Integer.parseInt(parts[1]);
					}
				}
			} else {
				start = Integer.parseInt(value);
			}
			
			loadNumber(this.mv, start);
			if (0 > end) {
				this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "substring", "(I)Ljava/lang/String;", false);
			} else if (dynamic) {
				this.mv.visitVarInsn(Opcodes.ALOAD, index);
				this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "length", "()I", false);
				loadNumber(this.mv, end);
				this.mv.visitInsn(Opcodes.ISUB);
				this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "substring", "(II)Ljava/lang/String;", false);
			} else {
				loadNumber(this.mv, end);
				this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "substring", "(II)Ljava/lang/String;", false);
			}
		}
		
		private void instrumentTrim() {
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
			Label label = new Label();
			
			this.mv.visitVarInsn(Opcodes.ALOAD, index);
			this.mv.visitLdcInsn(value);
			this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "contains", "(Ljava/lang/CharSequence;)Z", false);
			this.mv.visitJumpInsn(Opcodes.IFNE, label);
			instrumentThrownException(this.mv, ILLEGAL_ARGUMENT_EXCEPTION, "String must contain '" + value + "'");
			this.insertLabel(label);
		}
		
		private void instrumentEndsWith(int index, @NotNull Annotation annotation) {
			String value = annotation.getOrDefault("value");
			Label label = new Label();
			
			this.mv.visitVarInsn(Opcodes.ALOAD, index);
			this.mv.visitLdcInsn(value);
			this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "endsWith", "(Ljava/lang/String;)Z", false);
			this.mv.visitJumpInsn(Opcodes.IFNE, label);
			instrumentThrownException(this.mv, ILLEGAL_ARGUMENT_EXCEPTION, "String must end with '" + value + "'");
			this.insertLabel(label);
		}
		
		private void instrumentNotBlank(int index) {
			Label label = new Label();
			this.mv.visitVarInsn(Opcodes.ALOAD, index);
			this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "isBlank", "()Z", false);
			this.mv.visitJumpInsn(Opcodes.IFEQ, label);
			instrumentThrownException(this.mv, ILLEGAL_ARGUMENT_EXCEPTION, "String must not be blank");
			this.insertLabel(label);
		}
		
		private void instrumentNotEmpty(int index) {
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
			
			this.mv.visitVarInsn(Opcodes.ALOAD, index);
			this.mv.visitLdcInsn(value);
			if (offset > 0) {
				loadNumber(this.mv, offset);
				this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "startsWith", "(Ljava/lang/String;I)Z", false);
			} else {
				this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "startsWith", "(Ljava/lang/String;)Z", false);
			}
			Label label = new Label();
			this.mv.visitJumpInsn(Opcodes.IFNE, label);
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
