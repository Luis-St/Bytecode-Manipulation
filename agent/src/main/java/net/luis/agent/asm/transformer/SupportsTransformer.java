package net.luis.agent.asm.transformer;

import net.luis.agent.Agent;
import net.luis.agent.asm.base.*;
import net.luis.agent.asm.data.*;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.asm.type.SignatureType;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.util.*;
import java.util.stream.Collectors;

import static net.luis.agent.asm.Types.*;
import static net.luis.agent.asm.Instrumentations.*;

/**
 *
 * @author Luis-St
 *
 */

public class SupportsTransformer extends BaseClassTransformer {
	
	@Override
	protected boolean shouldIgnoreClass(@NotNull Type type) {
		return Agent.getClass(type).getParameters().stream().noneMatch(parameter -> parameter.isAnnotatedWith(SUPPORTS));
	}
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @NotNull ClassWriter writer) {
		return new MethodOnlyClassVisitor(writer, type, () -> this.modified = true) {
			
			@Override
			protected boolean isMethodValid(@NotNull Method method) {
				return method.getParameters().values().stream().anyMatch(parameter -> parameter.isAnnotatedWith(SUPPORTS));
			}
			
			@Override
			protected @NotNull MethodVisitor createMethodVisitor(@NotNull LocalVariablesSorter visitor, @NotNull Method method) {
				return new SupportsMethodVisitor(visitor, method);
			}
		};
	}
	
	private static class SupportsMethodVisitor extends LabelTrackingMethodVisitor {
		
		private static final String REPORT_CATEGORY = "Invalid Annotated Element";
		
		private final Map<Integer, Annotation> lookup = new HashMap<>();
		
		private SupportsMethodVisitor(@NotNull MethodVisitor visitor, @NotNull Method method) {
			super(visitor);
			this.method = method;
			//region Validation
			for (Parameter parameter : method.getParameters().values()) {
				if (!parameter.isAnnotatedWith(SUPPORTS)) {
					continue;
				}
				if (isPrimitive(parameter.getType())) {
					throw CrashReport.create("Parameter annotated with @Supports must not be a primitive type", REPORT_CATEGORY).addDetail("Method", method.getSignature(SignatureType.DEBUG))
						.addDetail("Parameter Index", parameter.getIndex()).addDetail("Parameter Type", parameter.getType()).addDetail("Parameter Name", parameter.getName()).exception();
				}
				this.lookup.put(parameter.getIndex(), parameter.getAnnotation(SUPPORTS));
			}
			//endregion
		}
		
		@Override
		public void visitCode() {
			super.visitCode();
			for (Map.Entry<Integer, Annotation> entry : this.lookup.entrySet()) {
				Annotation annotation = entry.getValue();
				this.instrument(entry.getKey(), annotation.getOrDefault("inherit"), annotation.getOrDefault("value"));
			}
		}
		
		//region Instrumentation
		private void instrument(int index, boolean inherit, @NotNull List<Type> types) {
			Label label = new Label();
			for (Type type : types) {
				this.mv.visitVarInsn(Opcodes.ALOAD, index);
				if (inherit) {
					this.mv.visitTypeInsn(Opcodes.INSTANCEOF, convertToWrapper(type).getInternalName());
					this.mv.visitJumpInsn(Opcodes.IFNE, label);
				} else {
					this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
					this.mv.visitLdcInsn(convertToWrapper(type));
					this.mv.visitJumpInsn(Opcodes.IF_ACMPEQ, label);
				}
			}
			this.mv.visitTypeInsn(Opcodes.NEW, ILLEGAL_ARGUMENT_EXCEPTION.getInternalName());
			this.mv.visitInsn(Opcodes.DUP);
			this.mv.visitVarInsn(Opcodes.ALOAD, index);
			this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
			this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false);
			this.mv.visitInvokeDynamicInsn("makeConcatWithConstants", "(Ljava/lang/String;)Ljava/lang/String;", STRING_CONCAT_HANDLE, this.buildMessage(types));
			this.mv.visitMethodInsn(Opcodes.INVOKESPECIAL, ILLEGAL_ARGUMENT_EXCEPTION.getInternalName(), "<init>", "(Ljava/lang/String;)V", false);
			this.mv.visitInsn(Opcodes.ATHROW);
			this.insertLabel(label);
		}
		//endregion
		
		//region Helper methods
		private @NotNull String buildMessage(@NotNull List<Type> types) {
			String str = types.stream().map(Type::getClassName).collect(Collectors.joining("', '", "'", "'"));
			return "Unsupported type, expected one of " + str + " but got: \u0001";
		}
		//endregion
	}
}
