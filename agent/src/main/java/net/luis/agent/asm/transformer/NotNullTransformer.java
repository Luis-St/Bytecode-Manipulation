package net.luis.agent.asm.transformer;

import net.luis.agent.Agent;
import net.luis.agent.asm.base.*;
import net.luis.agent.asm.data.*;
import net.luis.agent.asm.data.Class;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.asm.type.*;
import net.luis.agent.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.util.*;
import java.util.stream.Collectors;

import static net.luis.agent.asm.Instrumentations.*;
import static net.luis.agent.asm.Types.*;

/**
 *
 * @author Luis-St
 *
 */

public class NotNullTransformer extends BaseClassTransformer {
	
	static final Set<Type> LOOKUP = Agent.stream().filter(clazz -> clazz.is(ClassType.ANNOTATION) && clazz.isAnnotatedWith(IMPLICIT_NON_NULL)).map(Class::getType).collect(Collectors.toSet());
	
	public NotNullTransformer() {
		super(true);
	}
	
	//region Static helper methods
	private static boolean implicitNotNull(@NotNull Collection<Annotation> annotations) {
		return annotations.stream().anyMatch(annotation -> LOOKUP.contains(annotation.getType()) || PATTERN.equals(annotation.getType()) || PatternTransformer.LOOKUP.containsKey(annotation.getType()));
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
				if (method.isAnnotatedWith(NOT_NULL) || implicitNotNull(method.getAnnotations().values())) {
					return true;
				}
				Class clazz = Agent.getClass(method.getOwner());
				if (clazz.getFields().values().stream().anyMatch(field -> {
					return field.isAnnotatedWith(NOT_NULL) || implicitNotNull(field.getAnnotations().values());
				})) {
					return true;
				}
				if (method.getParameters().values().stream().anyMatch(parameter -> {
					return parameter.isAnnotatedWith(NOT_NULL) || implicitNotNull(parameter.getAnnotations().values());
				})) {
					return true;
				}
				return method.getLocals().stream().anyMatch(local -> {
					return local.isAnnotatedWith(NOT_NULL) || implicitNotNull(local.getAnnotations().values());
				});
			}
			
