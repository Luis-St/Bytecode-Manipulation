package net.luis.agent.asm.transformer.method;

import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.visitor.*;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.preload.PreloadContext;
import net.luis.agent.preload.data.*;
import net.luis.agent.preload.type.MethodType;
import net.luis.agent.preload.type.TypeModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.util.ArrayList;
import java.util.List;

import static net.luis.agent.asm.Types.*;

/**
 *
 * @author Luis-St
 *
 */

public class RangeTransformer extends BaseClassTransformer {
	
	private static final Type[] ANNOS = { ABOVE, ABOVE_EQUAL, BELOW, BELOW_EQUAL };
	
	public RangeTransformer(@NotNull PreloadContext context) {
		super(context, true);
	}
	
	//region Type filtering
	@Override
	protected boolean shouldTransform(@NotNull Type type) {
		ClassContent content = this.context.getClassContent(type);
		return content.getParameters().stream().anyMatch(parameter -> parameter.isAnnotatedWithAny(ANNOS)) || content.methods().stream().anyMatch(method -> method.isAnnotatedWithAny(ANNOS));
	}
	//endregion
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @Nullable Class<?> clazz, @NotNull ClassReader reader, @NotNull ClassWriter writer) {
		ClassContent content = this.context.getClassContent(type);
		return new MethodOnlyClassVisitor(writer, this.context, type, () -> this.modified = true) {
			
			@Override
			protected @NotNull MethodVisitor createMethodVisitor(@NotNull LocalVariablesSorter visitor, @NotNull MethodData method) {
				return new RangeVisitor(visitor, this.context, this.type, method, this::markModified);
			}
		};
	}
	
	private static class RangeVisitor extends BaseMethodVisitor {
		
		private static final String INVALID_CATEGORY = "Invalid Annotated Element";
		private static final String UNSUPPORTED_CATEGORY = "Unsupported Annotation Combination";
		private static final Type ILL_ARG = Type.getType(IllegalArgumentException.class);
		
		private final List<ParameterData> lookup = new ArrayList<>();
		
		private RangeVisitor(@NotNull MethodVisitor visitor, @NotNull PreloadContext context, @NotNull Type type, @NotNull MethodData method, @NotNull Runnable markedModified) {
			super(visitor, context, type, method, markedModified);
			//region Parameter validation
			for (ParameterData parameter : method.parameters()) {
				if (parameter.isAnnotatedWithAny(ANNOS)) {
					if (!parameter.isAny(NUMBERS)) {
						throw CrashReport.create(UNSUPPORTED_CATEGORY, "Parameter annotated with @Above, @AboveEqual, @Below or @BelowEqual must be a number type").addDetail("Method", method.getMethodSignature())
							.addDetail("Parameter", parameter.name()).addDetail("Parameter At", parameter.index()).addDetail("Parameter Type", parameter.type()).exception();
					}
					if (parameter.getAnnotations().stream().filter(annotation -> annotation.isAny(ABOVE, ABOVE_EQUAL)).count() > 1) {
						throw CrashReport.create(UNSUPPORTED_CATEGORY, "Parameter must not be annotated with @Above and @AboveEqual at the same time").addDetail("Method", method.getMethodSignature())
							.addDetail("Parameter At", parameter.index()).addDetail("Annotations", parameter.getAnnotations().stream().map(AnnotationData::type).toList()).exception();
					}
					if (parameter.getAnnotations().stream().filter(annotation -> annotation.isAny(BELOW, BELOW_EQUAL)).count() > 1) {
						throw CrashReport.create(UNSUPPORTED_CATEGORY, "Parameter must not be annotated with @Below and @BelowEqual at the same time").addDetail("Method", method.getMethodSignature())
							.addDetail("Parameter At", parameter.index()).addDetail("Annotations", parameter.getAnnotations().stream().map(AnnotationData::type).toList()).exception();
					}
					this.lookup.add(parameter);
				}
			}
			//endregion
			//region Method validation
			if (method.isAnnotatedWithAny(ANNOS)) {
				if (!this.method.is(MethodType.METHOD)) {
					throw CrashReport.create(INVALID_CATEGORY, "Annotation @Above, @AboveEqual, @Below or @BelowEqual can not be applied to constructors and static initializers").addDetail("Method", method.name()).exception();
				}
				if (!method.returnsAny(NUMBERS)) {
					throw CrashReport.create(INVALID_CATEGORY, "Method annotated with @Above, @AboveEqual, @Below or @BelowEqual must return a number type").addDetail("Method", method.getMethodSignature())
						.addDetail("Return Type", method.getReturnType()).exception();
				}
				if (method.getAnnotations().stream().filter(annotation -> annotation.isAny(ABOVE, ABOVE_EQUAL)).count() > 1) {
					throw CrashReport.create(UNSUPPORTED_CATEGORY, "Method must not be annotated with @Above and @AboveEqual at the same time").addDetail("Method", method.getMethodSignature())
						.addDetail("Annotations", method.getAnnotations().stream().map(AnnotationData::type).toList()).exception();
				}
				if (method.getAnnotations().stream().filter(annotation -> annotation.isAny(BELOW, BELOW_EQUAL)).count() > 1) {
					throw CrashReport.create(UNSUPPORTED_CATEGORY, "Method must not be annotated with @Below and @BelowEqual at the same time").addDetail("Method", method.getMethodSignature())
						.addDetail("Annotations", method.getAnnotations().stream().map(AnnotationData::type).toList()).exception();
				}
			}
			//endregion
		}
		
		@Override
		public void visitCode() {
			this.mv.visitCode();
			for (ParameterData parameter : this.lookup) {
				String message = parameter.getMessageName() + " must be ";
				int index = this.method.is(TypeModifier.STATIC) ? parameter.index() : parameter.index() + 1;
				
				if (parameter.isAnnotatedWith(ABOVE)) {
					this.instrument(parameter.getAnnotation(ABOVE), parameter.type(), index, true, Opcodes.IFGT, message + "above");
				}
				if (parameter.isAnnotatedWith(ABOVE_EQUAL)) {
					this.instrument(parameter.getAnnotation(ABOVE_EQUAL), parameter.type(), index, true, Opcodes.IFGE, message + "above or equal to");
				}
				if (parameter.isAnnotatedWith(BELOW)) {
					this.instrument(parameter.getAnnotation(BELOW), parameter.type(), index, false, Opcodes.IFGT, message + "below");
				}
				if (parameter.isAnnotatedWith(BELOW_EQUAL)) {
					this.instrument(parameter.getAnnotation(BELOW_EQUAL), parameter.type(), index, false, Opcodes.IFGE, message + "below or equal to");
				}
				this.markModified();
			}
		}
		
		@Override
		public void visitInsn(int opcode) {
			if (this.isValidOpcode(opcode) && this.method.isAnnotatedWithAny(ANNOS)) {
				Label start = new Label();
				Label end = new Label();
				Type type = this.method.getReturnType();
				int local = this.newLocal(type);
				String message = "Method " + this.type.getClassName() + "#" + this.method.name() + " return value must be ";
				this.mv.visitLabel(start);
				this.mv.visitVarInsn(type.getOpcode(Opcodes.ISTORE), local);
				
				if (this.method.isAnnotatedWith(ABOVE)) {
					this.instrument(this.method.getAnnotation(ABOVE), type, local, true, Opcodes.IFGT, message + "above");
				}
				if (this.method.isAnnotatedWith(ABOVE_EQUAL)) {
					this.instrument(this.method.getAnnotation(ABOVE_EQUAL), type, local, true, Opcodes.IFGE, message + "above or equal to");
				}
				if (this.method.isAnnotatedWith(BELOW)) {
					this.instrument(this.method.getAnnotation(BELOW), type, local, false, Opcodes.IFGT, message + "below");
				}
				if (this.method.isAnnotatedWith(BELOW_EQUAL)) {
					this.instrument(this.method.getAnnotation(BELOW_EQUAL), type, local, false, Opcodes.IFGE, message + "below or equal to");
				}
				
				this.mv.visitJumpInsn(Opcodes.GOTO, end);
				this.mv.visitLabel(end);
				this.mv.visitVarInsn(type.getOpcode(Opcodes.ILOAD), local);
				this.mv.visitLocalVariable("generated$RangeTransformer$Temp" + local, type.getDescriptor(), null, start, end, local);
				this.markModified();
			}
			this.mv.visitInsn(opcode);
		}
		
		//region Helper methods
		private void instrument(@NotNull AnnotationData annotation, @NotNull Type type, int loadIndex, boolean above, int compare, String message) {
			Label label = new Label();
			Double value = annotation.get("value");
			if (value == null) {
				return;
			}
			if (above) {
				this.loadNumberAsDouble(this.mv, type, loadIndex);
				this.loadNumber(this.mv, value);
			} else {
				this.loadNumber(this.mv, value);
				this.loadNumberAsDouble(this.mv, type, loadIndex);
			}
			this.mv.visitInsn(Opcodes.DCMPL);
			this.mv.visitJumpInsn(compare, label);
			this.instrumentThrownException(this.mv, ILL_ARG, message + " " + value);
			this.mv.visitJumpInsn(Opcodes.GOTO, label);
			this.mv.visitLabel(label);
		}
		
		private boolean isValidOpcode(int opcode) {
			return opcode == Opcodes.IRETURN || opcode == Opcodes.LRETURN || opcode == Opcodes.FRETURN || opcode == Opcodes.DRETURN;
		}
		//endregion
	}
}