			@Override
			protected @NotNull MethodVisitor createMethodVisitor(@NotNull LocalVariablesSorter visitor, @NotNull Method method) {
				return new NotNullVisitor(visitor, method);
			}
		};
	}
	
	private static class NotNullVisitor extends LabelTrackingMethodVisitor {
		
		private static final String REPORT_CATEGORY = "Invalid Annotated Element";
		
		private final List<Parameter> parameters = new ArrayList<>();
		private final boolean includeLocals;
		
		private NotNullVisitor(@NotNull MethodVisitor visitor, @NotNull Method method) {
			super(visitor);
			this.method = method;
			method.getParameters().values().stream().filter(this::isAnnotated).forEach(this.parameters::add);
			this.includeLocals = method.getLocals().stream().anyMatch(this::isAnnotated);
		}
		
		@Override
		public void visitCode() {
			this.mv.visitCode();
			for (Parameter parameter : this.parameters) {
				this.validateParameter(parameter);
				if (!isPrimitive(parameter.getType())) {
					instrumentNonNullCheck(this.mv, parameter.getLoadIndex(), this.getMessage(parameter.getAnnotation(NOT_NULL), parameter.getMessageName()));
					this.mv.visitInsn(Opcodes.POP);
				}
			}
		}
		
		@Override
		public void visitFieldInsn(int opcode, @NotNull String owner, @NotNull String name, @NotNull String descriptor) {
			Type type = Type.getType(descriptor);
			if (opcode == Opcodes.PUTFIELD || opcode == Opcodes.PUTSTATIC) {
				if (!isPrimitive(type))  {
					Field field = Agent.getClass(this.method.getOwner()).getField(name);
					if (field != null && this.isAnnotated(field)) {
						this.validateField(field);
						if (!isPrimitive(field.getType())) {
							instrumentNonNullCheck(this.mv, -1, this.getMessage(field.getAnnotation(NOT_NULL), "Field " + field.getName()));
							this.mv.visitTypeInsn(Opcodes.CHECKCAST, type.getInternalName());
						}
					}
				}
			}
			super.visitFieldInsn(opcode, owner, name, descriptor);
		}
		
		@Override
		public void visitVarInsn(int opcode, int index) {
			if (this.includeLocals && isStore(opcode) && this.method.isLocal(index)) {
				LocalVariable local = this.method.getLocals(index).stream().filter(this::isAnnotated).filter(l -> l.isInScope(this.getScopeIndex())).findFirst().orElse(null);
				if (local != null && this.isAnnotated(local)) {
					this.validateLocal(local);
					if (!isPrimitive(local.getType())) {
						instrumentNonNullCheck(this.mv, -1, this.getMessage(local.getAnnotation(NOT_NULL), local.getMessageName()));
						this.mv.visitTypeInsn(Opcodes.CHECKCAST, local.getType().getInternalName());
					}
				}
			}
			super.visitVarInsn(opcode, index);
		}
		
		@Override
		public void visitInsn(int opcode) {
			if (opcode == Opcodes.ARETURN && this.isAnnotated(this.method)) {
				this.validateMethod();
				if (!isPrimitive(this.method.getReturnType())) {
					instrumentNonNullCheck(this.mv, -1, "Method must not return null");
					this.mv.visitTypeInsn(Opcodes.CHECKCAST, this.method.getReturnType().getInternalName());
				}
			}
			this.mv.visitInsn(opcode);
		}
		
		//region Helper methods
		private boolean isAnnotated(@NotNull ASMData element) {
			return element.isAnnotatedWith(NOT_NULL) || implicitNotNull(element.getAnnotations().values());
		}
		
		private @NotNull String getMessage(@Nullable Annotation annotation, @NotNull String messageName) {
			if (annotation != null) {
				String value = annotation.getOrDefault("value");
				if (!value.isBlank()) {
					value = value.strip();
					if (Utils.isSingleWord(value)) {
						return Utils.capitalize(value) + " must not be null";
					} else if (value.charAt(0) == '\'' && value.charAt(value.length() - 1) == '\'') {
						return value.substring(1, value.length() - 1) + " must not be null";
					}
					return value;
				}
			}
			return messageName + " must not be null";
		}
		
		private void validateParameter(@NotNull Parameter parameter) {
			if (isPrimitive(parameter.getType()) && parameter.isAnnotatedWith(NOT_NULL)) {
				throw CrashReport.create("Parameter annotated with @NotNull must not be a primitive type", REPORT_CATEGORY).addDetail("Method", this.method.getSignature(SignatureType.DEBUG))
					.addDetail("Parameter Index", parameter.getIndex()).addDetail("Parameter Type", parameter.getType()).addDetail("Parameter Name", parameter.getName()).exception();
			}
		}
		
		private void validateField(@NotNull Field field) {
			if (isPrimitive(field.getType()) && field.isAnnotatedWith(NOT_NULL)) {
				throw CrashReport.create("Field annotated with @NotNull must not be a primitive type", REPORT_CATEGORY).addDetail("Method", this.method.getSignature(SignatureType.DEBUG))
					.addDetail("Field Name", field.getName()).addDetail("Field Type", field.getType()).exception();
			}
		}
		
		private void validateLocal(@NotNull LocalVariable local) {
			if (isPrimitive(local.getType()) && local.isAnnotatedWith(NOT_NULL)) {
				throw CrashReport.create("Parameter annotated with @NotNull must not be a primitive type", REPORT_CATEGORY).addDetail("Method", this.method.getSignature(SignatureType.DEBUG))
					.addDetail("Local Variable Index", local.getIndex()).addDetail("Local Variable  Type", local.getType()).addDetail("Local Variable Name", local.getName()).exception();
			}
		}
		
		private void validateMethod() {
			if (!this.method.is(MethodType.METHOD)) {
				throw CrashReport.create("Annotation @NotNull can not be applied to constructors and static initializers", REPORT_CATEGORY).addDetail("Method", this.method.getSignature(SignatureType.DEBUG)).exception();
			}
			if (this.method.returns(VOID)) {
				throw CrashReport.create("Method annotated with @NotNull must not return void", REPORT_CATEGORY).addDetail("Method", this.method.getSignature(SignatureType.DEBUG)).exception();
			}
			if (isPrimitive(this.method.getReturnType()) && this.method.isAnnotatedWith(NOT_NULL)) {
				throw CrashReport.create("Method annotated with @NotNull must not return a primitive type", REPORT_CATEGORY).addDetail("Method", this.method.getSignature(SignatureType.DEBUG))
					.addDetail("Return Type", this.method.getReturnType()).exception();
			}
		}
		//endregion
	}
}
